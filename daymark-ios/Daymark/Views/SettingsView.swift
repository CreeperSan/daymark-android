import SwiftUI

private let developerURL = URL(string: "https://creepersan.com")!
private let contactEmail = "admin@creepersan.com"

/// 设置页；iOS 没有经典版和模式切换入口。
struct SettingsView: View {
    @ObservedObject var api: DaymarkAPI
    @Binding var path: [DaymarkRoute]
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.openURL) private var openURL
    @Environment(\.locale) private var locale
    @AppStorage("theme") private var theme = "system"
    @AppStorage("language") private var language = "system"
    @AppStorage(DaymarkAPI.developerKey) private var developer = false
    @AppStorage(DaymarkAPI.accountHostKey) private var accountHost = ""
    @AppStorage(DaymarkAPI.serverHostKey) private var serverHost = ""
    @State private var selection: Selection?
    @State private var confirmLogout = false
    @State private var confirmDelete = false
    @State private var mailUnavailable = false
    @State private var titleTaps = 0
    @State private var lastTitleTap = Date.distantPast
    private let developerTapCount = 8
    private let developerTapInterval: TimeInterval = 0.5
    private let legalURL = URL(string: "https://blog.creepersan.com/article/61")!
    private let languages = [("system", "跟随系统"), ("zh-CN", "简体中文"), ("zh-TW", "繁體中文"), ("yue-Hans-CN", "简体粤语（广东）"), ("yue-Hant-HK", "繁體粵語（香港）"), ("en", "English"), ("ja", "日本語"), ("ko", "한국어")]
    enum Selection: String, Identifiable { case theme, language, nickname, accountHost, serverHost; var id: String { rawValue } }
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                if api.session != nil {
                    section("个人信息") {
                        rowLabel("用户名", value: api.session?.account.flatMap { $0.isEmpty ? nil : $0 } ?? "—", showArrow: false)
                        rowLabel("邮箱", value: api.session?.email.flatMap { $0.isEmpty ? nil : $0 } ?? "—", showArrow: false)
                        row("昵称", value: api.session.map { $0.name }.flatMap { $0.isEmpty ? nil : $0 } ?? "—") { selection = .nickname }
                        row("修改密码") { path.append(.changePassword) }
                        row("删除账号", danger: true) { confirmDelete = true }
                        Button("退出登录") { DaymarkLog.event("打开退出登录确认"); confirmLogout = true }.foregroundStyle(.red.opacity(0.8)).frame(maxWidth: .infinity).frame(height: 56).buttonStyle(.plain)
                    }
                }
                section("应用") {
                    row("主题", value: theme == "light" ? "亮色" : theme == "dark" ? "暗色" : "跟随系统") { selection = .theme }
                    row("语言", value: languages.first { $0.0 == language }?.1 ?? "跟随系统") { selection = .language }
                }
                if developer {
                    section("开发者设置") {
                        row("账号代理设置") { selection = .accountHost }
                        row("服务器代理设置") { selection = .serverHost }
                        row("关闭开发者模式") { developer = false }
                    }
                }
                section("其他") {
                    row("应用版本", value: Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "") {}
                    row("关于应用") { path.append(.about) }
                    Link(destination: legalURL) { rowLabel("隐私政策") }
                    Link(destination: legalURL) { rowLabel("服务协议") }
                }
                Text("Copyright 2026 纪念日 · 保留所有权利").font(.system(size: 11)).foregroundStyle(.secondary).frame(maxWidth: .infinity).padding(.vertical, 12)
            }.padding(.horizontal, 20).padding(.top, 12).padding(.bottom, 24)
        }
        .scrollIndicators(.hidden)
        .safeAreaInset(edge: .top, spacing: 0) { GlassHeader(title: "设置", back: { dismiss() }, onTitleTap: tapTitle) }
        .sheet(item: $selection) { selected in
            if selected == .nickname {
                NicknameSheet(api: api) { selection = nil }.presentationDetents([.medium]).presentationDragIndicator(.visible)
            } else if selected == .accountHost || selected == .serverHost {
                HostSheet(title: selected == .accountHost ? "账号代理设置" : "服务器代理设置", value: selected == .accountHost ? accountHost : serverHost) { value in
                    if selected == .accountHost { accountHost = value } else { serverHost = value }
                    selection = nil
                }.presentationDetents([.medium]).presentationDragIndicator(.visible)
            } else {
                SelectionSheet(title: selected == .theme ? "主题" : "语言", options: selected == .theme ? [("system", "跟随系统"), ("light", "亮色"), ("dark", "暗色")] : languages, value: selected == .theme ? theme : language) { value in
                    if selected == .theme { theme = value } else { language = value }
                    selection = nil
                }.presentationDetents([.medium, .large]).presentationDragIndicator(.visible)
            }
        }
        .alert("退出登录", isPresented: $confirmLogout) {
            Button("取消", role: .cancel) {}
            Button("确定", role: .destructive) { api.logout(); path.removeAll() }
        } message: { Text("确定要退出登录吗？") }
        .alert("删除账号", isPresented: $confirmDelete) {
            Button("取消", role: .cancel) {}
            Button("确定", role: .destructive) { DaymarkLog.event("确认申请删除账号"); composeDeletionMail() }
        } message: { Text("删除账号将导致你的账号所有数据都将永久删除，确定要继续吗？") }
        .alert("无法打开邮件应用", isPresented: $mailUnavailable) { Button("好", role: .cancel) {} }
    }
    /// 使用当前应用语言生成邮件；旧会话缺少资料时提示用户自行填写。
    private func composeDeletionMail() {
        let identifier = locale.identifier.replacingOccurrences(of: "_", with: "-")
        let language: String
        if identifier.hasPrefix("yue") { language = identifier.contains("Hant") || identifier.hasSuffix("HK") ? "yue-Hant" : "yue-Hans" }
        else if identifier.hasPrefix("zh") { language = identifier.contains("Hant") || identifier.hasSuffix("TW") || identifier.hasSuffix("HK") ? "zh-Hant" : "zh-Hans" }
        else { language = locale.language.languageCode?.identifier ?? "zh-Hans" }
        let path = Bundle.main.path(forResource: language, ofType: "lproj")
        let bundle = path.flatMap(Bundle.init(path:)) ?? .main
        let missing = bundle.localizedString(forKey: "请填写", value: nil, table: nil)
        let format = bundle.localizedString(forKey: "删除账号邮件内容", value: nil, table: nil)
        let session = api.session
        let body = String(format: format, session?.account.flatMap { $0.isEmpty ? nil : $0 } ?? missing, session?.email.flatMap { $0.isEmpty ? nil : $0 } ?? missing, session.map { String($0.id) } ?? missing)
        var components = URLComponents()
        components.scheme = "mailto"; components.path = contactEmail
        components.queryItems = [URLQueryItem(name: "subject", value: bundle.localizedString(forKey: "请求删除我的账号", value: nil, table: nil)), URLQueryItem(name: "body", value: body)]
        guard let url = components.url else { mailUnavailable = true; return }
        openURL(url) { accepted in if !accepted { mailUnavailable = true } }
    }
    /// 和 Android 一致，连续点击标题开启开发者设置。
    private func tapTitle() {
        let now = Date()
        titleTaps = now.timeIntervalSince(lastTitleTap) <= developerTapInterval ? titleTaps + 1 : 1
        lastTitleTap = now
        if titleTaps >= developerTapCount { developer = true; titleTaps = 0 }
    }
    private func section<Content: View>(_ title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(LocalizedStringKey(title)).font(.system(size: 12)).foregroundStyle(.secondary)
            VStack(spacing: 0, content: content).padding(6).modifier(GlassSurface(radius: 20))
        }
    }
    private func row(_ title: String, value: String = "", danger: Bool = false, action: @escaping () -> Void) -> some View {
        Button { DaymarkLog.event("点击设置项：\(title)"); action() } label: { rowLabel(title, value: value, danger: danger) }.buttonStyle(.plain)
    }
    private func rowLabel(_ title: String, value: String = "", danger: Bool = false, showArrow: Bool = true) -> some View {
        HStack { Text(LocalizedStringKey(title)).foregroundStyle(danger ? Color.red : colorScheme == .dark ? Color.white : GlassPalette.primaryLight); Spacer(); Group { if title == "主题" || title == "语言" { Text(LocalizedStringKey(value)) } else { Text(verbatim: value) } }.font(.system(size: 13)).foregroundStyle(.secondary); if showArrow { Image(systemName: "chevron.right").font(.system(size: 11)).foregroundStyle(.secondary) } }.font(.system(size: 14)).padding(.horizontal, 12).frame(height: 56).contentShape(Rectangle())
    }
}

