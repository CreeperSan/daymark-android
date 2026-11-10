import Foundation
import UIKit

/// 服务端返回的用户资料和令牌，过期时间为毫秒时间戳。
struct UserSession: Codable {
    var id: Int64
    var name: String
    var token: String
    var tokenExpire: Int64
    var refreshToken: String
    var refreshExpire: Int64
    var account: String? = nil
    var email: String? = nil
}

private struct ServerToken: Decodable {
    let token: String
    let tokenExpireTime: Int64
    let refreshToken: String
    let refreshTokenExpireTime: Int64
    enum CodingKeys: String, CodingKey { case token; case tokenExpireTime = "token_expire_time", refreshToken = "refresh_token", refreshTokenExpireTime = "refresh_token_expire_time" }
}
private struct ServerUser: Decodable { let id: Int64; let nickname: String?; let account: String?; let email: String? }
private struct LoginData: Decodable { let user: ServerUser; let token: ServerToken }
private struct RefreshData: Decodable { let token: ServerToken }
private struct UserUpdateData: Decodable { let user: ServerUser }
private struct ListData: Decodable { let list: [Anniversary] }
private struct SaveData: Decodable { let date: Anniversary }
private struct EmptyData: Decodable {}
private struct ResponseStatus: Decodable { let code: Int; let msg: String? }
private struct Envelope<T: Decodable>: Decodable { let code: Int; let msg: String?; let data: T? }

/// 网络错误包含服务端信息，令牌和密码不会被写入日志。
enum DaymarkError: LocalizedError {
    case response(String), server(Int, String), authentication, invalidData, userUpdateUnavailable
    var errorDescription: String? {
        switch self { case .response(let message), .server(_, let message): return message; case .authentication: return "登录已过期，请重新登录"; case .invalidData: return "服务器返回的数据无效"; case .userUpdateUnavailable: return "当前账号服务尚未开放此功能。" }
    }
}

/// 与 Android 使用相同的账号和纪念日接口，支持令牌刷新和分页。
@MainActor
final class DaymarkAPI: ObservableObject {
    static let accountHost = "https://account.creepersan.com"
    static let daymarkHost = "https://daymark.creepersan.com"
    static let developerKey = "developerMode"
    static let accountHostKey = "accountHost"
    static let serverHostKey = "serverHost"
    private static let appVersion = "30000"
    private static let pageSize = 100
    private static let refreshAheadMilliseconds: Int64 = 60_000
    private static let requestTimeout: TimeInterval = 30
    private static let deviceKey = "daymark.device.id"
    @Published private(set) var session: UserSession?
    @Published private(set) var lastSavedAnniversary: Anniversary?
    @Published private(set) var lastDeletedAnniversaryID: Int64?
    @Published var toastMessage: String?
    @Published var passwordChangeSucceeded = false
    private var refreshTask: Task<UserSession, Error>?
    private var generation = 0
    private let deviceID: String
    private let urlSession: URLSession
    private let saveSessionData: (Data) throws -> Void
    private let clearSessionData: () -> Void

    /// 只有主动开启开发者模式时才使用已配置的代理地址。
    private func host(_ fallback: String, key: String) -> String {
        let defaults = UserDefaults.standard
        guard defaults.bool(forKey: Self.developerKey), let value = defaults.string(forKey: key), !value.isEmpty else { return fallback }
        return value
    }

    /// 直连本机名或 IP 时保留业务域名，供开发服务按 Host 分发路由。
    static func developmentHostHeader(address: String, canonicalAddress: String) -> String? {
        guard let hostname = URLComponents(string: address)?.host?.lowercased(), let canonical = URLComponents(string: canonicalAddress)?.host else { return nil }
        let isIPv4 = hostname.contains(".") && hostname.allSatisfy { $0.isNumber || $0 == "." }
        return hostname == "localhost" || hostname.contains(":") || isIPv4 ? canonical : nil
    }

    init(urlSession: URLSession = .shared, loadSessionData: () -> Data? = KeychainStore.load, saveSessionData: @escaping (Data) throws -> Void = KeychainStore.save, clearSessionData: @escaping () -> Void = KeychainStore.clear) {
        self.urlSession = urlSession
        self.saveSessionData = saveSessionData
        self.clearSessionData = clearSessionData
        if let stored = UserDefaults.standard.string(forKey: Self.deviceKey) { deviceID = stored }
        else { let id = UUID().uuidString; UserDefaults.standard.set(id, forKey: Self.deviceKey); deviceID = id }
        if let data = loadSessionData() { session = try? JSONDecoder().decode(UserSession.self, from: data) }
    }

    private func persist(_ value: UserSession) throws {
        try saveSessionData(JSONEncoder().encode(value))
        session = value
    }

    /// 注销仅清理本机令牌，不影响其他设备或经典版数据。
    func logout() { DaymarkLog.event("退出登录，清除本机会话"); generation += 1; refreshTask?.cancel(); refreshTask = nil; session = nil; lastSavedAnniversary = nil; lastDeletedAnniversaryID = nil; clearSessionData() }

