import { common } from '@kit.AbilityKit';
import { Theme, Metrics } from './Theme';
import { I18n } from '../core/i18n/Strings';
import { Logger } from '../core/Logger';
import { ErrorText } from '../core/ErrorText';
import { GlassCard, GlassCheck, GlassHeader, GlassInput, GlassPrimaryButton } from './components/Glass';
import { Icon } from './components/Icon';
import { ApiProvider } from '../core/api/ApiProvider';
import { Routes, RouteParam } from './Routes';
import { Launcher } from '../common/Launcher';
import { LegalLinks } from '../common/LegalLinks';

const MIN_PASSWORD_LENGTH: number = 8;
const CODE_LENGTH: number = 6;
const RESEND_SECONDS: number = 60;

/**
 * 注册与找回密码共用页面。发送验证码后进入验证码步骤，
 * 与 iOS 的 AccountView 一致：注册需要账号、邮箱、密码与协议勾选。
 */
@Component
export struct AccountView {
  @Prop isDark: boolean = false;
  /** 语言标签：只用于触发页面重建，语言切换后所有文案需要重新求值 */
  @Prop langTag: string = '';
  @Prop isRegister: boolean = true;
  path: NavPathStack = new NavPathStack();
  @State account: string = '';
  @State email: string = '';
  @State password: string = '';
  @State code: string = '';
  @State agreed: boolean = false;
  @State verification: boolean = false;
  @State done: boolean = false;
  @State loading: boolean = false;
  @State error: string = '';
  @State countdown: number = 0;
  @StorageProp('bottomInset') bottomInset: number = 0;
  private timer: number = -1;

  aboutToAppear(): void {
    Logger.event(this.isRegister ? '打开注册页' : '打开找回密码页');
  }

  aboutToDisappear(): void {
    this.stopCountdown();
  }

  private stopCountdown(): void {
    if (this.timer !== -1) {
      clearInterval(this.timer);
      this.timer = -1;
    }
  }

  private startCountdown(): void {
    this.stopCountdown();
    this.countdown = RESEND_SECONDS;
    this.timer = setInterval(() => {
      if (this.countdown <= 1) {
        this.countdown = 0;
        this.stopCountdown();
      } else {
        this.countdown = this.countdown - 1;
      }
    }, 1000);
  }

  private canSend(): boolean {
    if (this.loading || this.email.length === 0) {
      return false;
    }
    if (!this.isRegister) {
      return true;
    }
    return this.account.length > 0 && this.password.length >= MIN_PASSWORD_LENGTH && this.agreed;
  }

  private async sendCode(): Promise<void> {
    if (!this.canSend()) {
      return;
    }
    this.loading = true;
    this.error = '';
    try {
      await ApiProvider.api().requestCode(this.email.trim(), this.isRegister);
      this.verification = true;
      this.startCountdown();
      Logger.event('验证码已发送');
    } catch (err) {
      this.error = ErrorText.of(err as Object);
      Logger.event('验证码发送失败');
    } finally {
      this.loading = false;
    }
  }

  private async submit(): Promise<void> {
    if (this.loading || this.code.length !== CODE_LENGTH || this.password.length < MIN_PASSWORD_LENGTH) {
      return;
    }
    this.loading = true;
    this.error = '';
    try {
      if (this.isRegister) {
        await ApiProvider.api().register(this.account.trim(), this.email.trim(), this.password, this.code);
        Logger.event('注册成功');
      } else {
        await ApiProvider.api().resetPassword(this.email.trim(), this.password, this.code);
        Logger.event('重置密码成功');
      }
      this.password = '';
      this.code = '';
      this.done = true;
    } catch (err) {
      this.error = ErrorText.of(err as Object);
      Logger.event(this.isRegister ? '注册失败' : '重置密码失败');
    } finally {
      this.loading = false;
    }
  }

  /** 隐私政策与服务协议是两个独立文档，各自打开对应地址。 */
  private async openLegal(url: string): Promise<void> {
    try {
      Logger.event('打开法务页面：' + url);
      const context = this.getUIContext().getHostContext() as common.UIAbilityContext;
      await Launcher.open(context, url);
    } catch (err) {
      Logger.warn('打开协议链接失败');
    }
  }