/// 昵称保存成功后关闭弹窗，失败时保留输入以便重试。
struct NicknameSheet: View {
    @ObservedObject var api: DaymarkAPI
    let onSaved: () -> Void
    @State private var value: String
    @State private var loading = false
    @State private var error: String?
    @State private var showingError = false
    init(api: DaymarkAPI, onSaved: @escaping () -> Void) {
        self.api = api; self.onSaved = onSaved; _value = State(initialValue: api.session?.name ?? "")
    }
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("昵称").font(.system(size: 18, weight: .semibold))
            GlassInput(placeholder: "昵称", text: $value)
            if let error { Text(LocalizedStringKey(error)).font(.system(size: 12)).foregroundStyle(.red) }
            GlassPrimaryButton(title: loading ? "保存中…" : "保存", disabled: loading || value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty) {
                Task { await save() }
            }
            Spacer()
        }.padding(24).interactiveDismissDisabled(loading)
            .alert("保存失败，请重试。", isPresented: $showingError) { Button("好", role: .cancel) {} } message: { Text(LocalizedStringKey(error ?? "保存失败，请重试。")) }
    }
    private func save() async {
        guard !loading else { return }
        loading = true; error = nil; defer { loading = false }
        do { try await api.updateNickname(value.trimmingCharacters(in: .whitespacesAndNewlines)); onSaved() }
        catch { DaymarkLog.event("昵称修改失败：\(type(of: error))"); self.error = error.localizedDescription; showingError = true }
    }
}

