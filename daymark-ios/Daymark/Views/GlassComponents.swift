import SwiftUI

/// 设计稿使用的暖色与暗色文字层级。
enum GlassPalette {
    static let accent = Color(red: 1, green: 106 / 255, blue: 69 / 255)
    static let primaryLight = Color(red: 74 / 255, green: 37 / 255, blue: 17 / 255)
    static let secondaryLight = Color(red: 165 / 255, green: 121 / 255, blue: 92 / 255)
    static let gradient = [Color(red: 1, green: 148 / 255, blue: 89 / 255), Color(red: 1, green: 107 / 255, blue: 82 / 255)]
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

/// 暖色渐变主按钮，不使用默认蓝色按钮样式。
struct GlassPrimaryButton: View {
    let title: String
    var disabled = false
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            Text(LocalizedStringKey(title)).font(.system(size: 16, weight: .semibold)).foregroundStyle(disabled ? Color.secondary : Color.white)
                .frame(maxWidth: .infinity).frame(height: 54)
                .background(LinearGradient(colors: GlassPalette.gradient.map { $0.opacity(disabled ? 0.4 : 1) }, startPoint: .leading, endPoint: .trailing), in: Capsule())
        }.buttonStyle(.plain).disabled(disabled)
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
                Button(action: back) { Image(systemName: "chevron.left").font(.system(size: 15, weight: .semibold)).frame(width: 36, height: 36).modifier(GlassSurface(radius: 18)) }.buttonStyle(.plain)
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

/// NavigationStack 自带背景会遮挡底层视图，页面背景必须位于导航内容自身。
struct GlassScreenBackground: ViewModifier {
    @Environment(\.colorScheme) private var scheme
    func body(content: Content) -> some View {
        content.background(GlassBackground().ignoresSafeArea())
            .foregroundStyle(scheme == .dark ? Color(red: 0.97, green: 0.92, blue: 0.89) : GlassPalette.primaryLight)
    }
}
