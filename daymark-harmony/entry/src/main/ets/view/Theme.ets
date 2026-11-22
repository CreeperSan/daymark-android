/**
 * 设计令牌。与 iOS 的 GlassPalette / 设计规范保持一致：
 * 暖色渐变背景、半透明玻璃卡片、暖橙强调色。
 */
export class Theme {
  static readonly accentLight: string = '#FF6A45';
  static readonly accentDark: string = '#FF7A55';
  static readonly accentStart: string = '#FF9459';
  static readonly accentEnd: string = '#FF6B52';
  static readonly primaryLight: string = '#4A2511';
  static readonly secondaryLight: string = '#A5795C';
  static readonly primaryDark: string = '#F7EBE3';
  static readonly secondaryDark: string = '#D5B4A4';
  static readonly dangerLight: string = '#D84A4A';
  static readonly dangerDark: string = '#FF6B6B';
  static readonly pageIcon: string = '#D4B6A1';
  static readonly pageAccent: string = '#E7A093';

  static accent(dark: boolean): string {
    return dark ? Theme.accentDark : Theme.accentLight;
  }

  static textPrimary(dark: boolean): string {
    return dark ? Theme.primaryDark : Theme.primaryLight;
  }

  static textSecondary(dark: boolean): string {
    return dark ? Theme.secondaryDark : Theme.secondaryLight;
  }

  static danger(dark: boolean): string {
    return dark ? Theme.dangerDark : Theme.dangerLight;
  }

  static glassFill(dark: boolean): string {
    return dark ? '#1AFFFFFF' : '#8CFFFFFF';
  }

  static glassFillWeak(dark: boolean): string {
    return dark ? '#12FFFFFF' : '#52FFFFFF';
  }

  static glassStroke(dark: boolean): string {
    return dark ? '#2EFFFFFF' : '#A6FFFFFF';
  }

  /** 输入框底色：比卡片更亮一档，保证在玻璃卡片上能看出独立的输入区域。 */
  static inputFill(dark: boolean): string {
    return dark ? '#26FFFFFF' : '#D9FFFFFF';
  }

  static inputStroke(dark: boolean): string {
    return dark ? '#33FFFFFF' : '#F2FFFFFF';
  }

  /** 主按钮禁用态：保持暖橙但整体压暗，既明显不可点又不会糊成一片白。 */
  static accentMutedStart(dark: boolean): string {
    return dark ? '#8A5A46' : '#FFA98F';
  }

  static accentMutedEnd(dark: boolean): string {
    return dark ? '#7D4F3D' : '#FF8F73';
  }

  static glassSolid(dark: boolean): string {
    return dark ? '#F2322A33' : '#F7FFF8F3';
  }

  static segmentTrack(dark: boolean): string {
    return dark ? '#14FFFFFF' : '#59FFFFFF';
  }

  static segmentThumb(dark: boolean): string {
    return dark ? '#2EFFFFFF' : '#D9FFFFFF';
  }

  /** 页面主背景渐变。 */
  static backgroundColors(dark: boolean): string[] {
    return dark ? ['#1F1C29', '#38262E', '#2A1F26'] : ['#FFF2E3', '#FFDEC7', '#FFEDDE'];
  }

  static glowOrange(dark: boolean): string {
    return dark ? 'rgba(255, 154, 92, 0.20)' : 'rgba(255, 148, 89, 0.38)';
  }

  static glowPurple(dark: boolean): string {
    return dark ? 'rgba(192, 140, 224, 0.16)' : 'rgba(192, 140, 224, 0.24)';
  }

  static glowYellow(dark: boolean): string {
    return dark ? 'rgba(255, 196, 107, 0.14)' : 'rgba(255, 196, 107, 0.25)';
  }

  /** 给 #RRGGBB 追加透明度，返回 #AARRGGBB。 */
  static withAlpha(color: string, alpha: number): string {
    let value: number = Math.round(alpha * 255);
    if (value < 0) {
      value = 0;
    }
    if (value > 255) {
      value = 255;
    }
    const hex: string = value.toString(16).toUpperCase();
    return '#' + (hex.length === 1 ? '0' + hex : hex) + color.substring(1);
  }
}

/** 布局尺寸，单位 vp / fp。 */
export class Metrics {
  static readonly PAGE_PADDING: number = 24;
  static readonly PAGE_PADDING_WIDE: number = 20;
  static readonly HEADER_HEIGHT: number = 52;
  static readonly ROUND_BUTTON: number = 36;
  static readonly CARD_RADIUS: number = 20;
  static readonly SHEET_RADIUS: number = 24;
  static readonly INPUT_RADIUS: number = 16;
  static readonly INPUT_HEIGHT: number = 54;
  static readonly BUTTON_HEIGHT: number = 54;
  static readonly BASE_PADDING: number = 20;
}