  @Builder
  brandTile() {
    Column() {
      GlassCard({ isDark: this.isDark, radius: 20, padH: 0, padV: 0 }) {
        Column() {
          Icon({ name: this.isRegister ? 'heart' : 'lock', glyphSize: 24, color: Theme.accent(this.isDark) })
        }
        .width(60)
        .height(60)
        .justifyContent(FlexAlign.Center)
      }
    }
    .width(60)
    .height(60)
  }

  @Builder
  codeBoxes() {
    Stack() {
      Row({ space: 8 }) {
        ForEach([0, 1, 2, 3, 4, 5], (index: number) => {
          Text(index < this.code.length ? this.code.substring(index, index + 1) : '')
            .fontSize(22)
            .fontColor(Theme.textPrimary(this.isDark))
            .textAlign(TextAlign.Center)
            .height(58)
            .layoutWeight(1)
            .backgroundColor(Theme.glassFill(this.isDark))
            .borderRadius(14)
            .border({
              width: 1,
              color: index === this.code.length ? Theme.accent(this.isDark) : Theme.glassStroke(this.isDark)
            })
        }, (index: number) => 'code' + index.toString())
      }
      .width('100%')

      TextInput({ text: this.code })
        .type(InputType.Number)
        .maxLength(CODE_LENGTH)
        .width('100%')
        .height(58)
        .opacity(0.02)
        .backgroundColor('#00000000')
        .onChange((value: string) => {
          let digits: string = '';
          for (let i = 0; i < value.length; i++) {
            const ch: string = value.substring(i, i + 1);
            if (ch >= '0' && ch <= '9') {
              digits = digits + ch;
            }
          }
          this.code = digits.length > CODE_LENGTH ? digits.substring(0, CODE_LENGTH) : digits;
        })
    }
    .width('100%')
    .height(58)
  }

