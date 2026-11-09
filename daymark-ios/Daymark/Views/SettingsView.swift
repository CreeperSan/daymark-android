import SwiftUI

/// 设置页；iOS 没有经典版和模式切换入口。
struct SettingsView: View {
    @ObservedObject var api: DaymarkAPI
    @Binding var path: [DaymarkRoute]
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var colorScheme
    @AppStorage("theme") private var theme = "system"
    @AppStorage("language") private var language = "system"
    @AppStorage(DaymarkAPI.developerKey) private var developer = false
    @AppStorage(DaymarkAPI.accountHostKey) private var accountHost = ""
    @AppStorage(DaymarkAPI.serverHostKey) private var serverHost = ""
    @State private var selection: Selection?
    @State private var developing = false
    @State private var titleTaps = 0
    @State private var lastTitleTap = Date.distantPast
    private let developerTapCount = 8
    private let developerTapInterval: TimeInterval = 0.5
    private let legalURL = URL(string: "https://blog.creepersan.com/article/61")!
    private let languages = [("system", "跟随系统"), ("zh-CN", "简体中文"), ("zh-TW", "繁體中文"), ("yue-Hans-CN", "简体粤语（广东）"), ("yue-Hant-HK", "繁體粵語（香港）"), ("en", "English"), ("ja", "日本語"), ("ko", "한국어")]
    enum Selection: String, Identifiable { case theme, language, accountHost, serverHost; var id: String { rawValue } }
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                if api.session != nil {
                    section("个人信息") {
                        row("头像") { developing = true }
                        row("用户名", value: api.session?.name ?? "—") { developing = true }
                        row("邮箱", value: "—") { developing = true }
                        row("修改密码") { path.append(.changePassword) }
                        row("删除账号", danger: true) { developing = true }
                        Button("退出登录") { api.logout(); path.removeAll() }.foregroundStyle(.red.opacity(0.8)).frame(maxWidth: .infinity).frame(height: 56).buttonStyle(.plain)
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
            if selected == .accountHost || selected == .serverHost {
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
        .modifier(DevelopmentNotice(presented: $developing))
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
        Button(action: action) { rowLabel(title, value: value, danger: danger) }.buttonStyle(.plain)
    }
    private func rowLabel(_ title: String, value: String = "", danger: Bool = false) -> some View {
        HStack { Text(LocalizedStringKey(title)).foregroundStyle(danger ? Color.red : colorScheme == .dark ? Color.white : GlassPalette.primaryLight); Spacer(); Group { if title == "主题" || title == "语言" { Text(LocalizedStringKey(value)) } else { Text(verbatim: value) } }.font(.system(size: 13)).foregroundStyle(.secondary); Image(systemName: "chevron.right").font(.system(size: 11)).foregroundStyle(.secondary) }.font(.system(size: 14)).padding(.horizontal, 12).frame(height: 56).contentShape(Rectangle())
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
                Button { onSelect(option.0) } label: { HStack { Text(LocalizedStringKey(option.1)); Spacer(); if value == option.0 { Image(systemName: "checkmark").foregroundStyle(GlassPalette.accent) } }.frame(maxWidth: .infinity).frame(height: 48).contentShape(Rectangle()) }.buttonStyle(.plain)
            } } }
        }.padding(24)
    }
}

struct AboutView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var developing = false
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
                    Button { developing = true } label: { HStack { Text("开发者"); Spacer(); Text("—") }.frame(height: 40) }.buttonStyle(.plain)
                    Button { developing = true } label: { HStack { Text("联系我们"); Spacer(); Text("—") }.frame(height: 40) }.buttonStyle(.plain)
                }.font(.system(size: 12)).foregroundStyle(.secondary).padding(.horizontal, 16).modifier(GlassSurface(radius: 20))
                Link("隐私政策 · 服务协议", destination: URL(string: "https://blog.creepersan.com/article/61")!).font(.system(size: 12)).foregroundStyle(GlassPalette.accent)
                Text("Copyright 2026 纪念日 · 保留所有权利").font(.system(size: 11)).foregroundStyle(.secondary)
            }.padding(24)
        }.scrollIndicators(.hidden).safeAreaInset(edge: .top, spacing: 0) { GlassHeader(title: "关于应用", back: { dismiss() }) }.modifier(DevelopmentNotice(presented: $developing))
    }
}
