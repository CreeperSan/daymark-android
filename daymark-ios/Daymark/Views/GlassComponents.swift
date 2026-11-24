import SwiftUI
import UIKit

/// 设计稿使用的暖色与暗色文字层级。
enum GlassPalette {
    static let accent = Color(red: 1, green: 106 / 255, blue: 69 / 255)
    static let primaryLight = Color(red: 74 / 255, green: 37 / 255, blue: 17 / 255)
    static let secondaryLight = Color(red: 165 / 255, green: 121 / 255, blue: 92 / 255)
}

/// 对应 Ardot 的两种操作区布局，调用方可按场景选择。
enum ChoiceDialogActionPlacement {
    case footer
    case titleBar
}

/// 弹窗面板的位置独立于操作按钮的位置。
enum ChoiceDialogPlacement {
    case bottom
    case center
}

/// 全屏容器只负责隔离点击和辅助功能焦点，转场由弹窗自行绘制。
func withoutChoiceDialogPresentationAnimation(_ update: () -> Void) {
    var transaction = Transaction(animation: nil)
    transaction.disablesAnimations = true
    withTransaction(transaction, update)
}

/// iPhone 和 iPad 共用选择弹窗，默认使用底部操作区。
struct ChoiceDialog<Content: View>: View {
    let title: LocalizedStringKey
    var placement: ChoiceDialogPlacement = .bottom
    var actionPlacement: ChoiceDialogActionPlacement = .footer
    var confirmTitle: LocalizedStringKey = "确定"
    var destructive = false
    var confirmEnabled = true
    var saveBeforeClosing: (() async -> Bool)? = nil
    let cancel: () -> Void
    let confirm: () -> Void
    @ViewBuilder let content: () -> Content
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var visible = false
    @State private var closing = false
    @State private var saving = false

    private var displayedConfirmTitle: LocalizedStringKey { saving ? "保存中…" : confirmTitle }

    private let outline = Color(red: 1, green: 140 / 255, blue: 66 / 255)
    private let cream = Color(red: 1, green: 246 / 255, blue: 236 / 255)
    private let actionGradient = LinearGradient(colors: [
        Color(red: 1, green: 148 / 255, blue: 89 / 255),
        Color(red: 1, green: 107 / 255, blue: 82 / 255)
    ], startPoint: .leading, endPoint: .trailing)
    private let actionWidth: CGFloat = 132
    private let actionSpacing: CGFloat = 12
    private let outerInset: CGFloat = 16
    private let tabletWidth: CGFloat = 540
    private let hiddenScale: CGFloat = 0.94
    private let dimOpacity = 0.35
    private let dimFadeIn = Animation.easeOut(duration: 0.20)
    private let dimFadeOut = Animation.easeInOut(duration: 0.17)

    /// 底部面板平滑停靠，居中面板在缩放结束前略微回弹。
    private var panelEntrance: Animation {
        if reduceMotion { return dimFadeIn }
        return placement == .bottom ? .smooth(duration: 0.36) : .smooth(duration: 0.30, extraBounce: 0.10)
    }

    /// 关闭时先轻轻起步，再加速离场。
    private var panelExit: Animation {
        if reduceMotion { return dimFadeOut }
        if placement == .center { return .easeIn(duration: 0.20) }
        return .timingCurve(0.40, 0.0, 0.85, 0.45, duration: 0.24)
    }

