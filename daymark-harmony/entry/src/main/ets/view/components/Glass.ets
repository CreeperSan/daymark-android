import { Theme, Metrics } from '../Theme';
import { I18n } from '../../core/i18n/Strings';
import { Icon } from './Icon';

/**
 * 页面背景：暖色纵向渐变 + 三团缓慢漂移的柔光。
 * iOS 用 TimelineView 驱动，这里用循环动画改变位移量，效果一致且不占渲染线程。
 */
@Component
export struct GlassBackground {
  @Prop isDark: boolean = false;
  @State drift: number = 0;

  aboutToAppear(): void {
    animateTo({
      duration: 18000,
      curve: Curve.EaseInOut,
      iterations: -1,
      playMode: PlayMode.Alternate
    }, () => {
      this.drift = 1;
    });
  }

  /** 把 rgba(...) 的透明度改为 0，作为光斑径向渐变的边缘色。 */
  private transparentOf(color: string): string {
    const index: number = color.lastIndexOf(',');
    return index > 0 ? color.substring(0, index) + ', 0)' : color;
  }

  @Builder
  glow(color: string, size: number, left: string, top: string, shift: number) {
    Column()
      .width(size)
      .height(size)
      .borderRadius(size / 2)
      .position({ x: left, y: top })
      .translate({ x: (this.drift - 0.5) * shift, y: (this.drift - 0.5) * shift * 0.7 })
      .radialGradient({
        center: [size / 2, size / 2],
        radius: size / 2,
        colors: [[color, 0.0], [this.transparentOf(color), 1.0]]
      })
  }

  build() {
    Stack() {
      Column()
        .width('100%')
        .height('100%')
        .linearGradient({
          angle: 180,
          colors: [
            [Theme.backgroundColors(this.isDark)[0], 0.0],
            [Theme.backgroundColors(this.isDark)[1], 0.55],
            [Theme.backgroundColors(this.isDark)[2], 1.0]
          ]
        })
      this.glow(Theme.glowOrange(this.isDark), 380, '-12%', '-4%', 26)
      this.glow(Theme.glowPurple(this.isDark), 340, '46%', '26%', -22)
      this.glow(Theme.glowYellow(this.isDark), 360, '-14%', '70%', 20)
    }
    .width('100%')
    .height('100%')
    .clip(true)
  }
}

/** 玻璃卡片。solid 用于模态弹层，避免弹层内透出下层文字。 */
@Component
export struct GlassCard {
  @Prop isDark: boolean = false;
  @Prop radius: number = Metrics.CARD_RADIUS;
  @Prop padH: number = 20;
  @Prop padV: number = 20;
  @Prop solid: boolean = false;
  @Prop marginTop: number = 0;
  @BuilderParam content: () => void = this.emptyContent;

  @Builder
  emptyContent() {
  }

  build() {
    Column() {
      this.content()
    }
    .width('100%')
    .alignItems(HorizontalAlign.Start)
    .padding({ left: this.padH, right: this.padH, top: this.padV, bottom: this.padV })
    .backgroundColor(this.solid ? Theme.glassSolid(this.isDark) : Theme.glassFill(this.isDark))
    .borderRadius(this.radius)
    .border({ width: 1, color: Theme.glassStroke(this.isDark) })
    .backgroundBlurStyle(this.solid ? BlurStyle.NONE : BlurStyle.Thin)
    .margin({ top: this.marginTop })
  }
}

/** 固定标题栏。返回按钮为圆形玻璃按钮，右侧可放图标或文字操作。 */
@Component
export struct GlassHeader {
  @Prop title: string = '';
  @Prop showBack: boolean = false;
  @Prop trailingText: string = '';
  @Prop trailingIcon: string = '';
  @Prop trailingEnabled: boolean = true;
  @Prop isDark: boolean = false;
  onBack: () => void = () => {
  };
  onTrailing: () => void = () => {
  };
  onTitleTap: () => void = () => {
  };

