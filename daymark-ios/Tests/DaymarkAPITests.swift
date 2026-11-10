import XCTest
@testable import Daymark

/// 使用本地模拟响应核对接口，不向真实服务器发送测试数据。
final class DaymarkAPITests: XCTestCase {
    @MainActor
    func testRegistrationCodeRequest() async throws {
        let configuration = URLSessionConfiguration.ephemeral
        configuration.protocolClasses = [StubProtocol.self]
        let session = URLSession(configuration: configuration)
        defer { session.invalidateAndCancel() }
        StubProtocol.handler = { request in
            XCTAssertEqual(request.url?.host, "account.creepersan.com")
            XCTAssertEqual(request.url?.path, "/api/register/request_auth_code")
            XCTAssertEqual(request.httpMethod, "POST")
            XCTAssertEqual(request.value(forHTTPHeaderField: "CS-App"), "Daymark")
            // URLSession 可能将请求体转换为流交给 URLProtocol。
            let body: Data
            if let data = request.httpBody { body = data }
            else {
                let stream = try XCTUnwrap(request.httpBodyStream)
                stream.open(); defer { stream.close() }
                var data = Data(); var buffer = [UInt8](repeating: 0, count: 1024)
                while stream.hasBytesAvailable {
                    let count = stream.read(&buffer, maxLength: buffer.count)
                    if count <= 0 { break }
                    data.append(contentsOf: buffer.prefix(count))
                }
                body = data
            }
            let json = try XCTUnwrap(JSONSerialization.jsonObject(with: body) as? [String: String])
            XCTAssertEqual(json["email"], "test@example.invalid")
            return Data("{\"code\":200,\"data\":null}".utf8)
        }
        defer { StubProtocol.handler = nil }
        let api = DaymarkAPI(urlSession: session)
        try await api.requestCode(email: "test@example.invalid", register: true)
    }

    @MainActor
    func testServerErrorDoesNotDependOnSuccessDataShape() async throws {
        let configuration = URLSessionConfiguration.ephemeral
        configuration.protocolClasses = [StubProtocol.self]
        let session = URLSession(configuration: configuration)
        defer { session.invalidateAndCancel(); StubProtocol.handler = nil }
        StubProtocol.handler = { _ in Data("{\"code\":400,\"msg\":\"验证码无效\",\"data\":\"invalid\"}".utf8) }
        let api = DaymarkAPI(urlSession: session)
        do {
            try await api.requestCode(email: "test@example.invalid", register: true)
            XCTFail("服务端错误应当抛出")
        } catch DaymarkError.server(let code, let message) {
            XCTAssertEqual(code, 400)
            XCTAssertEqual(message, "验证码无效")
        }
    }

    @MainActor
    func testResetPasswordRequestsUseEmailCodeAndNewPassword() async throws {
        let configuration = URLSessionConfiguration.ephemeral
        configuration.protocolClasses = [StubProtocol.self]
        let session = URLSession(configuration: configuration)
        defer { session.invalidateAndCancel(); StubProtocol.handler = nil }
        StubProtocol.handler = { request in
            let body = try Self.jsonBody(request)
            XCTAssertEqual(body["email"], "test@example.invalid")
            if request.url?.path == "/api/password/reset" {
                XCTAssertEqual(body["code"], "123456")
                XCTAssertEqual(body["new_password"], "new-password")
            } else {
                XCTAssertEqual(request.url?.path, "/api/password/reset/request_auth_code")
            }
            return Data("{\"code\":200,\"data\":{}}".utf8)
        }
        let api = DaymarkAPI(urlSession: session)
        try await api.requestCode(email: "test@example.invalid", register: false)
        try await api.resetPassword(email: "test@example.invalid", password: "new-password", code: "123456")
    }