    var body: some View {
        GeometryReader { geometry in
            let wide = horizontalSizeClass == .regular && geometry.size.width >= tabletWidth + 2 * outerInset
            let inset: CGFloat = wide ? 28 : 20
            let shape = RoundedRectangle(cornerRadius: wide ? 26 : 22)
            let bottom = placement == .bottom
            let bottomInset = bottom ? geometry.safeAreaInsets.bottom : 0
            let slideOffscreen = bottom && !visible && !reduceMotion
            ZStack(alignment: bottom ? .bottom : .center) {
                // 遮罩固定覆盖全屏，仅改变透明度。
                Color.black.opacity(visible ? dimOpacity : 0)
                    .animation(visible ? dimFadeIn : dimFadeOut, value: visible)
                    .ignoresSafeArea().contentShape(Rectangle())
                    .onTapGesture { close(perform: cancel) }.accessibilityHidden(true)
                VStack(alignment: .leading, spacing: 0) {
                    if actionPlacement == .titleBar {
                        titleBar(wide: wide)
                    } else {
                        Text(title).font(.system(size: wide ? 18 : 16, weight: .semibold))
                            .multilineTextAlignment(.center).frame(maxWidth: .infinity)
                            .accessibilityAddTraits(.isHeader)
                            .padding(.horizontal, inset).padding(.top, wide ? 26 : 20).padding(.bottom, wide ? 14 : 12)
                    }
                    // 横屏或分屏高度不足时，只滚动内容，底部操作始终可用。
                    ViewThatFits(in: .vertical) {
                        content().fixedSize(horizontal: false, vertical: true)
                        ScrollView { content() }
                    }.disabled(saving).frame(maxWidth: .infinity).padding(.horizontal, inset)
                        .padding(.bottom, actionPlacement == .titleBar ? (wide ? 26 : 22) : (wide ? 24 : 20))
                    if actionPlacement == .footer {
                        Rectangle().fill(outline.opacity(0.10)).frame(height: 1)
                        HStack(spacing: actionSpacing) {
                            if wide { Spacer(minLength: 0) }
                            action("取消", wide: wide, primary: false)
                            action(displayedConfirmTitle, wide: wide, primary: true)
                        }.padding(.horizontal, inset).padding(.top, wide ? 20 : 16).padding(.bottom, wide ? 26 : 20)
                    }
                }
                .foregroundStyle(colorScheme == .dark ? Color.white : GlassPalette.primaryLight)
                .frame(maxWidth: wide ? tabletWidth : 353)
                // 只延伸背景到屏幕底边，键盘出现时内容和按钮仍留在键盘上方。
                .background {
                    shape.fill(colorScheme == .dark ? Color(uiColor: .secondarySystemBackground) : Color.white)
                        .ignoresSafeArea(.container, edges: bottom ? .bottom : [])
                }
                .shadow(color: outline.opacity(0.10), radius: wide ? 36 : 30, y: wide ? 14 : 10)
                .padding(.horizontal, outerInset)
                .padding(.top, outerInset)
                .padding(.bottom, bottom ? 0 : outerInset)
                .scaleEffect(!bottom && !visible && !reduceMotion ? hiddenScale : 1)
                .opacity((!bottom || reduceMotion) && !visible ? 0 : 1)
                .visualEffect { panel, proxy in
                    panel.offset(y: slideOffscreen ? proxy.size.height + bottomInset : 0)
                }
                .allowsHitTesting(visible && !closing)
                .accessibilityHidden(!visible || closing)
                .accessibilityAction(.escape) { close(perform: cancel) }
            }.frame(maxWidth: .infinity, maxHeight: .infinity, alignment: bottom ? .bottom : .center)
        }.task {
            // 先让透明容器完成布局，再启动面板和遮罩的动画。
            await Task.yield()
            guard !Task.isCancelled, !closing else { return }
            withAnimation(panelEntrance) { visible = true }
        }
    }

    /// 表单保存成功后才离场，提交期间禁止重复操作或关闭弹窗。
    private func requestConfirmation() {
        guard visible, !closing, !saving, confirmEnabled else { return }
        guard let saveBeforeClosing else { close(perform: confirm); return }
        saving = true
        Task {
            let saved = await saveBeforeClosing()
            saving = false
            if saved { close(perform: confirm) }
        }
    }

    /// 等退出动画结束后再关闭容器并提交操作，避免系统转场叠加。
    private func close(perform: @escaping () -> Void) {
        guard visible, !closing, !saving else { return }
        closing = true
        withAnimation(panelExit, completionCriteria: .removed) {
            visible = false
        } completion: {
            withoutChoiceDialogPresentationAnimation(perform)
        }
    }

