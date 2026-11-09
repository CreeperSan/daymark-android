import SwiftUI
import UIKit

/// 首次启动的三页引导，提醒和共享功能明确标记为规划中。
struct OnboardingView: View {
    let onFinish: () -> Void
    let onLogin: () -> Void
    @State private var page = 0
    private let images = ["onboarding_record", "onboarding_remind", "onboarding_share"]
    private let titles = ["把值得记住的日子\n都留在同一个地方", "重要的日子，\n提前替你想起来", "和 TA 一起，\n写同一份回忆"]
    private let descriptions = ["相识、告白、旅行与每一个小小的纪念日，时间会替你们记得。", "提醒、日历订阅与桌面小组件功能正在规划中。", "共享纪念日功能正在规划中。"]
    var body: some View {
        VStack(spacing: 0) {
            HStack { Spacer(); Button("跳过", action: onFinish).foregroundStyle(GlassPalette.accent) }.padding(.top, 12)
            Spacer(minLength: 12)
            Image(images[page]).resizable().scaledToFit().frame(maxWidth: 290, maxHeight: 290)
            Text(LocalizedStringKey(titles[page])).font(.system(size: 26, weight: .semibold)).multilineTextAlignment(.center).padding(.top, 24)
            Text(LocalizedStringKey(descriptions[page])).font(.system(size: 14)).foregroundStyle(.secondary).multilineTextAlignment(.center).padding(.top, 14)
            Spacer()
            HStack(spacing: 7) { ForEach(0..<3) { index in Capsule().fill(index == page ? GlassPalette.accent : .gray.opacity(0.3)).frame(width: index == page ? 16 : 6, height: 6) } }.padding(.bottom, 20)
            GlassPrimaryButton(title: page == 2 ? "进入应用" : "下一步") { if page == 2 { onFinish() } else { page += 1 } }
            Button("已有账号？直接登录", action: onLogin).font(.system(size: 13)).foregroundStyle(GlassPalette.accent).padding(.vertical, 16)
        }.padding(.horizontal, 24).buttonStyle(.plain)
    }
}

/// 账号登录；第三方登录入口保留并提示尚未接入。
struct LoginView: View {
    @ObservedObject var api: DaymarkAPI
    @Binding var path: [DaymarkRoute]
    @State private var account = ""
    @State private var password = ""
    @State private var loading = false
    @State private var error: String?
    @State private var developing = false
    var body: some View {
        GeometryReader { proxy in
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    BrandTile(symbol: "heart.fill")
                    Text("欢迎回来").font(.system(size: 30, weight: .semibold)).padding(.top, 16)
                    Text("登录你的纪念日，把值得记住的每一天都存下来。").font(.system(size: 15)).foregroundStyle(.secondary).padding(.top, 8)
                    VStack(spacing: 14) {
                        GlassInput(placeholder: "账号", text: $account)
                        GlassInput(placeholder: "请输入密码", text: $password, secure: true)
                        HStack {
                            Button { developing = true } label: { Label("记住我", systemImage: "square") }.foregroundStyle(.secondary)
                            Spacer()
                            Button("忘记密码？") { path.append(.reset) }.foregroundStyle(GlassPalette.accent)
                        }.font(.system(size: 13)).buttonStyle(.plain)
                        if let error { Text(error).font(.system(size: 12)).foregroundStyle(.red).frame(maxWidth: .infinity, alignment: .leading) }
                        GlassPrimaryButton(title: loading ? "正在登录…" : "登 录", disabled: loading || account.isEmpty || password.isEmpty) { Task { await login() } }
                    }.padding(20).modifier(GlassSurface(radius: 28)).padding(.top, 32)
                    SocialOptions(label: "或使用以下方式继续") { developing = true }.padding(.top, 26)
                    Spacer(minLength: 40)
                    HStack(spacing: 6) {
                        Text("还没有账号？").foregroundStyle(.secondary)
                        Button("立即注册") { path.append(.register) }.foregroundStyle(GlassPalette.accent)
                        Button("设置") { path.append(.settings) }.foregroundStyle(.secondary).padding(.leading, 8)
                    }.font(.system(size: 13)).frame(maxWidth: .infinity).padding(.bottom, 16)
                }.padding(.horizontal, 24).padding(.top, 16).frame(minHeight: proxy.size.height, alignment: .top)
            }.scrollIndicators(.hidden).scrollDismissesKeyboard(.interactively)
        }
        // 键盘上提供明确的收起入口，同时支持下滑收起。
        .toolbar { ToolbarItemGroup(placement: .keyboard) { Spacer(); Button("完成") { UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil) } } }
        .modifier(DevelopmentNotice(presented: $developing))
        .alert("密码修改成功，请重新登录。", isPresented: $api.passwordChangeSucceeded) { Button("好", role: .cancel) {} }
    }
    private func login() async {
        loading = true; defer { loading = false }
        do { try await api.login(account: account.trimmingCharacters(in: .whitespacesAndNewlines), password: password); password = "" }
        catch { self.error = error.localizedDescription }
    }
}