  build() {
    Row({ space: 12 }) {
      if (this.showBack) {
        Button({ type: ButtonType.Circle, stateEffect: true }) {
          Icon({ name: 'back', glyphSize: 17, color: Theme.textPrimary(this.isDark) })
        }
        .width(Metrics.ROUND_BUTTON)
        .height(Metrics.ROUND_BUTTON)
        .backgroundColor(Theme.glassFill(this.isDark))
        .border({ width: 1, color: Theme.glassStroke(this.isDark) })
        .onClick(() => {
          this.onBack();
        })
      }
      Text(this.title)
        .fontSize(17)
        .fontWeight(FontWeight.Bold)
        .fontColor(Theme.textPrimary(this.isDark))
        .layoutWeight(1)
        .maxLines(1)
        .textOverflow({ overflow: TextOverflow.Ellipsis })
        .onClick(() => {
          this.onTitleTap();
        })
      if (this.trailingIcon.length > 0) {
        Button({ type: ButtonType.Circle, stateEffect: true }) {
          Icon({ name: this.trailingIcon, glyphSize: 18, color: Theme.accent(this.isDark) })
        }
        .width(Metrics.ROUND_BUTTON)
        .height(Metrics.ROUND_BUTTON)
        .backgroundColor(Theme.glassFill(this.isDark))
        .border({ width: 1, color: Theme.glassStroke(this.isDark) })
        .enabled(this.trailingEnabled)
        .onClick(() => {
          this.onTrailing();
        })
      }
      if (this.trailingText.length > 0) {
        Text(this.trailingText)
          .fontSize(14)
          .fontWeight(FontWeight.Medium)
          .fontColor(this.trailingEnabled ? Theme.accent(this.isDark) : Theme.textSecondary(this.isDark))
          .padding({ left: 8, right: 4, top: 6, bottom: 6 })
          .onClick(() => {
            if (this.trailingEnabled) {
              this.onTrailing();
            }
          })
      }
    }
    .width('100%')
    .height(Metrics.HEADER_HEIGHT)
    .padding({ left: Metrics.PAGE_PADDING_WIDE, right: Metrics.PAGE_PADDING_WIDE })
  }
}

/** 玻璃输入框，密码可切换可见性。 */
@Component
export struct GlassInput {
  @Prop placeholder: string = '';
  @Prop text: string = '';
  @Prop secure: boolean = false;
  @Prop isDark: boolean = false;
  @Prop inputType: InputType = InputType.Normal;
  @Prop marginTop: number = 0;
  @State visible: boolean = false;
  onTextChange: (value: string) => void = () => {
  };

  build() {
    Row({ space: 8 }) {
      TextInput({ text: this.text, placeholder: this.placeholder })
        .type(this.secure && !this.visible ? InputType.Password : this.inputType)
        // 关闭系统自带的密码可见性图标，避免与右侧自绘的图标重复
        .showPasswordIcon(false)
        .placeholderColor(Theme.textSecondary(this.isDark))
        .fontColor(Theme.textPrimary(this.isDark))
        .placeholderFont({ size: 15 })
        .fontSize(15)
        .backgroundColor('#00000000')
        .padding(0)
        .height(Metrics.INPUT_HEIGHT - 6)
        .layoutWeight(1)
        .caretColor(Theme.accent(this.isDark))
        .enterKeyType(EnterKeyType.Done)
        .onChange((value: string) => {
          this.onTextChange(value);
        })
      if (this.secure) {
        Button({ type: ButtonType.Circle }) {
          Icon({
            name: this.visible ? 'eye-off' : 'eye',
            glyphSize: 18,
            color: Theme.textSecondary(this.isDark),
          })
        }
        .width(32)
        .height(32)
        .backgroundColor('#00000000')
        .onClick(() => {
          this.visible = !this.visible;
        })
      }
    }
    .width('100%')
    .height(Metrics.INPUT_HEIGHT)
    .padding({ left: 16, right: 8 })
    .backgroundColor(Theme.inputFill(this.isDark))
    .borderRadius(Metrics.INPUT_RADIUS)
    .border({ width: 1, color: Theme.inputStroke(this.isDark) })
    .margin({ top: this.marginTop })
  }
}

/** 勾选框：默认描边方块，选中后填充强调色并显示对勾。 */
@Component
export struct GlassCheck {
  @Prop checked: boolean = false;
  @Prop isDark: boolean = false;
  @Prop label: string = '';
  @Prop boxSize: number = 20;
  onToggle: () => void = () => {
  };

  build() {
    Row({ space: 8 }) {
      Stack() {
        if (this.checked) {
          Icon({ name: 'check', glyphSize: 13, color: '#FFFFFF' })
        }
      }
      .width(this.boxSize)
      .height(this.boxSize)
      .borderRadius(this.boxSize * 0.3)
      .backgroundColor(this.checked ? Theme.accent(this.isDark) : '#00000000')
      .border({
        width: 1.4,
        color: this.checked ? Theme.accent(this.isDark) : Theme.textSecondary(this.isDark)
      })
      .opacity(this.checked ? 1 : 0.75)

      if (this.label.length > 0) {
        Text(this.label)
          .fontSize(13)
          .fontColor(Theme.textSecondary(this.isDark))
      }
    }
    .alignItems(VerticalAlign.Center)
    .onClick(() => {
      this.onToggle();
    })
  }
}


/** 通栏主按钮，暖橙渐变。禁用时切换为低饱和暖色，避免整块透明得像坏掉了。 */
@Component
export struct GlassPrimaryButton {
  @Prop title: string = '';
  @Prop active: boolean = true;
  @Prop isDark: boolean = false;
  @Prop marginTop: number = 0;
  onTap: () => void = () => {
  };