    /// 标题栏使用轻量文字操作，两侧留出相同宽度以保持标题居中。
    private func titleBar(wide: Bool) -> some View {
        let actionFont = Font.system(size: wide ? 15 : 14)
        let titleActionWidth: CGFloat = 60
        let touchHeight: CGFloat = 44
        let cancelColor = Color(red: 138 / 255, green: 90 / 255, blue: 60 / 255)
        return HStack(spacing: wide ? 10 : 8) {
            Button { close(perform: cancel) } label: {
                Text("取消").font(actionFont).foregroundStyle(colorScheme == .dark ? Color.secondary : cancelColor)
                    .frame(width: titleActionWidth, height: touchHeight, alignment: .leading).contentShape(Rectangle())
            }.buttonStyle(.plain).disabled(saving)
            Text(title).font(.system(size: wide ? 17 : 15, weight: .semibold))
                .multilineTextAlignment(.center).frame(maxWidth: .infinity).accessibilityAddTraits(.isHeader)
            Button(role: destructive ? .destructive : nil, action: requestConfirmation) {
                Text(displayedConfirmTitle).font(actionFont.weight(.semibold)).foregroundStyle(destructive ? Color.red : GlassPalette.accent)
                    .frame(width: titleActionWidth, height: touchHeight, alignment: .trailing).contentShape(Rectangle())
            }.buttonStyle(.plain).disabled(saving || !confirmEnabled)
        }.padding(.horizontal, wide ? 24 : 16).padding(.top, wide ? 12 : 6).padding(.bottom, wide ? 12 : 10)
    }

    /// 主操作固定在右侧，两种按钮共用尺寸规则。
    private func action(_ title: LocalizedStringKey, wide: Bool, primary: Bool) -> some View {
        let shape = RoundedRectangle(cornerRadius: wide ? 16 : 14)
        let disabled = saving || (primary && !confirmEnabled)
        return Button(role: primary && destructive ? .destructive : nil) {
            if primary { requestConfirmation() } else { close(perform: cancel) }
        } label: {
            Text(title).font(.system(size: 15, weight: .semibold))
                .foregroundStyle(primary || colorScheme == .dark ? Color.white : GlassPalette.primaryLight)
                .frame(maxWidth: .infinity, minHeight: wide ? 52 : 48)
                .background {
                    if primary && destructive {
                        shape.fill(Color.red)
                    } else if primary {
                        shape.fill(actionGradient).shadow(color: outline.opacity(0.28), radius: 8, y: 6)
                    } else {
                        shape.fill(colorScheme == .dark ? Color(uiColor: .tertiarySystemBackground) : cream)
                            .overlay { shape.strokeBorder(outline.opacity(0.16)) }
                    }
                }.contentShape(shape)
        }.buttonStyle(.plain).disabled(disabled).opacity(disabled ? 0.55 : 1).frame(width: wide ? actionWidth : nil)
    }
}

/// 两份文档暂时指向同一地址，正式地址可分别替换。
enum LegalLinks {
    static let userAgreement = URL(string: "https://blog.creepersan.com/article/65")!
    static let privacyPolicy = URL(string: "https://blog.creepersan.com/article/64")!
}

/// 危险操作使用明确的红色，并沿用按下时淡化的反馈。
struct WarningButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label.foregroundStyle(.white).frame(maxWidth: .infinity, minHeight: 54)
            .background(Color(red: 216 / 255, green: 74 / 255, blue: 74 / 255), in: Capsule())
            .opacity(configuration.isPressed ? 0.56 : 1)
    }
}

/// iOS 26 起使用系统 Liquid Glass；旧系统使用系统材质回退。
struct GlassSurface: ViewModifier {
    @Environment(\.colorScheme) private var scheme
    var radius: CGFloat = 24
    func body(content: Content) -> some View {
        if #available(iOS 26.0, *) {
            content.glassEffect(.regular, in: RoundedRectangle(cornerRadius: radius))
        } else {
            content.background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: radius))
                .overlay(RoundedRectangle(cornerRadius: radius).stroke(.white.opacity(scheme == .dark ? 0.18 : 0.6), lineWidth: 1))
        }
    }
}

/// 跨页面共享的渐变及缓慢移动光斑，背景不参与导航切换动画。
struct GlassBackground: View {
    @Environment(\.colorScheme) private var scheme
    var body: some View {
        TimelineView(.animation(minimumInterval: 1.0 / 30)) { timeline in
            let t = timeline.date.timeIntervalSinceReferenceDate
            GeometryReader { proxy in
                let dark = scheme == .dark
                ZStack {
                    LinearGradient(colors: dark ? [Color(red: 0.12, green: 0.11, blue: 0.16), Color(red: 0.22, green: 0.15, blue: 0.18)] : [Color(red: 1, green: 0.95, blue: 0.89), Color(red: 1, green: 0.87, blue: 0.78), Color(red: 1, green: 0.93, blue: 0.87)], startPoint: .top, endPoint: .bottom)
                    glow(.orange, size: proxy.size.width * 0.9, x: proxy.size.width * (0.1 + 0.08 * sin(t / 18)), y: proxy.size.height * 0.09, opacity: dark ? 0.20 : 0.38)
                    glow(.purple, size: proxy.size.width * 0.85, x: proxy.size.width * (0.9 + 0.07 * sin(t / 22)), y: proxy.size.height * 0.35, opacity: dark ? 0.16 : 0.24)
                    glow(.yellow, size: proxy.size.width * 0.85, x: proxy.size.width * (0.12 + 0.07 * sin(t / 25)), y: proxy.size.height * 0.86, opacity: dark ? 0.14 : 0.25)
                }.ignoresSafeArea()
            }
        }
    }
    private func glow(_ color: Color, size: CGFloat, x: CGFloat, y: CGFloat, opacity: Double) -> some View {
        Circle().fill(RadialGradient(colors: [color.opacity(opacity), .clear], center: .center, startRadius: 0, endRadius: size / 2)).frame(width: size, height: size).position(x: x, y: y)
    }
}