/// 已登录用户通过原密码修改密码，成功后服务端会注销所有设备。
struct ChangePasswordView: View {
    @ObservedObject var api: DaymarkAPI
    @Environment(\.dismiss) private var dismiss
    @State private var oldPassword = ""
    @State private var newPassword = ""
    @State private var confirmation = ""
    @State private var loading = false
    @State private var error: String?
    private let minimumPasswordLength = 8

    var body: some View {
        VStack(spacing: 0) {
            GlassHeader(title: "修改密码", back: { dismiss() })
            ScrollView {
                VStack(alignment: .leading, spacing: 14) {
                    Text("修改密码后，所有设备都需要重新登录。").font(.system(size: 13)).foregroundStyle(.secondary)
                    GlassInput(placeholder: "原密码", text: $oldPassword, secure: true)
                    GlassInput(placeholder: "设置新密码（至少 8 位）", text: $newPassword, secure: true)
                    GlassInput(placeholder: "确认新密码", text: $confirmation, secure: true)
                    if !confirmation.isEmpty && confirmation != newPassword { Text("两次输入的密码不一致").font(.system(size: 12)).foregroundStyle(.red) }
                    if let error { Text(error).font(.system(size: 12)).foregroundStyle(.red) }
                    GlassPrimaryButton(title: loading ? "请稍候…" : "确认修改", disabled: loading || oldPassword.isEmpty || newPassword.count < minimumPasswordLength || confirmation != newPassword) { Task { await submit() } }
                }.padding(20).modifier(GlassSurface(radius: 28)).padding(24)
            }.scrollIndicators(.hidden).scrollDismissesKeyboard(.interactively)
        }
        .toolbar { ToolbarItemGroup(placement: .keyboard) { Spacer(); Button("完成") { UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil) } } }
    }

    /// 仅提交有效表单；请求失败时保留输入，允许用户修正后重试。
    private func submit() async {
        guard !loading, !oldPassword.isEmpty, newPassword.count >= minimumPasswordLength, confirmation == newPassword else { return }
        loading = true; error = nil; defer { loading = false }
        do { try await api.changePassword(oldPassword: oldPassword, newPassword: newPassword) }
        catch { self.error = error.localizedDescription }
    }
}

