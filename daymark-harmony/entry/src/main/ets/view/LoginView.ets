import { Theme, Metrics } from './Theme';
import { I18n } from '../core/i18n/Strings';
import { GlassCard, GlassCheck, GlassHeader, GlassInput, GlassPrimaryButton } from './components/Glass';
import { Icon } from './components/Icon';
import { SessionManager } from '../service/SessionManager';
import { AppSettings } from '../service/AppSettings';
import { ApiProvider } from '../core/api/ApiProvider';
import { Toast } from '../core/Toast';
import { ErrorText } from '../core/ErrorText';
import { Routes, RouteParam } from './Routes';
import { Logger } from '../core/Logger';

/** 账号登录。仅在选择“记住我”时保存账号，密码不落盘。 */
@Component
export struct LoginView {
  @Prop isDark: boolean = false;
  /** 语言标签：只用于触发页面重建，语言切换后所有文案需要重新求值 */
  @Prop langTag: string = '';
  path: NavPathStack = new NavPathStack();
  @StorageProp('bottomInset') bottomInset: number = 0;
  @State account: string = '';
  @State password: string = '';
  @State remember: boolean = false;
  @State loading: boolean = false;

  aboutToAppear(): void {
    const remembered: string = AppSettings.rememberedAccount();
    this.account = remembered;
    this.remember = remembered.length > 0;
  }

  private async login(): Promise<void> {
    if (this.loading) {
      return;
    }
    this.loading = true;
    try {
      const trimmed: string = this.account.trim();
      await SessionManager.shared().login(trimmed, this.password);
      AppSettings.setRememberedAccount(this.remember ? trimmed : '');
      this.password = '';
      Toast.show(I18n.t('loginSuccess'));
    } catch (err) {
      Logger.event('登录失败');
      Toast.show(ErrorText.of(err as Object));
    } finally {
      this.loading = false;
    }
  }

  build() {
    Stack() {
      Scroll() {
        Column() {
          Row() {
            Column() {
              GlassCard({ isDark: this.isDark, radius: 20, padH: 0, padV: 0 }) {
                Column() {
                  Icon({ name: 'heart', glyphSize: 26, color: Theme.accent(this.isDark) })
                }
                .width(60)
                .height(60)
                .justifyContent(FlexAlign.Center)
              }
            }
            .width(60)
            .height(60)
            Blank()
            // 未登录时也要能进设置切换主题与语言
            Button({ type: ButtonType.Circle, stateEffect: true }) {
              Icon({ name: 'gear', glyphSize: 18, color: Theme.accent(this.isDark) })
            }
            .width(36)
            .height(36)
            .backgroundColor(Theme.glassFill(this.isDark))
            .border({ width: 1, color: Theme.glassStroke(this.isDark) })
            .onClick(() => {
              this.path.pushPath({ name: Routes.SETTINGS, param: new RouteParam(0) });
            })
          }
          .width('100%')
          .alignItems(VerticalAlign.Top)

          Text(I18n.t('welcomeBack'))
            .fontSize(30)
            .fontWeight(FontWeight.Medium)
            .fontColor(Theme.textPrimary(this.isDark))
            .margin({ top: 16 })
          Text(I18n.t('loginSubtitle'))
            .fontSize(15)
            .lineHeight(22)
            .fontColor(Theme.textSecondary(this.isDark))
            .margin({ top: 8 })

          GlassCard({ isDark: this.isDark, radius: 28, padH: 20, padV: 20, marginTop: 32 }) {
            Column({ space: 14 }) {
              GlassInput({
                placeholder: I18n.t('account'),
                text: this.account,
                isDark: this.isDark,
                onTextChange: (value: string) => {
                  this.account = value;
                }
              })
              GlassInput({
                placeholder: I18n.t('passwordPlaceholder'),
                text: this.password,
                secure: true,
                isDark: this.isDark,
                onTextChange: (value: string) => {
                  this.password = value;
                }
              })
              Row() {
                GlassCheck({
                  checked: this.remember,
                  isDark: this.isDark,
                  label: I18n.t('rememberMe'),
                  onToggle: () => {
                    this.remember = !this.remember;
                  }
                })
                Blank()
                Text(I18n.t('forgotPassword'))
                  .fontSize(13)
                  .fontColor(Theme.accent(this.isDark))
                  .padding({ top: 6, bottom: 6 })
                  .onClick(() => {
                    Logger.event('打开找回密码');
                    this.path.pushPath({ name: Routes.RESET, param: new RouteParam(0) });
                  })
              }
              .width('100%')

              GlassPrimaryButton({
                title: this.loading ? I18n.t('loggingIn') : I18n.t('loginAction'),
                active: !this.loading && this.account.length > 0 && this.password.length > 0,
                isDark: this.isDark,
                onTap: () => {
                  this.login();
                }
              })
            }
            .width('100%')
          }

          if (ApiProvider.useMock) {
            Text(I18n.t('mockAccountHint'))
              .fontSize(12)
              .fontColor(Theme.textSecondary(this.isDark))
              .opacity(0.8)
              .margin({ top: 12 })
          }

          // 注册入口紧跟表单，水平居中；不要被 Blank 推到屏幕最底部
          Row({ space: 6 }) {
            Text(I18n.t('noAccount'))
              .fontSize(13)
              .fontColor(Theme.textSecondary(this.isDark))
            Text(I18n.t('registerNow'))
              .fontSize(13)
              .fontColor(Theme.accent(this.isDark))
              .onClick(() => {
                this.path.pushPath({ name: Routes.REGISTER, param: new RouteParam(0) });
              })
          }
          .width('100%')
          .justifyContent(FlexAlign.Center)
          .padding({ top: 24, bottom: 16 + this.bottomInset })
        }
        .width('100%')
        .height('100%')
        .padding({ left: 24, right: 24, top: 16 })
        .alignItems(HorizontalAlign.Start)
      }
      .width('100%')
      .height('100%')
      .scrollBar(BarState.Off)

      if (this.loading) {
        Column() {
          Row({ space: 14 }) {
            LoadingProgress()
              .width(22)
              .height(22)
              .color(Theme.accent(this.isDark))
            Text(I18n.t('loggingIn'))
              .fontSize(15)
              .fontColor(Theme.textPrimary(this.isDark))
          }
          .padding({ left: 22, right: 22, top: 18, bottom: 18 })
          .backgroundColor(Theme.glassSolid(this.isDark))
          .borderRadius(20)
          .border({ width: 1, color: Theme.glassStroke(this.isDark) })
        }
        .width('100%')
        .height('100%')
        .justifyContent(FlexAlign.Center)
        .backgroundColor('#33000000')
      }
    }
    .width('100%')
    .height('100%')
  }
}