    @MainActor
    func testChangePasswordClearsSessionOnlyAfterSuccess() async throws {
        let configuration = URLSessionConfiguration.ephemeral
        configuration.protocolClasses = [StubProtocol.self]
        let session = URLSession(configuration: configuration)
        defer { session.invalidateAndCancel(); StubProtocol.handler = nil }
        let expires = Int64(Date().timeIntervalSince1970 * 1000) + 3_600_000
        let user = UserSession(id: 1, name: "测试", token: "test-token", tokenExpire: expires, refreshToken: "refresh-token", refreshExpire: expires)
        // 使用独立内存存储，既不依赖模拟器签名，也不影响真实账号。
        var storedData: Data? = try JSONEncoder().encode(user)
        let api = DaymarkAPI(urlSession: session, loadSessionData: { storedData }, saveSessionData: { storedData = $0 }, clearSessionData: { storedData = nil })
        StubProtocol.handler = { request in
            XCTAssertEqual(request.url?.path, "/api/password/change")
            XCTAssertEqual(request.value(forHTTPHeaderField: "CS-Token"), "test-token")
            let body = try Self.jsonBody(request)
            XCTAssertEqual(body["old_password"], "old-password")
            XCTAssertEqual(body["new_password"], "new-password")
            return Data("{\"code\":400,\"msg\":\"原密码不正确\"}".utf8)
        }
        do {
            try await api.changePassword(oldPassword: "old-password", newPassword: "new-password")
            XCTFail("错误的原密码应当返回错误")
        } catch DaymarkError.server(let code, _) { XCTAssertEqual(code, 400) }
        XCTAssertNotNil(api.session)
        XCTAssertNotNil(storedData)
        StubProtocol.handler = { _ in Data("{\"code\":200,\"data\":null}".utf8) }
        try await api.changePassword(oldPassword: "old-password", newPassword: "new-password")
        XCTAssertNil(api.session)
        XCTAssertNil(storedData)
        XCTAssertTrue(api.passwordChangeSucceeded)
    }

    /// 昵称更新使用当前令牌，并只在服务端成功后持久化返回的昵称。
    @MainActor
    func testUpdateNicknamePersistsServerResponse() async throws {
        let configuration = URLSessionConfiguration.ephemeral
        configuration.protocolClasses = [StubProtocol.self]
        let network = URLSession(configuration: configuration)
        defer { network.invalidateAndCancel(); StubProtocol.handler = nil }
        let expires = Int64(Date().timeIntervalSince1970 * 1000) + 3_600_000
        let user = UserSession(id: 1, name: "原昵称", token: "test-token", tokenExpire: expires, refreshToken: "refresh-token", refreshExpire: expires, account: "account", email: "test@example.invalid")
        var stored: Data? = try JSONEncoder().encode(user)
        let api = DaymarkAPI(urlSession: network, loadSessionData: { stored }, saveSessionData: { stored = $0 }, clearSessionData: { stored = nil })
        StubProtocol.handler = { request in
            XCTAssertEqual(request.url?.path, "/api/user/update")
            XCTAssertEqual(request.value(forHTTPHeaderField: "CS-Token"), "test-token")
            XCTAssertEqual(try Self.jsonBody(request), ["nickname": "新昵称"])
            return Data("{\"code\":400,\"msg\":\"更新失败\"}".utf8)
        }
        do { try await api.updateNickname("新昵称"); XCTFail("失败时不能更新昵称") }
        catch DaymarkError.server(let code, _) { XCTAssertEqual(code, 400) }
        XCTAssertEqual(api.session?.name, "原昵称")
        StubProtocol.handler = { _ in Data("{\"code\":200,\"data\":{\"user\":{\"id\":1,\"nickname\":\"新昵称\"}}}".utf8) }
        try await api.updateNickname("新昵称")
        XCTAssertEqual(api.session?.name, "新昵称")
        let saved = try JSONDecoder().decode(UserSession.self, from: XCTUnwrap(stored))
        XCTAssertEqual(saved.name, "新昵称")
        XCTAssertEqual(saved.account, "account")
        XCTAssertEqual(saved.token, "test-token")
    }