/// 使用系统按钮样式，旧系统回退到系统边框按钮。
struct SystemGlassButton: ViewModifier {
    var prominent = false
    func body(content: Content) -> some View {
        if #available(iOS 26.0, *) {
            if prominent { content.buttonStyle(.glassProminent) }
            else { content.buttonStyle(.glass) }
        } else {
            if prominent { content.buttonStyle(.borderedProminent) }
            else { content.buttonStyle(.bordered) }
        }
    }
}

/// 主要操作使用系统强调按钮，保留暖色调和通栏尺寸。
struct GlassPrimaryButton: View {
    let title: String
    var disabled = false
    let action: () -> Void
    var body: some View {
        Button {
            DaymarkLog.event("点击按钮：\(title)")
            action()
        } label: {
            Text(LocalizedStringKey(title)).font(.system(size: 16, weight: .semibold))
                .frame(maxWidth: .infinity).frame(minHeight: 36)
        }.modifier(SystemGlassButton(prominent: true)).buttonBorderShape(.capsule).tint(GlassPalette.accent).disabled(disabled)
    }
}

/// 固定导航栏，滚动内容通过系统安全区域延伸至其后方。
struct GlassHeader: View {
    let title: String
    var back: (() -> Void)? = nil
    var trailing: AnyView? = nil
    var onTitleTap: (() -> Void)? = nil
    var body: some View {
        HStack(spacing: 12) {
            if let back {
                Button { DaymarkLog.event("返回页面：\(title)"); back() } label: { Image(systemName: "chevron.left").font(.system(size: 15, weight: .semibold)) }.modifier(SystemGlassButton()).buttonBorderShape(.circle)
            }
            Text(LocalizedStringKey(title)).font(.system(size: 17, weight: .semibold)).onTapGesture { onTitleTap?() }
            Spacer()
            trailing
        }.frame(height: 52).padding(.horizontal, 20)
    }
}

/// 使用系统导航栏放置返回和操作按钮，由系统提供标题栏样式及点击区域。
struct SystemNavigationHeader: ViewModifier {
    let title: String
    let back: () -> Void
    var trailing: AnyView? = nil
    var onTitleTap: (() -> Void)? = nil

    func body(content: Content) -> some View {
        content
            .navigationBarBackButtonHidden(true)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar(.visible, for: .navigationBar)
            // 滚动时不显示系统默认的着色背景和底部分割线。
            .toolbarBackground(.hidden, for: .navigationBar)
            .background(InteractiveBackGesture())
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button { DaymarkLog.event("返回页面：\(title)"); back() } label: { Image(systemName: "chevron.left") }.accessibilityLabel("返回")
                }
                ToolbarItem(placement: .principal) {
                    Text(LocalizedStringKey(title)).font(.system(size: 17, weight: .semibold)).onTapGesture { onTitleTap?() }
                }
                ToolbarItem(placement: .topBarTrailing) { trailing }
            }
    }
}

/// 自定义导航栏返回动作时，恢复系统的交互式边缘返回手势。
private struct InteractiveBackGesture: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> InteractiveBackController { InteractiveBackController() }
    func updateUIViewController(_ controller: InteractiveBackController, context: Context) {}
}

/// 仅在导航栈有上一页且没有转场时允许系统处理返回手势。
private final class InteractiveBackController: UIViewController, UIGestureRecognizerDelegate {
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        navigationController?.interactivePopGestureRecognizer?.delegate = self
    }

    func gestureRecognizerShouldBegin(_ gestureRecognizer: UIGestureRecognizer) -> Bool {
        guard let navigationController else { return false }
        return navigationController.viewControllers.count > 1 && navigationController.transitionCoordinator == nil
    }
}