  build() {
    Column() {
      GlassHeader({
        title: '',
        showBack: true,
        isDark: this.isDark,
        onBack: () => {
          if (this.verification && !this.done) {
            this.verification = false;
            this.stopCountdown();
          } else {
            this.path.pop();
          }
        }
      })

      Scroll() {
        Column({ space: 0 }) {
          if (!this.verification && !this.done) {
            this.brandTile()
          }

          Text(I18n.t(this.done ? 'operationDone' : (this.verification ? 'inputCode' :
            (this.isRegister ? 'createAccount' : 'resetPasswordTitle'))))
            .fontSize(this.verification ? 26 : 30)
            .fontWeight(FontWeight.Medium)
            .fontColor(Theme.textPrimary(this.isDark))
            .margin({ top: this.verification || this.done ? 8 : 16 })

          Text(this.verification && !this.done ?
            I18n.t('codeSentTo', this.email) :
            I18n.t(this.done ? 'doneDesc' : (this.isRegister ? 'registerSubtitle' : 'resetSubtitle')))
            .fontSize(15)
            .lineHeight(22)
            .fontColor(Theme.textSecondary(this.isDark))
            .margin({ top: 8 })

          if (this.done) {
            GlassPrimaryButton({
              title: I18n.t('backToLogin'),
              isDark: this.isDark,
              marginTop: 30,
              onTap: () => {
                this.path.clear();
              }
            })
          } else if (this.verification) {
            Column() {
              this.codeBoxes()
            }
            .width('100%')
            .margin({ top: 30 })

            if (!this.isRegister) {
              GlassInput({
                placeholder: I18n.t('setNewPassword'),
                text: this.password,
                secure: true,
                isDark: this.isDark,
                marginTop: 14,
                onTextChange: (value: string) => {
                  this.password = value;
                }
              })
            }

            Text(this.countdown > 0 ? I18n.t('resendAfter', this.countdown.toString()) : I18n.t('resendCode'))
              .fontSize(12)
              .fontColor(Theme.accent(this.isDark))
              .margin({ top: 16 })
              .onClick(() => {
                if (this.countdown === 0) {
                  this.sendCode();
                }
              })
          } else {
            GlassCard({ isDark: this.isDark, radius: 28, padH: 20, padV: 20, marginTop: 30 }) {
              Column({ space: 14 }) {
                if (this.isRegister) {
                  GlassInput({
                    placeholder: I18n.t('account'),
                    text: this.account,
                    isDark: this.isDark,
                    onTextChange: (value: string) => {
                      this.account = value;
                    }
                  })
                }
                GlassInput({
                  placeholder: I18n.t('email'),
                  text: this.email,
                  isDark: this.isDark,
                  inputType: InputType.Email,
                  onTextChange: (value: string) => {
                    this.email = value;
                  }
                })
                if (this.isRegister) {
                  GlassInput({
                    placeholder: I18n.t('setPassword'),
                    text: this.password,
                    secure: true,
                    isDark: this.isDark,
                    onTextChange: (value: string) => {
                      this.password = value;
                    }
                  })
                  Row({ space: 6 }) {
                    GlassCheck({
                      checked: this.agreed,
                      isDark: this.isDark,
                      onToggle: () => {
                        Logger.event(this.agreed ? '取消同意协议' : '同意协议');
                        this.agreed = !this.agreed;
                      }
                    })
                    Text(I18n.t('agreePrefix'))
                      .fontSize(12)
                      .fontColor(Theme.textSecondary(this.isDark))
                    // 两份文档分别可点，避免都跳到同一个页面
                    Text(I18n.t('termsLink'))
                      .fontSize(12)
                      .fontColor(Theme.accent(this.isDark))
                      .onClick(() => {
                        this.openLegal(LegalLinks.TERMS);
                      })
                    Text(I18n.t('linkJoiner'))
                      .fontSize(12)
                      .fontColor(Theme.textSecondary(this.isDark))
                    Text(I18n.t('privacyLink'))
                      .fontSize(12)
                      .fontColor(Theme.accent(this.isDark))
                      .onClick(() => {
                        this.openLegal(LegalLinks.PRIVACY);
                      })
                  }
                  .width('100%')
                } else {
                  Text(I18n.t('codeHint'))
                    .fontSize(12)
                    .fontColor(Theme.textSecondary(this.isDark))
                }

                GlassPrimaryButton({
                  title: this.loading ? I18n.t('pleaseWait') : I18n.t('sendCode'),
                  active: this.canSend(),
                  isDark: this.isDark,
                  onTap: () => {
                    this.sendCode();
                  }
                })
              }
              .width('100%')
              // 表单列默认居中，这里左对齐，协议勾选才不会跑到中间
              .alignItems(HorizontalAlign.Start)
            }

            if (!this.isRegister) {
              Text(I18n.t('resendHint'))
                .fontSize(12)
                .fontColor(Theme.textSecondary(this.isDark))
                .margin({ top: 20 })
            }
          }

          if (this.error.length > 0) {
            Text(this.error)
              .fontSize(12)
              .fontColor(Theme.danger(this.isDark))
              .margin({ top: 12 })
          }
        }
        .width('100%')
        // Scroll 内容不足一屏时会垂直居中，这里锁定从顶部开始排布
        .constraintSize({ minHeight: '100%' })
        .justifyContent(FlexAlign.Start)
        .alignItems(HorizontalAlign.Start)
        .padding({ left: 24, right: 24, top: 8, bottom: 24 })
      }
      .width('100%')
      .layoutWeight(1)
      .scrollBar(BarState.Off)

      if (this.verification && !this.done) {
        GlassPrimaryButton({
          title: this.loading ? I18n.t('pleaseWait') : I18n.t('confirmContinue'),
          active: !this.loading && this.code.length === CODE_LENGTH && this.password.length >= MIN_PASSWORD_LENGTH,
          isDark: this.isDark,
          onTap: () => {
            this.submit();
          }
        })
          .padding({ left: 24, right: 24, bottom: 16 + this.bottomInset })
      }
    }
    .width('100%')
    .height('100%')
  }
}
