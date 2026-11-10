import Foundation
import OSLog

/// 统一记录应用事件；文件异步写入，调试版本同时输出到控制台。
enum DaymarkLog {
    private static let logger = Logger(subsystem: "com.creepersan.memorialday", category: "Daymark")
    private static let queue = DispatchQueue(label: "com.creepersan.daymark.log")
    private static let maxFileSize: UInt64 = 500 * 1024
    private static let formatter: DateFormatter = {
        let value = DateFormatter(); value.locale = Locale(identifier: "en_US_POSIX"); value.dateFormat = "yyyy-MM-dd HH:mm:ss.SSS"; return value
    }()

    /// 日志内容仅包含事件名称和错误类型，不传入密码、令牌或用户输入。
    static func event(_ message: String) {
        logger.info("\(message, privacy: .public)")
        #if DEBUG
        print("[Daymark] \(message)")
        #endif
        let now = Date()
        queue.async {
            do {
                let manager = FileManager.default
                let base = try manager.url(for: .applicationSupportDirectory, in: .userDomainMask, appropriateFor: nil, create: true)
                let directory = base.appendingPathComponent("logs", isDirectory: true)
                try manager.createDirectory(at: directory, withIntermediateDirectories: true)
                let timestamp = formatter.string(from: now)
                let date = String(timestamp.prefix(10))
                let text = message.replacingOccurrences(of: "\\", with: "\\\\").replacingOccurrences(of: "\n", with: "\\n").replacingOccurrences(of: "\r", with: "\\r")
                let data = Data("\(timestamp) \(text)\n".utf8)
                var sequence = 1
                var file = directory.appendingPathComponent("\(date)-\(sequence).log")
                while manager.fileExists(atPath: file.path) {
                    let size = (try manager.attributesOfItem(atPath: file.path)[.size] as? NSNumber)?.uint64Value ?? 0
                    if size == 0 || size + UInt64(data.count) <= maxFileSize { break }
                    sequence += 1; file = directory.appendingPathComponent("\(date)-\(sequence).log")
                }
                if !manager.fileExists(atPath: file.path) { _ = manager.createFile(atPath: file.path, contents: nil) }
                let handle = try FileHandle(forWritingTo: file)
                defer { try? handle.close() }
                try handle.seekToEnd(); try handle.write(contentsOf: data)
            } catch { logger.error("日志文件写入失败") }
        }
    }

    /// 网络明细不写入文件，仅调试版本输出脱敏后的内容。
    static func network(_ message: @autoclosure () -> String) {
        #if DEBUG
        print("[DaymarkNetwork] \(message())")
        #endif
    }
}

/// 网络日志仅解析 JSON，未知内容不输出；递归遮盖认证信息和联系方式。
enum NetworkLogFormatter {
    private static let hidden = "[已隐藏]"
    private static let sensitiveKeys = ["password", "passwd", "token", "authorization", "cookie", "secret", "apikey", "authcode", "verificationcode", "code", "email", "account", "phone", "devicecode"]

    static func url(_ url: URL) -> String {
        guard var parts = URLComponents(url: url, resolvingAgainstBaseURL: false) else { return "[无效地址]" }
        parts.user = nil; parts.password = nil
        parts.queryItems = parts.queryItems?.map { sensitive($0.name) ? URLQueryItem(name: $0.name, value: hidden) : $0 }
        return parts.string ?? "[无效地址]"
    }

    static func json(_ data: Data?, preserveStatusCode: Bool = false) -> String {
        guard let data, let value = try? JSONSerialization.jsonObject(with: data), value is [String: Any] || value is [Any],
              let result = try? JSONSerialization.data(withJSONObject: redact(value, isRoot: preserveStatusCode), options: [.sortedKeys]), let text = String(data: result, encoding: .utf8) else { return "[非 JSON 内容已省略]" }
        return text
    }

    private static func sensitive(_ key: String) -> Bool {
        let normalized = key.lowercased().filter { $0.isLetter || $0.isNumber }
        return sensitiveKeys.contains { normalized == $0 || ($0 != "code" && normalized.contains($0)) }
    }

    private static func redact(_ value: Any, isRoot: Bool = false) -> Any {
        if let dictionary = value as? [String: Any] {
            return dictionary.mapValues { $0 }.reduce(into: [String: Any]()) { result, pair in
                let isStatusCode = isRoot && pair.key.lowercased() == "code" && pair.value is NSNumber && !(pair.value is Bool)
                result[pair.key] = sensitive(pair.key) && !isStatusCode ? hidden : redact(pair.value)
            }
        }
        if let array = value as? [Any] { return array.map { redact($0) } }
        return value
    }
}
