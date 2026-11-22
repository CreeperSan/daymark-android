import { common } from '@kit.AbilityKit';
import { Theme, Metrics } from './Theme';
import { I18n } from '../core/i18n/Strings';
import { Logger } from '../core/Logger';
import { Toast } from '../core/Toast';
import { GlassCard, GlassHeader } from './components/Glass';
import { Icon } from './components/Icon';
import { AppSettings } from '../service/AppSettings';
import { Launcher } from '../common/Launcher';
import { LegalLinks } from '../common/LegalLinks';

const DEVELOPER_URL: string = 'https://creepersan.com';
const CONTACT_EMAIL: string = 'admin@creepersan.com';

/** 关于页：品牌说明、版本、开发者与联系入口，内容与 iOS 版一致。 */
@Component
export struct AboutView {
  @Prop isDark: boolean = false;
  /** 语言标签：只用于触发页面重建，语言切换后所有文案需要重新求值 */
  @Prop langTag: string = '';
  path: NavPathStack = new NavPathStack();

  aboutToAppear(): void {
    Logger.event('打开关于页');
  }

  private async open(url: string): Promise<void> {
    try {
      const context = this.getUIContext().getHostContext() as common.UIAbilityContext;
      await Launcher.open(context, url);
    } catch (err) {
      Toast.show(I18n.t('mailUnavailable'));
    }
  }

  private async mail(): Promise<void> {
    try {
      const context = this.getUIContext().getHostContext() as common.UIAbilityContext;
      await Launcher.openMail(context, CONTACT_EMAIL, I18n.t('aboutApp'), '');
    } catch (err) {
      Toast.show(I18n.t('mailUnavailable'));
    }
  }

  @Builder
  linkRow(title: string, action: () => void) {
    Row() {
      Text(I18n.t(title))
        .fontSize(12)
        .fontColor(Theme.textSecondary(this.isDark))
      Blank()
      Icon({ name: 'chevron', glyphSize: 11, color: Theme.textSecondary(this.isDark) })
    }
    .width('100%')
    .height(40)
    .onClick(() => {
      action();
    })
  }

  build() {
    Column() {
      GlassHeader({
        title: I18n.t('aboutApp'),
        showBack: true,
        isDark: this.isDark,
        onBack: () => {
          this.path.pop();
        }
      })

      Scroll() {
        Column({ space: 14 }) {
          Column() {
            GlassCard({ isDark: this.isDark, radius: 30, padH: 0, padV: 0 }) {
              Column() {
                Icon({ name: 'heart', glyphSize: 36, color: Theme.accent(this.isDark) })
              }
              .width(88)
              .height(88)
              .justifyContent(FlexAlign.Center)
            }
          }
          .width(88)
          .height(88)

          Text(I18n.t('appName'))
            .fontSize(24)
            .fontWeight(FontWeight.Medium)
            .fontColor(Theme.textPrimary(this.isDark))
          Text(I18n.t('aboutTagline'))
            .fontSize(14)
            .fontColor(Theme.accent(this.isDark))

          GlassCard({ isDark: this.isDark, radius: 24, padH: 20, padV: 20, marginTop: 6 }) {
            Column({ space: 8 }) {
              Text(I18n.t('aboutWhy'))
                .fontSize(14)
                .fontWeight(FontWeight.Medium)
                .fontColor(Theme.textPrimary(this.isDark))
                .width('100%')
              Text(I18n.t('aboutBody'))
                .fontSize(13)
                .lineHeight(21)
                .fontColor(Theme.textSecondary(this.isDark))
                .width('100%')
            }
            .width('100%')
            .alignItems(HorizontalAlign.Start)
          }

          GlassCard({ isDark: this.isDark, radius: 20, padH: 16, padV: 4 }) {
            Column() {
              Row() {
                Text(I18n.t('currentVersion'))
                  .fontSize(12)
                  .fontColor(Theme.textSecondary(this.isDark))
                Blank()
                Text(AppSettings.versionName)
                  .fontSize(12)
                  .fontColor(Theme.textSecondary(this.isDark))
              }
              .width('100%')
              .height(40)

              this.linkRow('developer', () => {
                this.open(DEVELOPER_URL);
              })
              this.linkRow('contactUs', () => {
                this.mail();
              })
            }
            .width('100%')
          }

          // 两个文档分别是独立页面，这里拆成两个可点击链接
          Row({ space: 6 }) {
            Text(I18n.t('privacyPolicy'))
              .fontSize(12)
              .fontColor(Theme.accent(this.isDark))
              .onClick(() => {
                this.open(LegalLinks.PRIVACY);
              })
            Text('·')
              .fontSize(12)
              .fontColor(Theme.textSecondary(this.isDark))
            Text(I18n.t('serviceAgreement'))
              .fontSize(12)
              .fontColor(Theme.accent(this.isDark))
              .onClick(() => {
                this.open(LegalLinks.TERMS);
              })
          }

          Text(I18n.t('copyright'))
            .fontSize(11)
            .fontColor(Theme.textSecondary(this.isDark))
            .margin({ top: 6 })
        }
        .width('100%')
        .padding({ left: 24, right: 24, top: 24, bottom: 24 })
        .alignItems(HorizontalAlign.Center)
      }
      .width('100%')
      .layoutWeight(1)
      .scrollBar(BarState.Off)
    }
    .width('100%')
    .height('100%')
  }
}
