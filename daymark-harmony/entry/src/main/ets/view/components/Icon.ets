/**
 * 图标组件。
 *
 * 实现说明：iOS 用 SF Symbols，鸿蒙侧没有等价符号库，这里用自绘图标。
 * 资源是「白色线条 + 透明背景」的 PNG（由 .tools/gen_icons.py 生成），
 * 配合 ImageRenderMode.Template 按 alpha 作遮罩、fillColor 上色，因此可以随主题换色。
 *
 * 踩过的坑（不要重犯）：
 * 1. Shape + Path 的坐标不会随组件尺寸缩放，只会按固定像素绘制，图标大小会失控；
 * 2. Image.fillColor 只能改 SVG 的 fill，改不了 stroke，描边图标会一直是黑色；
 * 3. Image.colorBlend 会把整个矩形染上颜色，不适合图标。
 */
export class IconRes {
  /** 图标名到 SVG 资源的映射。新增图标时只需在 media 目录放同名 svg 并在此登记。 */
  static of(name: string): Resource {
    switch (name) {
      case 'back':
        return $r('app.media.ic_back');
      case 'chevron':
        return $r('app.media.ic_chevron');
      case 'plus':
        return $r('app.media.ic_plus');
      case 'check':
        return $r('app.media.ic_check');
      case 'eye':
        return $r('app.media.ic_eye');
      case 'eye-off':
        return $r('app.media.ic_eye_off');
      case 'gear':
        return $r('app.media.ic_gear');
      case 'calendar':
        return $r('app.media.ic_calendar');
      case 'wifi':
        return $r('app.media.ic_wifi');
      case 'wifi-off':
        return $r('app.media.ic_wifi_off');
      case 'info':
        return $r('app.media.ic_info');
      case 'square':
        return $r('app.media.ic_square');
      case 'lock':
        return $r('app.media.ic_lock');
      case 'heart':
        return $r('app.media.ic_heart');
      default:
        return $r('app.media.ic_info');
    }
  }
}

@Component
export struct Icon {
  @Prop name: string = '';
  /** 图标绘制边长（vp）。图标是矢量 SVG，线宽随尺寸同比缩放。 */
  @Prop glyphSize: number = 16;
  @Prop color: string = '#4A2511';

  build() {
    Image(IconRes.of(this.name))
      .width(this.glyphSize)
      .height(this.glyphSize)
      .objectFit(ImageFit.Fill)
      // 图标是纯填充的 SVG，fillColor 直接覆盖填充色即可跟随主题
      .fillColor(this.color)
      .draggable(false)
  }
}
