import SwiftUI

/// 应用导航入口；iOS 仅提供新版，不包含经典模式。
enum DaymarkRoute: Hashable { case register, reset, changePassword, settings, about, edit(Int64?) }

@main
struct DaymarkApp: App {
    @Environment(\.scenePhase) private var scenePhase
    @StateObject private var api = DaymarkAPI()
    @AppStorage("onboardingComplete") private var onboardingComplete = false
    @AppStorage("theme") private var theme = "system"
    @AppStorage("language") private var language = "system"
    @State private var path: [DaymarkRoute] = []

    var body: some Scene {
        WindowGroup {
            ZStack {
                GlassBackground()
                NavigationStack(path: $path) {
                    Group {
                        if !onboardingComplete { OnboardingView(onFinish: { onboardingComplete = true }, onLogin: { onboardingComplete = true }) }
                        else if api.session != nil { HomeView(api: api, path: $path) }
                        else { LoginView(api: api, path: $path) }
                    }.modifier(GlassScreenBackground())
                    .navigationDestination(for: DaymarkRoute.self) { route in
                        Group {
                            switch route {
                            case .register: AccountView(api: api, isRegister: true, path: $path).modifier(GlassScreenBackground())
                            case .reset: AccountView(api: api, isRegister: false, path: $path).modifier(GlassScreenBackground())
                            case .changePassword: ChangePasswordView(api: api).modifier(GlassScreenBackground())
                            case .settings: SettingsView(api: api, path: $path).modifier(GlassScreenBackground())
                            case .about: AboutView().modifier(GlassScreenBackground())
                            case .edit(let id): AnniversaryEditView(api: api, id: id).modifier(GlassScreenBackground())
                            }
                        }.toolbar(.hidden, for: .navigationBar)
                    }.toolbar(.hidden, for: .navigationBar)
                }.background(.clear).scrollContentBackground(.hidden)
            }
            .overlay {
                if let message = api.toastMessage {
                    Text(verbatim: message).font(.system(size: 16)).foregroundStyle(Color(red: 93 / 255, green: 67 / 255, blue: 60 / 255)).padding(.horizontal, 28).padding(.vertical, 18).background(Color(red: 244 / 255, green: 231 / 255, blue: 222 / 255), in: Capsule()).frame(maxWidth: .infinity, maxHeight: .infinity).ignoresSafeArea().allowsHitTesting(false)
                }
            }
            .task(id: api.toastMessage) {
                guard api.toastMessage != nil else { return }
                do { try await Task.sleep(for: .seconds(2.5)); api.toastMessage = nil } catch { }
            }
            .preferredColorScheme(theme == "light" ? .light : theme == "dark" ? .dark : nil)
            .environment(\.locale, language == "system" ? .current : Locale(identifier: language))
            .onChange(of: api.session?.id) { _, _ in path.removeAll() }
            .onAppear { DaymarkLog.event("应用界面显示") }
            .onChange(of: scenePhase) { _, phase in DaymarkLog.event("应用状态变化：\(String(describing: phase))") }
            .onChange(of: path) { _, newPath in DaymarkLog.event("导航变化：页面层数 \(newPath.count)") }
        }
    }
}
