import { Theme } from './Theme';
import { I18n } from '../core/i18n/Strings';
import { GlassPrimaryButton } from './components/Glass';
import { Logger } from '../core/Logger';

/**
 * 首次启动引导。三页内容与 iOS 一致，提醒与共享功能明确标注为规划中。
 * 支持左右滑动，底部按钮与页码联动。
 */
@Component
export struct OnboardingView {
  @Prop isDark: boolean = false;
  /** 语言标签：只用于触发页面重建，语言切换后所有文案需要重新求值 */
  @Prop langTag: string = '';
  onFinish: () => void = () => {
  };
  onLogin: () => void = () => {
  };
  @State page: number = 0;
  private controller: SwiperController = new SwiperController();

  aboutToAppear(): void {
    Logger.event('展示引导页');
  }

  private imageAt(index: number): Resource {
    if (index === 0) {
      return $r('app.media.onboarding_record');
    }
    return index === 1 ? $r('app.media.onboarding_remind') : $r('app.media.onboarding_share');
  }

  private titleAt(index: number): string {
    if (index === 0) {
      return I18n.t('onboardingTitle1');
    }
    return index === 1 ? I18n.t('onboardingTitle2') : I18n.t('onboardingTitle3');
  }

  private descAt(index: number): string {
    if (index === 0) {
      return I18n.t('onboardingDesc1');
    }
    return index === 1 ? I18n.t('onboardingDesc2') : I18n.t('onboardingDesc3');
  }

  @Builder
  pageContent(index: number) {
    Column() {
      Image(this.imageAt(index))
        .objectFit(ImageFit.Contain)
        .width('100%')
        .constraintSize({ maxWidth: 290, maxHeight: 290 })
        .flexShrink(1)
      Text(this.titleAt(index))
        .fontSize(26)
        .lineHeight(34)
        .fontWeight(FontWeight.Medium)
        .fontColor(Theme.textPrimary(this.isDark))
        .textAlign(TextAlign.Center)
        .margin({ top: 24 })
      Text(this.descAt(index))
        .fontSize(14)
        .lineHeight(22)
        .fontColor(Theme.textSecondary(this.isDark))
        .textAlign(TextAlign.Center)
        .margin({ top: 14 })
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .padding({ left: 8, right: 8 })
  }

  build() {
    Column() {
      Row() {
        Blank()
        Text(I18n.t('skip'))
          .fontSize(15)
          .fontColor(Theme.accent(this.isDark))
          .padding(8)
          .onClick(() => {
            Logger.event('跳过引导');
            this.onFinish();
          })
      }
      .width('100%')
      .padding({ top: 4 })

      Swiper(this.controller) {
        ForEach([0, 1, 2], (index: number) => {
          this.pageContent(index)
        }, (index: number) => index.toString())
      }
      .index(this.page)
      .indicator(false)
      .loop(false)
      .layoutWeight(1)
      .onChange((index: number) => {
        this.page = index;
        Logger.event('引导页翻到第 ' + (index + 1).toString() + ' 页');
      })

      Row({ space: 7 }) {
        ForEach([0, 1, 2], (index: number) => {
          Column()
            .width(index === this.page ? 16 : 6)
            .height(6)
            .borderRadius(3)
            .backgroundColor(index === this.page ? Theme.accent(this.isDark) : Theme.textSecondary(this.isDark))
            .opacity(index === this.page ? 1 : 0.35)
            .animation({ duration: 200, curve: Curve.EaseOut })
        }, (index: number) => 'dot' + index.toString())
      }
      .margin({ bottom: 20 })

      GlassPrimaryButton({
        title: this.page === 2 ? I18n.t('enterApp') : I18n.t('next'),
        isDark: this.isDark,
        onTap: () => {
          if (this.page === 2) {
            Logger.event('引导页完成，进入应用');
            this.onFinish();
          } else {
            this.page = this.page + 1;
            this.controller.showNext();
          }
        }
      })

      Text(I18n.t('haveAccountLogin'))
        .fontSize(13)
        .fontColor(Theme.accent(this.isDark))
        .padding({ top: 16, bottom: 16 })
        .onClick(() => {
          this.onLogin();
        })
    }
    .width('100%')
    .height('100%')
    .padding({ left: 24, right: 24 })
  }
}