/// 注册和找回密码共用验证码页面；发送和提交均经过服务端校验。
struct AccountView: View {
    @ObservedObject var api: DaymarkAPI
    let isRegister: Bool
    @Binding var path: [DaymarkRoute]
    @Environment(\.dismiss) private var dismiss
    @State private var account = ""
    @State private var email = ""
    @State private var password = ""
    @State private var code = ""
    @State private var agreed = false
    @State private var verification = false
    @State private var done = false
    @State private var loading = false
    @State private var error: String?
    @State private var sentAt: Date?
    @State private var developing = false
    private let legalURL = URL(string: "https://blog.creepersan.com/article/61")!
    var body: some View {
        VStack(spacing: 0) {
            GlassHeader(title: "", back: { if verification && !done { verification = false } else { dismiss() } })
            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    if !verification && !done { BrandTile(symbol: isRegister ? "heart.fill" : "lock.fill").padding(.bottom, 16) }
                    Text(LocalizedStringKey(done ? "操作完成" : verification ? "输入验证码" : isRegister ? "创建账号" : "找回密码")).font(.system(size: verification ? 26 : 30, weight: .semibold))
                    Group {
                        if verification && !done { Text("验证码已发送至 \(email)，5 分钟内有效。") }
                        else { Text(LocalizedStringKey(done ? "请返回登录页面，使用账号和密码登录。" : isRegister ? "注册后即可开始记录你的纪念日。" : "输入注册时使用的邮箱，我们会把验证码发给你。")) }
                    }.font(.system(size: 15)).foregroundStyle(.secondary).padding(.top, 8)
                    if done { GlassPrimaryButton(title: "返回登录", action: { path.removeAll() }).padding(.top, 30) }
                    else if verification {
                        codeInput.padding(.top, 30)
                        if !isRegister { GlassInput(placeholder: "设置新密码（至少 8 位）", text: $password, secure: true).padding(.top, 14) }
                        TimelineView(.periodic(from: .now, by: 1)) { context in
                            let remaining = max(0, Int(ceil(60 - context.date.timeIntervalSince(sentAt ?? .distantPast))))
                            Button(remaining > 0 ? "没有收到？\(remaining) 秒后可重新发送" : "重新发送验证码") { Task { await sendCode() } }
                                .disabled(remaining > 0 || loading).font(.system(size: 12)).foregroundStyle(GlassPalette.accent).padding(.top, 16)
                        }
                    } else {
                        VStack(spacing: 14) {
                            if isRegister { GlassInput(placeholder: "账号", text: $account) }
                            GlassInput(placeholder: "邮箱", text: $email).keyboardType(.emailAddress)
                            if isRegister {
                                GlassInput(placeholder: "设置密码（至少 8 位）", text: $password, secure: true)
                                HStack(alignment: .top, spacing: 8) {
                                    Button { agreed.toggle() } label: { Image(systemName: agreed ? "checkmark.square.fill" : "square") }.foregroundStyle(GlassPalette.accent)
                                    Text("我已阅读并同意 ") + Text("《用户协议》与《隐私政策》").foregroundColor(GlassPalette.accent)
                                }.font(.system(size: 12)).frame(maxWidth: .infinity, alignment: .leading)
                                Link("查看用户协议与隐私政策", destination: legalURL).font(.system(size: 12)).foregroundStyle(GlassPalette.accent).frame(maxWidth: .infinity, alignment: .leading)
                            } else { Text("验证码 5 分钟内有效，请注意查收邮件。").font(.system(size: 12)).foregroundStyle(.secondary).frame(maxWidth: .infinity, alignment: .leading) }
                            GlassPrimaryButton(title: loading ? "请稍候…" : "发送验证码", disabled: loading || email.isEmpty || (isRegister && (account.isEmpty || password.count < 8 || !agreed))) { Task { await sendCode() } }
                        }.padding(20).modifier(GlassSurface(radius: 28)).padding(.top, 30)
                        if isRegister { SocialOptions(label: "或使用以下方式注册") { developing = true }.padding(.top, 22) }
                        else { Text("还没收到验证码？请检查垃圾邮件，或稍后重新发送。").font(.system(size: 12)).foregroundStyle(.secondary).padding(.top, 20) }
                    }
                    if let error { Text(error).font(.system(size: 12)).foregroundStyle(.red).padding(.top, 12) }
                }.padding(.horizontal, 24).padding(.top, 16).padding(.bottom, 24)
            }.scrollIndicators(.hidden).scrollDismissesKeyboard(.interactively)
            if verification && !done {
                GlassPrimaryButton(title: loading ? "请稍候…" : "确认并继续", disabled: loading || code.count != 6 || password.count < 8) { Task { await submit() } }.padding(.horizontal, 24).padding(.bottom, 16)
            } else if !done {
                Button(LocalizedStringKey(isRegister ? "已有账号？去登录" : "还没有账号？立即注册")) {
                    if isRegister { path.removeAll() } else { path.append(.register) }
                }.font(.system(size: 13)).foregroundStyle(GlassPalette.accent).padding(.bottom, 16)
            }
        }.toolbar(.hidden, for: .navigationBar)
            .toolbar { ToolbarItemGroup(placement: .keyboard) { Spacer(); Button("完成") { UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil) } } }
            .modifier(DevelopmentNotice(presented: $developing)
        )
    }
    private var codeInput: some View {
        ZStack {
            HStack(spacing: 8) { ForEach(0..<6) { index in Text(index < code.count ? String(Array(code)[index]) : "").font(.system(size: 22)).frame(maxWidth: .infinity).frame(height: 58).modifier(GlassSurface(radius: 14)) } }
            TextField("验证码", text: $code).keyboardType(.numberPad).textContentType(.oneTimeCode).opacity(0.02).onChange(of: code) { _, value in code = String(value.filter(\.isNumber).prefix(6)) }
        }
    }
    private func sendCode() async {
        loading = true; error = nil; defer { loading = false }
        do { try await api.requestCode(email: email.trimmingCharacters(in: .whitespacesAndNewlines), register: isRegister); sentAt = Date(); verification = true }
        catch { self.error = error.localizedDescription }
    }
    private func submit() async {
        loading = true; error = nil; defer { loading = false }
        do {
            if isRegister { try await api.register(account: account.trimmingCharacters(in: .whitespacesAndNewlines), email: email.trimmingCharacters(in: .whitespacesAndNewlines), password: password, code: code) }
            else { try await api.resetPassword(email: email.trimmingCharacters(in: .whitespacesAndNewlines), password: password, code: code) }
            password = ""; code = ""; done = true
        } catch { self.error = error.localizedDescription }
    }
}

struct BrandTile: View {
    let symbol: String
    var body: some View { Image(systemName: symbol).font(.system(size: 24, weight: .semibold)).foregroundStyle(GlassPalette.accent).frame(width: 60, height: 60).modifier(GlassSurface(radius: 20)) }
}
struct SocialOptions: View {
    let label: String
    let action: () -> Void
    var body: some View {
        VStack(spacing: 16) {
            HStack { Rectangle().frame(height: 1); Text(LocalizedStringKey(label)).fixedSize(); Rectangle().frame(height: 1) }.font(.system(size: 12)).foregroundStyle(.secondary.opacity(0.65))
            HStack(spacing: 16) {
                Button(action: action) { Image(systemName: "apple.logo").frame(width: 56, height: 56).modifier(GlassSurface(radius: 28)) }
                Button(action: action) { Image(systemName: "bubble.left.and.bubble.right.fill").foregroundStyle(.green).frame(width: 56, height: 56).modifier(GlassSurface(radius: 28)) }
            }.buttonStyle(.plain)
        }
    }
}