    /// 直连本地 IP 时，实际请求必须携带账号服务的 Host。
    @MainActor
    func testLocalProxyUsesAccountHost() async throws {
        let defaults = UserDefaults.standard
        let oldDeveloper = defaults.object(forKey: DaymarkAPI.developerKey)
        let oldHost = defaults.object(forKey: DaymarkAPI.accountHostKey)
        defaults.set(true, forKey: DaymarkAPI.developerKey)
        defaults.set("http://127.0.0.1:8080", forKey: DaymarkAPI.accountHostKey)
        defer {
            if let oldDeveloper { defaults.set(oldDeveloper, forKey: DaymarkAPI.developerKey) } else { defaults.removeObject(forKey: DaymarkAPI.developerKey) }
            if let oldHost { defaults.set(oldHost, forKey: DaymarkAPI.accountHostKey) } else { defaults.removeObject(forKey: DaymarkAPI.accountHostKey) }
            StubProtocol.handler = nil
        }
        let configuration = URLSessionConfiguration.ephemeral; configuration.protocolClasses = [StubProtocol.self]
        let network = URLSession(configuration: configuration); defer { network.invalidateAndCancel() }
        let expires = Int64(Date().timeIntervalSince1970 * 1000) + 3_600_000
        let user = UserSession(id: 1, name: "旧昵称", token: "token", tokenExpire: expires, refreshToken: "refresh", refreshExpire: expires)
        var stored: Data? = try JSONEncoder().encode(user)
        let api = DaymarkAPI(urlSession: network, loadSessionData: { stored }, saveSessionData: { stored = $0 }, clearSessionData: { stored = nil })
        StubProtocol.handler = { request in
            XCTAssertEqual(request.url?.host, "127.0.0.1")
            XCTAssertEqual(request.value(forHTTPHeaderField: "Host"), "account.creepersan.com")
            return Data("{\"code\":200,\"data\":{\"user\":{\"id\":1,\"nickname\":\"新昵称\"}}}".utf8)
        }
        try await api.updateNickname("新昵称")
        XCTAssertEqual(api.session?.name, "新昵称")
        XCTAssertNil(DaymarkAPI.developmentHostHeader(address: "https://proxy.example.com", canonicalAddress: DaymarkAPI.accountHost))
        XCTAssertEqual(DaymarkAPI.developmentHostHeader(address: "http://10.0.2.2:8080", canonicalAddress: DaymarkAPI.daymarkHost), "daymark.creepersan.com")
    }

    /// 昵称接口不存在时保留原会话，并返回可展示给用户的明确错误。
    @MainActor
    func testNicknameEndpointUnavailable() async throws {
        let configuration = URLSessionConfiguration.ephemeral; configuration.protocolClasses = [StubProtocol.self]
        let network = URLSession(configuration: configuration)
        defer { network.invalidateAndCancel(); StubProtocol.handler = nil; StubProtocol.statusCode = 200 }
        let expires = Int64(Date().timeIntervalSince1970 * 1000) + 3_600_000
        let user = UserSession(id: 1, name: "原昵称", token: "token", tokenExpire: expires, refreshToken: "refresh", refreshExpire: expires)
        var stored: Data? = try JSONEncoder().encode(user)
        let api = DaymarkAPI(urlSession: network, loadSessionData: { stored }, saveSessionData: { stored = $0 }, clearSessionData: { stored = nil })
        StubProtocol.statusCode = 404
        StubProtocol.handler = { _ in Data("not found".utf8) }
        do { try await api.updateNickname("新昵称"); XCTFail("接口不存在时应当返回错误") }
        catch DaymarkError.userUpdateUnavailable { }
        XCTAssertEqual(api.session?.name, "原昵称")
    }

    /// 保存成功后发布服务端返回的完整纪念日，首页无需重新请求列表。
    @MainActor
    func testSavePublishesServerAnniversary() async throws {
        let configuration = URLSessionConfiguration.ephemeral; configuration.protocolClasses = [StubProtocol.self]
        let network = URLSession(configuration: configuration)
        defer { network.invalidateAndCancel(); StubProtocol.handler = nil }
        let expires = Int64(Date().timeIntervalSince1970 * 1000) + 3_600_000
        let user = UserSession(id: 1, name: "测试", token: "token", tokenExpire: expires, refreshToken: "refresh", refreshExpire: expires)
        let data = try JSONEncoder().encode(user)
        let api = DaymarkAPI(urlSession: network, loadSessionData: { data }, saveSessionData: { _ in }, clearSessionData: {})
        StubProtocol.handler = { request in
            XCTAssertEqual(request.url?.path, "/api/anniversary/add")
            return Data("{\"code\":200,\"data\":{\"date\":{\"id\":7,\"name\":\"纪念日\",\"description\":\"\",\"calendar_type\":\"solar\",\"loop_type\":\"yearly\",\"year\":2026,\"month\":1,\"day\":1,\"is_leap_month\":0,\"next_timestamp\":123}}}".utf8)
        }
        try await api.save(Anniversary(name: "纪念日"))
        XCTAssertEqual(api.lastSavedAnniversary?.id, 7)
        XCTAssertEqual(api.lastSavedAnniversary?.nextTimestamp, 123)
    }