struct GlassInput: View {
    let placeholder: String
    @Binding var text: String
    var secure = false
    @State private var visible = false
    var body: some View {
        HStack {
            Group { if secure && !visible { SecureField(LocalizedStringKey(placeholder), text: $text) } else { TextField(LocalizedStringKey(placeholder), text: $text) } }
                .textInputAutocapitalization(.never).autocorrectionDisabled()
            if secure { Button { visible.toggle() } label: { Image(systemName: visible ? "eye.slash" : "eye") }.buttonStyle(.plain).accessibilityLabel(visible ? "隐藏密码" : "显示密码") }
        }.font(.system(size: 15)).padding(.horizontal, 16).frame(height: 54).modifier(GlassSurface(radius: 16))
    }
}

/// 与设计稿一致的未完成入口反馈。
struct DevelopmentNotice: ViewModifier {
    @Binding var presented: Bool
    func body(content: Content) -> some View { content.alert("功能开发中", isPresented: $presented) { Button("好", role: .cancel) {} } }
}

/// 页面级空数据、断网和加载失败状态，不使用卡片背景。
enum GlassPageStateKind { case empty, offline, failed }

/// 状态内容随页面宽度居中，图标和文字沿用公共组件设计稿。
struct GlassPageState: View {
    let kind: GlassPageStateKind
    var retry: (() -> Void)? = nil
    @Environment(\.colorScheme) private var scheme
    private let iconColor = Color(red: 212 / 255, green: 182 / 255, blue: 161 / 255)
    private let accentColor = Color(red: 231 / 255, green: 160 / 255, blue: 147 / 255)
    private let darkSecondaryColor = Color(red: 213 / 255, green: 180 / 255, blue: 164 / 255)
    private var title: LocalizedStringKey {
        switch kind { case .empty: return "还没有纪念日"; case .offline: return "网络连接中断"; case .failed: return "加载失败" }
    }
    private var description: LocalizedStringKey {
        switch kind { case .empty: return "创建第一个，开始记录你们的时光。"; case .offline: return "检查网络后点击重试，内容不会丢失。"; case .failed: return "稍后再试，或下拉刷新这一页。" }
    }
    var body: some View {
        VStack(spacing: 10) {
            icon.scaleEffect(1.5).frame(width: 48, height: 48)
            Text(title).font(.system(size: 18, weight: .semibold)).foregroundStyle((scheme == .dark ? Color.white : GlassPalette.primaryLight).opacity(0.7)).multilineTextAlignment(.center)
            Text(description).font(.system(size: 14)).lineSpacing(4).foregroundStyle((scheme == .dark ? darkSecondaryColor : GlassPalette.secondaryLight).opacity(0.9)).multilineTextAlignment(.center)
            if let retry { Button("重试", action: retry).font(.system(size: 15)).foregroundStyle(GlassPalette.accent.opacity(0.8)).padding(8) }
        }.frame(maxWidth: .infinity).padding(.horizontal, 24)
    }
    @ViewBuilder private var icon: some View {
        switch kind {
        case .empty:
            Image(systemName: "calendar").font(.system(size: 23)).foregroundStyle(iconColor)
        case .offline:
            ZStack {
                Image(systemName: "wifi").font(.system(size: 22)).foregroundStyle(iconColor)
                Capsule().fill(accentColor).frame(width: 27, height: 2).rotationEffect(.degrees(48))
            }
        case .failed:
            ZStack {
                Circle().stroke(iconColor, lineWidth: 2).frame(width: 21, height: 21)
                VStack(spacing: 2) { Capsule().fill(accentColor).frame(width: 2, height: 6); Circle().fill(accentColor).frame(width: 3, height: 3) }
            }
        }
    }
}

/// NavigationStack 自带背景会遮挡底层视图，页面背景必须位于导航内容自身。
struct GlassScreenBackground: ViewModifier {
    @Environment(\.colorScheme) private var scheme
    func body(content: Content) -> some View {
        content.background(GlassBackground().ignoresSafeArea())
            .foregroundStyle(scheme == .dark ? Color(red: 0.97, green: 0.92, blue: 0.89) : GlassPalette.primaryLight)
    }
}