  build() {
    // 注意：不要用 enabled(false)，系统会把整个按钮调暗，导致禁用色和主题色都失真；
    // 这里通过 stateEffect 关掉点击反馈，并在 onClick 里拦截点击。
    Button({ type: ButtonType.Capsule, stateEffect: this.active }) {
      Text(this.title)
        .fontSize(16)
        .fontWeight(FontWeight.Medium)
        .fontColor('#FFFFFF')
    }
    .width('100%')
    .height(Metrics.BUTTON_HEIGHT)
    .margin({ top: this.marginTop })
    .linearGradient({
      angle: 90,
      colors: this.active
        ? [[Theme.accentStart, 0.0], [Theme.accentEnd, 1.0]]
        : [[Theme.accentMutedStart(this.isDark), 0.0], [Theme.accentMutedEnd(this.isDark), 1.0]]
    })
    .onClick(() => {
      if (this.active) {
        this.onTap();
      }
    })
  }
}


/** 单选卡片：重复模式、闰月开关等。 */
@Component
export struct GlassChoice {
  @Prop title: string = '';
  @Prop selected: boolean = false;
  @Prop isDark: boolean = false;
  onTap: () => void = () => {
  };

  build() {
    Column() {
      Text(this.title)
        .fontSize(13)
        .fontWeight(this.selected ? FontWeight.Medium : FontWeight.Normal)
        .fontColor(this.selected ? Theme.accent(this.isDark) : Theme.textSecondary(this.isDark))
        .maxLines(1)
        .textOverflow({ overflow: TextOverflow.Ellipsis })
    }
    .justifyContent(FlexAlign.Center)
    .height(48)
    .layoutWeight(1)
    .backgroundColor(this.selected ? Theme.glassFill(this.isDark) : Theme.glassFillWeak(this.isDark))
    .borderRadius(16)
    .border({ width: 1, color: this.selected ? Theme.accent(this.isDark) : Theme.glassStroke(this.isDark) })
    .onClick(() => {
      this.onTap();
    })
  }
}

/** 分段控件（公历 / 农历）。 */
@Component
export struct GlassSegments {
  @Prop values: string[] = [];
  @Prop labels: string[] = [];
  @Prop selected: string = '';
  @Prop isDark: boolean = false;
  onSelect: (value: string) => void = () => {
  };

  build() {
    Row({ space: 4 }) {
      ForEach(this.values, (value: string, index: number) => {
        Text(this.labels[index])
          .fontSize(13)
          .fontWeight(this.selected === value ? FontWeight.Medium : FontWeight.Normal)
          .fontColor(this.selected === value ? Theme.accent(this.isDark) : Theme.textSecondary(this.isDark))
          .textAlign(TextAlign.Center)
          .height(36)
          .layoutWeight(1)
          .borderRadius(11)
          .backgroundColor(this.selected === value ? Theme.segmentThumb(this.isDark) : '#00000000')
          .onClick(() => {
            this.onSelect(value);
          })
      }, (value: string) => value)
    }
    .width('100%')
    .height(44)
    .padding(4)
    .backgroundColor(Theme.segmentTrack(this.isDark))
    .borderRadius(14)
    .border({ width: 1, color: Theme.glassStroke(this.isDark) })
  }
}

/** 页面级空数据 / 断网 / 失败状态，不使用卡片背景。 */
@Component
export struct PageState {
  @Prop kind: string = 'empty';
  @Prop isDark: boolean = false;
  @Prop title: string = '';
  @Prop description: string = '';
  onRetry: () => void = () => {
  };

  build() {
    Column({ space: 10 }) {
      if (this.kind === 'offline') {
        Icon({ name: 'wifi-off', glyphSize: 100, color: Theme.pageIcon })
      } else if (this.kind === 'failed') {
        Icon({ name: 'info', glyphSize: 100, color: Theme.pageIcon })
      } else {
        Icon({ name: 'calendar', glyphSize: 100, color: Theme.pageIcon })
      }
      Text(this.title)
        .fontSize(18)
        .fontWeight(FontWeight.Medium)
        .fontColor(Theme.textPrimary(this.isDark))
        .opacity(0.72)
        .textAlign(TextAlign.Center)
      Text(this.description)
        .fontSize(14)
        .lineHeight(20)
        .fontColor(Theme.textSecondary(this.isDark))
        .opacity(0.9)
        .textAlign(TextAlign.Center)
      if (this.kind !== 'empty') {
        Text(I18n.t('retry'))
          .fontSize(15)
          .fontColor(Theme.accent(this.isDark))
          .padding(8)
          .onClick(() => {
            this.onRetry();
          })
      }
    }
    .width('100%')
    .padding({ left: 24, right: 24 })
    .alignItems(HorizontalAlign.Center)
  }
}