/// 开发代理支持 HTTP 和 HTTPS，可留空恢复默认地址。
struct HostSheet: View {
    let title: String
    let onSave: (String) -> Void
    @State private var value: String
    @State private var invalid = false
    init(title: String, value: String, onSave: @escaping (String) -> Void) {
        self.title = title; self.onSave = onSave; _value = State(initialValue: value)
    }
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(LocalizedStringKey(title)).font(.system(size: 18, weight: .semibold))
            Text("留空使用默认服务器，仅在开发者模式下生效。").font(.system(size: 12)).foregroundStyle(.secondary)
            GlassInput(placeholder: title, text: $value).keyboardType(.URL)
            if invalid { Text("请输入有效的 HTTP 或 HTTPS 地址").font(.system(size: 12)).foregroundStyle(.red) }
            GlassPrimaryButton(title: "保存") {
                let host = value.trimmingCharacters(in: .whitespacesAndNewlines).trimmingCharacters(in: CharacterSet(charactersIn: "/"))
                let components = URLComponents(string: host)
                guard host.isEmpty || (["http", "https"].contains(components?.scheme?.lowercased() ?? "") && components?.host?.isEmpty == false && components?.user == nil && components?.password == nil && components?.query == nil && components?.fragment == nil) else { invalid = true; return }
                onSave(host)
            }
            Spacer()
        }.padding(24)
    }
}

/// 主题和语言使用模态选择，选择后立即保存。
struct SelectionSheet: View {
    let title: String
    let options: [(String, String)]
    let value: String
    let onSelect: (String) -> Void
    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(LocalizedStringKey(title)).font(.system(size: 18, weight: .semibold)).padding(.bottom, 12)
            ScrollView { VStack(spacing: 0) { ForEach(options, id: \.0) { option in
                Button { DaymarkLog.event("选择设置选项：\(title)"); onSelect(option.0) } label: { HStack { Text(LocalizedStringKey(option.1)); Spacer(); if value == option.0 { Image(systemName: "checkmark").foregroundStyle(GlassPalette.accent) } }.frame(maxWidth: .infinity).frame(height: 48).contentShape(Rectangle()) }.buttonStyle(.plain)
            } } }
        }.padding(24)
    }
}

struct AboutView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.openURL) private var openURL
    @State private var mailUnavailable = false
    var body: some View {
        ScrollView {
            VStack(spacing: 14) {
                Image(systemName: "heart.fill").font(.system(size: 36)).foregroundStyle(GlassPalette.accent).frame(width: 88, height: 88).modifier(GlassSurface(radius: 30))
                Text("纪念日").font(.system(size: 24, weight: .semibold))
                Text("把时间，过成值得纪念的样子。").font(.system(size: 14)).foregroundStyle(GlassPalette.accent)
                VStack(alignment: .leading, spacing: 8) {
                    Text("为什么会有这个 App").font(.system(size: 14, weight: .semibold))
                    Text("我们总觉得日子还很长，回头才发现，很多重要的时刻已经慢慢变模糊。\n\n纪念日想做的只有一件事：把它们收好。第一次见面、第一次旅行、第一次吵架又和好，都值得被认真地放在一个地方，让时间替你数着。")
                        .font(.system(size: 13)).foregroundStyle(.secondary)
                }.frame(maxWidth: .infinity, alignment: .leading).padding(20).modifier(GlassSurface())
                VStack(spacing: 0) {
                    HStack { Text("当前版本"); Spacer(); Text(Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "") }.frame(height: 40)
                    Button { DaymarkLog.event("打开开发者网站"); openURL(developerURL) } label: { HStack { Text("开发者"); Spacer(); Image(systemName: "chevron.right").font(.system(size: 11)) }.frame(height: 40) }.buttonStyle(.plain)
                    Button { DaymarkLog.event("打开联系邮件"); openURL(URL(string: "mailto:\(contactEmail)")!) { if !$0 { mailUnavailable = true } } } label: { HStack { Text("联系我们"); Spacer(); Image(systemName: "chevron.right").font(.system(size: 11)) }.frame(height: 40) }.buttonStyle(.plain)
                }.font(.system(size: 12)).foregroundStyle(.secondary).padding(.horizontal, 16).modifier(GlassSurface(radius: 20))
                Link("隐私政策 · 服务协议", destination: URL(string: "https://blog.creepersan.com/article/61")!).font(.system(size: 12)).foregroundStyle(GlassPalette.accent)
                Text("Copyright 2026 纪念日 · 保留所有权利").font(.system(size: 11)).foregroundStyle(.secondary)
            }.padding(24)
        }.scrollIndicators(.hidden).safeAreaInset(edge: .top, spacing: 0) { GlassHeader(title: "关于应用", back: { dismiss() }) }.alert("无法打开邮件应用", isPresented: $mailUnavailable) { Button("好", role: .cancel) {} }
    }
}