    /// 各语言的动态验证码和倒计时文案必须保留格式参数。
    func testLocalizedDynamicMessages() throws {
        let languages = ["zh-Hans", "zh-Hant", "yue-Hans", "yue-Hant", "en", "ja", "ko"]
        let formats = ["验证码已发送至 %@，5 分钟内有效。": "%@", "没有收到？%lld 秒后可重新发送": "%lld", "还有 %lld 天": "%lld", "已过去 %lld 天": "%lld"]
        for language in languages {
            let path = try XCTUnwrap(Bundle.main.path(forResource: language, ofType: "lproj"))
            let bundle = try XCTUnwrap(Bundle(path: path))
            for (key, placeholder) in formats {
                let value = bundle.localizedString(forKey: key, value: "__missing__", table: nil)
                XCTAssertNotEqual(value, "__missing__", "\(language): \(key)")
                XCTAssertTrue(value.contains(placeholder), "\(language): \(key)")
            }
        }
    }

    /// 调试网络日志保留状态和普通字段，递归隐藏凭据及联系方式。
    func testNetworkLogRedactsCredentials() throws {
        let body = Data("{\"code\":200,\"data\":{\"nickname\":\"昵称\",\"refresh_token\":\"refresh-secret\",\"email\":\"a@example.com\",\"items\":[{\"password\":\"pass-secret\",\"code\":\"123456\"}]}}".utf8)
        let result = NetworkLogFormatter.json(body, preserveStatusCode: true)
        XCTAssertTrue(result.contains("昵称")); XCTAssertTrue(result.contains("200"))
        for secret in ["refresh-secret", "a@example.com", "pass-secret", "123456"] { XCTAssertFalse(result.contains(secret)) }
        XCTAssertFalse(NetworkLogFormatter.json(Data("{\"code\":123456}".utf8)).contains("123456"))
        let url = try XCTUnwrap(URL(string: "https://user:pass@example.com/path?token=secret&count=3"))
        let address = NetworkLogFormatter.url(url)
        XCTAssertFalse(address.contains("secret")); XCTAssertFalse(address.contains("user")); XCTAssertFalse(address.contains("pass"))
        XCTAssertTrue(address.contains("count=3"))
        XCTAssertFalse(NetworkLogFormatter.json(Data("not-json-secret".utf8)).contains("secret"))
    }

    /// URLSession 可能将 JSON 请求体转换为输入流。
    private static func jsonBody(_ request: URLRequest) throws -> [String: String] {
        let data: Data
        if let body = request.httpBody { data = body }
        else {
            let stream = try XCTUnwrap(request.httpBodyStream)
            stream.open(); defer { stream.close() }
            var body = Data(); var buffer = [UInt8](repeating: 0, count: 1024)
            while stream.hasBytesAvailable {
                let count = stream.read(&buffer, maxLength: buffer.count)
                if count <= 0 { break }
                body.append(contentsOf: buffer.prefix(count))
            }
            data = body
        }
        return try XCTUnwrap(JSONSerialization.jsonObject(with: data) as? [String: String])
    }
}

/// 仅用于测试的 URL 加载器；所有响应都留在本机。
private final class StubProtocol: URLProtocol {
    static var handler: ((URLRequest) throws -> Data)?
    static var statusCode = 200
    override class func canInit(with request: URLRequest) -> Bool { true }
    override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }
    override func startLoading() {
        do {
            let data = try XCTUnwrap(Self.handler)(request)
            let response = HTTPURLResponse(url: request.url!, statusCode: Self.statusCode, httpVersion: nil, headerFields: nil)!
            client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
            client?.urlProtocol(self, didLoad: data)
            client?.urlProtocolDidFinishLoading(self)
        } catch { client?.urlProtocol(self, didFailWithError: error) }
    }
    override func stopLoading() {}
}