    private func request<T: Decodable>(_ host: String, _ path: String, body: [String: Any], token: String? = nil, query: [URLQueryItem] = []) async throws -> T? {
        let resolvedHost = host == Self.accountHost ? self.host(host, key: Self.accountHostKey) : self.host(host, key: Self.serverHostKey)
        guard var components = URLComponents(string: resolvedHost + path) else { throw DaymarkError.invalidData }
        if !query.isEmpty { components.queryItems = query }
        guard let url = components.url else { throw DaymarkError.invalidData }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.timeoutInterval = Self.requestTimeout
        if let routingHost = Self.developmentHostHeader(address: resolvedHost, canonicalAddress: host) { request.setValue(routingHost, forHTTPHeaderField: "Host") }
        request.httpBody = try JSONSerialization.data(withJSONObject: body)
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("Daymark", forHTTPHeaderField: "CS-App")
        request.setValue(deviceID, forHTTPHeaderField: "CS-Device-Code")
        request.setValue(UIDevice.current.model, forHTTPHeaderField: "CS-Device-Name")
        request.setValue("iOS", forHTTPHeaderField: "CS-Device-Platform")
        request.setValue(Self.appVersion, forHTTPHeaderField: "CS-Version")
        if let token { request.setValue(token, forHTTPHeaderField: "CS-Token") }
        DaymarkLog.event("请求开始：POST \(path)")
        DaymarkLog.network("请求 POST \(NetworkLogFormatter.url(url))\n路由 Host：\(request.value(forHTTPHeaderField: "Host") ?? url.host ?? "")\n参数：\(NetworkLogFormatter.json(request.httpBody))")
        let data: Data
        let response: URLResponse
        do { (data, response) = try await urlSession.data(for: request) }
        catch {
            DaymarkLog.event("请求失败：\(path)，错误类型 \(type(of: error))")
            DaymarkLog.network("请求失败 \(NetworkLogFormatter.url(url))：\(type(of: error))")
            throw error
        }
        let statusCode = (response as? HTTPURLResponse)?.statusCode ?? 0
        DaymarkLog.event("请求完成：\(path)，HTTP \(statusCode)")
        DaymarkLog.network("响应 \(NetworkLogFormatter.url(url))：HTTP \(statusCode)\n结果：\(NetworkLogFormatter.json(data, preserveStatusCode: true))")
        if path == "/api/user/update" && statusCode == 404 { throw DaymarkError.userUpdateUnavailable }
        guard let http = response as? HTTPURLResponse, (200...299).contains(http.statusCode) else { throw DaymarkError.response("网络请求失败") }
        // 失败响应的 data 结构可能与成功结果不同，必须先判断错误码。
        let status = try JSONDecoder().decode(ResponseStatus.self, from: data)
        guard status.code == 200 else { throw DaymarkError.server(status.code, status.msg ?? "请求失败") }
        let result = try JSONDecoder().decode(Envelope<T>.self, from: data)
        return result.data
    }

    func login(account: String, password: String) async throws {
        let currentGeneration = generation
        guard let data: LoginData = try await request(Self.accountHost, "/api/login", body: ["account": account, "password": password]) else { throw DaymarkError.invalidData }
        guard generation == currentGeneration else { throw CancellationError() }
        let token = data.token
        guard data.user.id > 0, !token.token.isEmpty, !token.refreshToken.isEmpty, token.tokenExpireTime > 0, token.refreshTokenExpireTime > 0 else { throw DaymarkError.invalidData }
        try persist(UserSession(id: data.user.id, name: data.user.nickname ?? "", token: token.token, tokenExpire: token.tokenExpireTime, refreshToken: token.refreshToken, refreshExpire: token.refreshTokenExpireTime, account: data.user.account, email: data.user.email))
    }

    func requestCode(email: String, register: Bool) async throws {
        let _: EmptyData? = try await request(Self.accountHost, register ? "/api/register/request_auth_code" : "/api/password/reset/request_auth_code", body: ["email": email])
    }
    func register(account: String, email: String, password: String, code: String) async throws {
        let _: EmptyData? = try await request(Self.accountHost, "/api/register", body: ["account": account, "email": email, "password": password, "code": code])
    }
    func resetPassword(email: String, password: String, code: String) async throws {
        let _: EmptyData? = try await request(Self.accountHost, "/api/password/reset", body: ["email": email, "new_password": password, "code": code])
    }

    /// 修改密码会使服务端所有令牌失效，成功后清除当前会话并提示重新登录。
    func changePassword(oldPassword: String, newPassword: String) async throws {
        let token = try await validSession().token
        let currentGeneration = generation
        let _: EmptyData? = try await request(Self.accountHost, "/api/password/change", body: ["old_password": oldPassword, "new_password": newPassword], token: token)
        // 请求期间若已切换账号，不得清除新账号的会话。
        guard generation == currentGeneration else { return }
        passwordChangeSucceeded = true
        logout()
    }

