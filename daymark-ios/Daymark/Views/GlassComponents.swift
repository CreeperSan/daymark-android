import SwiftUI

/// 设计稿使用的暖色与暗色文字层级。
enum GlassPalette {
    static let accent = Color(red: 1, green: 106 / 255, blue: 69 / 255)
    static let primaryLight = Color(red: 74 / 255, green: 37 / 255, blue: 17 / 255)
    static let secondaryLight = Color(red: 165 / 255, green: 121 / 255, blue: 92 / 255)
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
