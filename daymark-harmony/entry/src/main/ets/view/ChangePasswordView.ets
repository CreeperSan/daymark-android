import { Theme, Metrics } from './Theme';
import { I18n } from '../core/i18n/Strings';
import { Logger } from '../core/Logger';
import { Toast } from '../core/Toast';
import { ErrorText } from '../core/ErrorText';
import { GlassCard, GlassHeader, GlassInput, GlassPrimaryButton } from './components/Glass';
import { SessionManager } from '../service/SessionManager';

const MIN_PASSWORD_LENGTH: number = 8;

/** 修改密码：成功后服务端会吊销全部令牌，本机立即退出登录并提示重新登录。 */
@Component
export struct ChangePasswordView {
  @Prop isDark: boolean = false;
  /** 语言标签：只用于触发页面重建，语言切换后所有文案需要重新求值 */
  @Prop langTag: string = '';
  path: NavPathStack = new NavPathStack();
  @State oldPassword: string = '';
  @State newPassword: string = '';
  @State confirmation: string = '';
  @State loading: boolean = false;
  @State error: string = '';

  aboutToAppear(): void {
    Logger.event('打开修改密码页');
  }

  private canSubmit(): boolean {
    return !this.loading && this.oldPassword.length > 0 && this.newPassword.length >= MIN_PASSWORD_LENGTH &&
      this.confirmation === this.newPassword;
  }

  private async submit(): Promise<void> {
    if (!this.canSubmit()) {
      return;
    }
    this.loading = true;
    this.error = '';
    try {
      await SessionManager.shared().changePassword(this.oldPassword, this.newPassword);
      Logger.event('密码修改成功，已退出登录');
      Toast.show(I18n.t('passwordChangedRelogin'));
    } catch (err) {
      this.error = ErrorText.of(err as Object);
      Logger.event('密码修改失败');
    } finally {
      this.loading = false;
    }
  }

  build() {
    Column() {
      GlassHeader({
        title: I18n.t('changePassword'),
        showBack: true,
        isDark: this.isDark,
        onBack: () => {
          this.path.pop();
        }
      })

      Scroll() {
        Column() {
          GlassCard({ isDark: this.isDark, radius: 28, padH: 20, padV: 20, marginTop: 8 }) {
            Column({ space: 14 }) {
              Text(I18n.t('changePasswordHint'))
                .fontSize(13)
                .lineHeight(20)
                .fontColor(Theme.textSecondary(this.isDark))
                .width('100%')
              GlassInput({
                placeholder: I18n.t('oldPassword'),
                text: this.oldPassword,
                secure: true,
                isDark: this.isDark,
                onTextChange: (value: string) => {
                  this.oldPassword = value;
                }
              })
              GlassInput({
                placeholder: I18n.t('setNewPassword'),
                text: this.newPassword,
                secure: true,
                isDark: this.isDark,
                onTextChange: (value: string) => {
                  this.newPassword = value;
                }
              })
              GlassInput({
                placeholder: I18n.t('confirmNewPassword'),
                text: this.confirmation,
                secure: true,
                isDark: this.isDark,
                onTextChange: (value: string) => {
                  this.confirmation = value;
                }
              })
              if (this.confirmation.length > 0 && this.confirmation !== this.newPassword) {
                Text(I18n.t('passwordMismatch'))
                  .fontSize(12)
                  .fontColor(Theme.danger(this.isDark))
                  .width('100%')
              }
              if (this.error.length > 0) {
                Text(this.error)
                  .fontSize(12)
                  .fontColor(Theme.danger(this.isDark))
                  .width('100%')
              }
              GlassPrimaryButton({
                title: this.loading ? I18n.t('pleaseWait') : I18n.t('confirmChange'),
                active: this.canSubmit(),
                isDark: this.isDark,
                onTap: () => {
                  this.submit();
                }
              })
            }
            .width('100%')
            .alignItems(HorizontalAlign.Start)
          }
        }
        .width('100%')
        // Scroll 内容不足一屏时会垂直居中，这里强制从顶部开始排布
        .constraintSize({ minHeight: '100%' })
        .justifyContent(FlexAlign.Start)
        .padding({ left: 24, right: 24, top: 8, bottom: 24 })
      }
      .width('100%')
      .layoutWeight(1)
      .scrollBar(BarState.Off)
    }
    .width('100%')
    .height('100%')
  }
}
