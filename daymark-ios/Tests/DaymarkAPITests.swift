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
    override class func canInit(with request: URLRequest) -> Bool { true }
    override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }
    override func startLoading() {
        do {
            let data = try XCTUnwrap(Self.handler)(request)
            let response = HTTPURLResponse(url: request.url!, statusCode: 200, httpVersion: nil, headerFields: nil)!
            client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
            client?.urlProtocol(self, didLoad: data)
            client?.urlProtocolDidFinishLoading(self)
        } catch { client?.urlProtocol(self, didFailWithError: error) }
    }
    override func stopLoading() {}
}
