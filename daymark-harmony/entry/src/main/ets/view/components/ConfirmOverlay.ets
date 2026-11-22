import { Theme } from '../Theme';
import { I18n } from '../../core/i18n/Strings';

/**
 * 统一风格的确认弹层。
 *
 * 系统 AlertDialog 的按钮是蓝色强调色，和应用暖色调冲突，因此这里自绘：
 * 全屏半透明遮罩 + 居中玻璃卡片，取消 / 确认并排，危险操作用红色。
 * 使用方式：页面根节点放 `if (this.visible) { ConfirmOverlay({...}) }`。
 */
@Component
export struct ConfirmOverlay {
  @Prop isDark: boolean = false;
  @Prop title: string = '';
  @Prop message: string = '';
  @Prop confirmText: string = '';
  /** 危险操作（删除、退出）用红色确认按钮 */
  @Prop danger: boolean = false;
  onCancel: () => void = () => {
  };
  onConfirm: () => void = () => {
  };

  build() {
    Stack({ alignContent: Alignment.Center }) {
      // 遮罩：点击空白处等同取消
      Column()
        .width('100%')
        .height('100%')
        .backgroundColor(this.isDark ? '#99000000' : '#66000000')
        .onClick(() => {
          this.onCancel();
        })

      Column({ space: 8 }) {
        Text(this.title)
          .fontSize(17)
          .fontWeight(FontWeight.Medium)
          .fontColor(Theme.textPrimary(this.isDark))
          .textAlign(TextAlign.Center)
        Text(this.message)
          .fontSize(14)
          .lineHeight(20)
          .fontColor(Theme.textSecondary(this.isDark))
          .textAlign(TextAlign.Center)
          .margin({ top: 2 })

        Row({ space: 10 }) {
          Button({ type: ButtonType.Capsule, stateEffect: true }) {
            Text(I18n.t('cancel'))
              .fontSize(15)
              .fontWeight(FontWeight.Medium)
              .fontColor(Theme.textPrimary(this.isDark))
          }
          .layoutWeight(1)
          .height(46)
          .backgroundColor(Theme.glassFillWeak(this.isDark))
          .border({ width: 1, color: Theme.glassStroke(this.isDark) })
          .onClick(() => {
            this.onCancel();
          })

          Button({ type: ButtonType.Capsule, stateEffect: true }) {
            Text(this.confirmText.length > 0 ? this.confirmText : I18n.t('confirm'))
              .fontSize(15)
              .fontWeight(FontWeight.Medium)
              .fontColor('#FFFFFF')
          }
          .layoutWeight(1)
          .height(46)
          .backgroundColor(this.danger ? Theme.danger(this.isDark) : Theme.accent(this.isDark))
          .onClick(() => {
            this.onConfirm();
          })
        }
        .width('100%')
        .margin({ top: 16 })
      }
      .width('86%')
      .constraintSize({ maxWidth: 380 })
      .padding({ left: 22, right: 22, top: 22, bottom: 18 })
      .backgroundColor(Theme.glassSolid(this.isDark))
      .borderRadius(24)
      .border({ width: 1, color: Theme.glassStroke(this.isDark) })
      .shadow({ radius: 30, color: '#33000000', offsetY: 10 })
    }
    .width('100%')
    .height('100%')
    .transition(TransitionEffect.OPACITY.animation({ duration: 160 }))
  }
}