    /// 更新昵称并保存服务端返回的资料；账号切换后不覆盖新会话。
    func updateNickname(_ nickname: String) async throws {
        DaymarkLog.event("提交昵称修改")
        let currentGeneration = generation
        guard let userID = session?.id else { throw DaymarkError.authentication }
        let current = try await validSession()
        guard generation == currentGeneration, current.id == userID else { throw CancellationError() }
        guard let data: UserUpdateData = try await request(Self.accountHost, "/api/user/update", body: ["nickname": nickname], token: current.token),
              let nickname = data.user.nickname, data.user.id == userID else { throw DaymarkError.invalidData }
        guard generation == currentGeneration, var latest = session, latest.id == userID else { throw CancellationError() }
        latest.name = nickname
        try persist(latest)
        DaymarkLog.event("昵称修改成功")
    }

    private func validSession() async throws -> UserSession {
        guard let current = session else { throw DaymarkError.authentication }
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        if current.tokenExpire > now + Self.refreshAheadMilliseconds { return current }
        guard current.refreshExpire > now, !current.refreshToken.isEmpty else { logout(); throw DaymarkError.authentication }
        if let refreshTask { return try await refreshTask.value }
        let currentGeneration = generation
        let task = Task { () throws -> UserSession in
            guard let data: RefreshData = try await request(Self.accountHost, "/api/token/refresh", body: ["refresh_token": current.refreshToken], token: current.token) else { throw DaymarkError.invalidData }
            guard generation == currentGeneration else { throw CancellationError() }
            let token = data.token
            guard !token.token.isEmpty, !token.refreshToken.isEmpty, token.tokenExpireTime > 0, token.refreshTokenExpireTime > 0 else { throw DaymarkError.invalidData }
            guard var updated = session, updated.id == current.id else { throw CancellationError() }
            // 刷新期间昵称可能已更新，保留最新资料，只替换令牌。
            updated.token = token.token; updated.tokenExpire = token.tokenExpireTime
            updated.refreshToken = token.refreshToken; updated.refreshExpire = token.refreshTokenExpireTime
            try persist(updated)
            return updated
        }
        refreshTask = task
        // 新会话可能已经启动了另一轮刷新，不能清除其任务。
        defer { if generation == currentGeneration { refreshTask = nil } }
        do { return try await task.value }
        catch DaymarkError.server(let code, _) where (code == 400 || code == 401) && generation == currentGeneration {
            // 只有服务端明确拒绝刷新时才清除会话；网络故障不能让用户意外退出。
            logout()
            throw DaymarkError.authentication
        }
    }

    func list() async throws -> [Anniversary] {
        let token = try await validSession().token
        let _: EmptyData? = try await request(Self.daymarkHost, "/api/anniversary/refresh", body: ["force": 0], token: token)
        var page = 1
        var all: [Anniversary] = []
        while true {
            guard let result: ListData = try await request(Self.daymarkHost, "/api/anniversary/list", body: [:], token: token, query: [URLQueryItem(name: "page", value: String(page)), URLQueryItem(name: "page_size", value: String(Self.pageSize))]) else { throw DaymarkError.invalidData }
            all += result.list
            if result.list.count < Self.pageSize { break }
            page += 1
        }
        return all
    }

    func get(id: Int64) async throws -> Anniversary {
        let token = try await validSession().token
        guard let item: Anniversary = try await request(Self.daymarkHost, "/api/anniversary/get", body: ["id": id], token: token) else { throw DaymarkError.invalidData }
        return item
    }

    /// 删除成功后通知首页，避免旧列表响应重新显示已删除的记录。
    func delete(id: Int64) async throws {
        let currentGeneration = generation
        let token = try await validSession().token
        guard currentGeneration == generation else { throw CancellationError() }
        let _: EmptyData? = try await request(Self.daymarkHost, "/api/anniversary/delete", body: ["id": id], token: token)
        guard currentGeneration == generation else { throw CancellationError() }
        lastDeletedAnniversaryID = id
    }

    func save(_ item: Anniversary) async throws {
        let currentGeneration = generation
        let token = try await validSession().token
        guard currentGeneration == generation else { throw CancellationError() }
        var body: [String: Any] = ["name": item.name, "description": item.description, "calendar_type": item.calendarType, "loop_type": item.loopType, "year": item.year, "month": item.month, "day": item.day, "is_leap_month": item.isLeapMonth]
        if item.id > 0 { body["id"] = item.id }
        let result: SaveData? = try await request(Self.daymarkHost, item.id > 0 ? "/api/anniversary/edit" : "/api/anniversary/add", body: body, token: token)
        guard let result else { throw DaymarkError.invalidData }
        guard currentGeneration == generation else { throw CancellationError() }
        // 将服务端返回的完整纪念日交给首页，页面返回时无需刷新列表。
        lastSavedAnniversary = result.date
    }
}
