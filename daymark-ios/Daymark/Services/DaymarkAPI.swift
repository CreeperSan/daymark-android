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
}

private struct ServerToken: Decodable {
    let token: String
    let tokenExpireTime: Int64
    let refreshToken: String
    let refreshTokenExpireTime: Int64
    enum CodingKeys: String, CodingKey { case token; case tokenExpireTime = "token_expire_time", refreshToken = "refresh_token", refreshTokenExpireTime = "refresh_token_expire_time" }
}
private struct ServerUser: Decodable { let id: Int64; let nickname: String? }
private struct LoginData: Decodable { let user: ServerUser; let token: ServerToken }
private struct RefreshData: Decodable { let token: ServerToken }
private struct ListData: Decodable { let list: [Anniversary] }
private struct SaveData: Decodable { let date: Anniversary }
private struct EmptyData: Decodable {}
private struct ResponseStatus: Decodable { let code: Int; let msg: String? }
private struct Envelope<T: Decodable>: Decodable { let code: Int; let msg: String?; let data: T? }

/// 网络错误包含服务端信息，令牌和密码不会被写入日志。
enum DaymarkError: LocalizedError {
    case response(String), server(Int, String), authentication, invalidData
    var errorDescription: String? {
        switch self { case .response(let message), .server(_, let message): return message; case .authentication: return "登录已过期，请重新登录"; case .invalidData: return "服务器返回的数据无效" }
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
    private static let deviceKey = "daymark.device.id"
    @Published private(set) var session: UserSession?
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
    func logout() { generation += 1; refreshTask?.cancel(); refreshTask = nil; session = nil; clearSessionData() }

    private func request<T: Decodable>(_ host: String, _ path: String, body: [String: Any], token: String? = nil, query: [URLQueryItem] = []) async throws -> T? {
        let resolvedHost = host == Self.accountHost ? self.host(host, key: Self.accountHostKey) : self.host(host, key: Self.serverHostKey)
        guard var components = URLComponents(string: resolvedHost + path) else { throw DaymarkError.invalidData }
        if !query.isEmpty { components.queryItems = query }
        guard let url = components.url else { throw DaymarkError.invalidData }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.httpBody = try JSONSerialization.data(withJSONObject: body)
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("Daymark", forHTTPHeaderField: "CS-App")
        request.setValue(deviceID, forHTTPHeaderField: "CS-Device-Code")
        request.setValue(UIDevice.current.model, forHTTPHeaderField: "CS-Device-Name")
        request.setValue("iOS", forHTTPHeaderField: "CS-Device-Platform")
        request.setValue(Self.appVersion, forHTTPHeaderField: "CS-Version")
        if let token { request.setValue(token, forHTTPHeaderField: "CS-Token") }
        let (data, response) = try await urlSession.data(for: request)
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
        try persist(UserSession(id: data.user.id, name: data.user.nickname ?? "", token: token.token, tokenExpire: token.tokenExpireTime, refreshToken: token.refreshToken, refreshExpire: token.refreshTokenExpireTime))
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
            let updated = UserSession(id: current.id, name: current.name, token: token.token, tokenExpire: token.tokenExpireTime, refreshToken: token.refreshToken, refreshExpire: token.refreshTokenExpireTime)
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

    func save(_ item: Anniversary) async throws {
        let token = try await validSession().token
        var body: [String: Any] = ["name": item.name, "description": item.description, "calendar_type": item.calendarType, "loop_type": item.loopType, "year": item.year, "month": item.month, "day": item.day, "is_leap_month": item.isLeapMonth]
        if item.id > 0 { body["id"] = item.id }
        let result: SaveData? = try await request(Self.daymarkHost, item.id > 0 ? "/api/anniversary/edit" : "/api/anniversary/add", body: body, token: token)
        guard result != nil else { throw DaymarkError.invalidData }
    }
}
