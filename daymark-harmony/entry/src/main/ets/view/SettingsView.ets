import { common } from '@kit.AbilityKit';
import { Theme, Metrics } from './Theme';
import { I18n } from '../core/i18n/Strings';
import { Logger } from '../core/Logger';
import { Toast } from '../core/Toast';
import { ErrorText } from '../core/ErrorText';
import { GlassCard, GlassHeader, GlassInput, GlassPrimaryButton } from './components/Glass';
import { ConfirmOverlay } from './components/ConfirmOverlay';
import { Icon } from './components/Icon';
import { SessionManager } from '../service/SessionManager';
import { AppSettings, ThemeMode, LanguageSetting } from '../service/AppSettings';
import { UserSession } from '../model/UserSession';
import { Routes, RouteParam } from './Routes';
import { Launcher } from '../common/Launcher';
import { LegalLinks } from '../common/LegalLinks';

const DEVELOPER_URL: string = 'https://creepersan.com';
const CONTACT_EMAIL: string = 'admin@creepersan.com';
const DEVELOPER_TAP_COUNT: number = 8;
const DEVELOPER_TAP_INTERVAL: number = 500;

type SheetKind = 'none' | 'nickname' | 'theme' | 'language' | 'accountHost' | 'serverHost';

/**
 * 设置页。iOS 版没有头像、模式切换与经典版入口，这里保持一致。
 * 连续点击标题 8 次开启开发者设置，与两端行为相同。
 */
@Component
export struct SettingsView {
  @Prop isDark: boolean = false;
  /** 语言标签：只用于触发页面重建，语言切换后所有文案需要重新求值 */
  @Prop langTag: string = '';
  path: NavPathStack = new NavPathStack();
  @StorageLink('sessionVersion') sessionVersion: number = 0;
  @State developer: boolean = false;
  @State mockFailure: boolean = false;
  @State theme: string = 'system';
  @State language: string = 'system';
  @State titleTaps: number = 0;
  @State lastTapAt: number = 0;
  @State showSheet: boolean = false;
  @State sheet: SheetKind = 'none';
  @State nickname: string = '';
  @State hostValue: string = '';
  @State hostInvalid: boolean = false;
  @State saving: boolean = false;
  /** 当前需要确认的操作：空字符串表示没有弹层 */
  @State confirmKind: string = '';
  @StorageProp('bottomInset') bottomInset: number = 0;
  private session: SessionManager = SessionManager.shared();

  aboutToAppear(): void {
    this.developer = AppSettings.developer();
    this.mockFailure = AppSettings.mockFailure();
    this.theme = AppSettings.theme();
    this.language = AppSettings.language();
    Logger.event('打开设置页');
  }

  private currentSession(): UserSession | null {
    return this.session.session;
  }

  private accountText(): string {
    const current: UserSession | null = this.session.session;
    if (current === null || current.account.length === 0) {
      return '—';
    }
    return current.account;
  }

  private emailText(): string {
    const current: UserSession | null = this.session.session;
    if (current === null || current.email.length === 0) {
      return '—';
    }
    return current.email;
  }

  private nameText(): string {
    const current: UserSession | null = this.session.session;
    if (current === null || current.name.length === 0) {
      return '—';
    }
    return current.name;
  }

  private themeLabel(): string {
    if (this.theme === 'light') {
      return I18n.t('light');
    }
    if (this.theme === 'dark') {
      return I18n.t('dark');
    }
    return I18n.t('followSystem');
  }

  private languageLabel(): string {
    if (this.language === 'zh-Hans') {
      return I18n.t('zhHansName');
    }
    if (this.language === 'zh-Hant') {
      return I18n.t('zhHantName');
    }
    if (this.language === 'en') {
      return I18n.t('enName');
    }
    return I18n.t('followSystem');
  }

  /** 标题连击计时：相邻两次点击超过阈值就重新计数。 */
  private tapTitle(): void {
    const now: number = Date.now();
    this.titleTaps = now - this.lastTapAt <= DEVELOPER_TAP_INTERVAL ? this.titleTaps + 1 : 1;
    this.lastTapAt = now;
    if (this.titleTaps >= DEVELOPER_TAP_COUNT) {
      this.titleTaps = 0;
      this.developer = true;
      AppSettings.setDeveloper(true);
      Toast.show(I18n.t('developerSettings'));
    }
  }

  private openSheet(kind: SheetKind): void {
    this.sheet = kind;
    if (kind === 'nickname') {
      const current: UserSession | null = this.currentSession();
      this.nickname = current === null ? '' : current.name;
    } else if (kind === 'accountHost') {
      this.hostValue = AppSettings.accountHost();
      this.hostInvalid = false;
    } else if (kind === 'serverHost') {
      this.hostValue = AppSettings.serverHost();
      this.hostInvalid = false;
    }
    this.showSheet = true;
  }

  private closeSheet(): void {
    this.showSheet = false;
    this.sheet = 'none';
  }

  private validHost(value: string): boolean {
    if (value.length === 0) {
      return true;
    }
    const lower: string = value.toLowerCase();
    if (!lower.startsWith('http://') && !lower.startsWith('https://')) {
      return false;
    }
    const rest: string = lower.substring(lower.indexOf('://') + 3);
    if (rest.length === 0 || rest.indexOf(' ') >= 0) {
      return false;
    }
    const parts: string[] = rest.split('/');
    return parts[0].length > 0;
  }

  private saveHost(): void {
    const value: string = this.hostValue.trim();
    if (!this.validHost(value)) {
      this.hostInvalid = true;
      return;
    }
    if (this.sheet === 'accountHost') {
      AppSettings.setAccountHost(value);
    } else {
      AppSettings.setServerHost(value);
    }
    Logger.event('保存代理地址');
    this.closeSheet();
  }

  private async saveNickname(): Promise<void> {
    if (this.saving) {
      return;
    }
    const value: string = this.nickname.trim();
    if (value.length === 0) {
      return;
    }
    this.saving = true;
    try {
      await this.session.updateNickname(value);
      Logger.event('昵称修改成功');
      this.closeSheet();
    } catch (err) {
      Logger.event('昵称修改失败');
      Toast.show(ErrorText.of(err as Object));
    } finally {
      this.saving = false;
    }
  }

  private confirmLogout(): void {
    this.confirmKind = 'logout';
  }

  private confirmDeleteAccount(): void {
    this.confirmKind = 'deleteAccount';
  }

  /** 确认弹层点“确认”后按类型分发。 */
  private handleConfirm(): void {
    const kind: string = this.confirmKind;
    this.confirmKind = '';
    if (kind === 'logout') {
      Logger.event('确认退出登录');
      this.session.logout();
      this.path.clear();
    } else if (kind === 'deleteAccount') {
      this.composeDeletionMail();
    }
  }

  /** 删除账号走邮件申请，与 iOS 一致；缺少资料时由用户自行填写。 */
  private async composeDeletionMail(): Promise<void> {
    const current: UserSession | null = this.currentSession();
    const account: string = current === null || current.account.length === 0 ? '—' : current.account;
    const email: string = current === null || current.email.length === 0 ? '—' : current.email;
    const id: string = current === null ? '—' : current.id.toString();
    const body: string = '账号：' + account + '\n邮箱：' + email + '\n用户 ID：' + id + '\n\n请协助删除我的账号及全部数据。';
    Logger.event('发起删除账号邮件');
    try {
      const context = this.getUIContext().getHostContext() as common.UIAbilityContext;
      await Launcher.openMail(context, CONTACT_EMAIL, I18n.t('deleteAccountConfirmTitle'), body);
    } catch (err) {
      Toast.show(I18n.t('mailUnavailable'));
    }
  }

  /** 隐私政策与服务协议是两个独立文档，各自打开对应地址。 */
  private async openLegal(url: string): Promise<void> {
    Logger.event('打开法务页面：' + url);
    const context = this.getUIContext().getHostContext() as common.UIAbilityContext;
    await Launcher.open(context, url);
  }

  @Builder
  rowLabel(title: string, value: string, danger: boolean, arrow: boolean) {
    Row() {
      Text(I18n.t(title))
        .fontSize(14)
        .fontColor(danger ? Theme.danger(this.isDark) : Theme.textPrimary(this.isDark))
      Blank()
      if (value.length > 0) {
        Text(value)
          .fontSize(13)
          .fontColor(Theme.textSecondary(this.isDark))
          .maxLines(1)
          .textOverflow({ overflow: TextOverflow.Ellipsis })
          .margin({ left: 12 })
      }
      if (arrow) {
        Icon({ name: 'chevron', glyphSize: 12, color: Theme.textSecondary(this.isDark) })
          .margin({ left: 6 })
      }
    }
    .width('100%')
    .height(56)
    .padding({ left: 12, right: 12 })
  }

  @Builder
  sectionTitle(title: string) {
    Text(I18n.t(title))
      .fontSize(12)
      .fontColor(Theme.textSecondary(this.isDark))
      .margin({ top: 14, bottom: 8, left: 4 })
  }

  @Builder
  optionRow(key: string, label: string, current: string, pick: (value: string) => void) {
    Row() {
      Text(label)
        .fontSize(15)
        .fontColor(Theme.textPrimary(this.isDark))
      Blank()
      if (current === key) {
        Icon({ name: 'check', glyphSize: 16, color: Theme.accent(this.isDark) })
      }
    }
    .width('100%')
    .height(48)
    .padding({ left: 4, right: 4 })
    .onClick(() => {
      pick(key);
    })
  }

  /** 主题 / 语言 / 昵称 / 代理共用一个底部弹层，按 sheet 字段切换内容。 */
  @Builder
  sheetContent() {
    Column({ space: 16 }) {
      if (this.sheet === 'nickname') {
        Text(I18n.t('nickname'))
          .fontSize(18)
          .fontWeight(FontWeight.Medium)
          .fontColor(Theme.textPrimary(this.isDark))
          .width('100%')
        GlassInput({
          placeholder: I18n.t('nickname'),
          text: this.nickname,
          isDark: this.isDark,
          onTextChange: (value: string) => {
            this.nickname = value;
          }
        })
        GlassPrimaryButton({
          title: this.saving ? I18n.t('saving') : I18n.t('save'),
          active: !this.saving && this.nickname.trim().length > 0,
          isDark: this.isDark,
          onTap: () => {
            this.saveNickname();
          }
        })
      } else if (this.sheet === 'theme') {
        Text(I18n.t('theme'))
          .fontSize(18)
          .fontWeight(FontWeight.Medium)
          .fontColor(Theme.textPrimary(this.isDark))
          .width('100%')
        this.optionRow('system', I18n.t('followSystem'), this.theme, (value: string) => {
          this.theme = value;
          AppSettings.setTheme(value as ThemeMode);
          this.closeSheet();
        })
        this.optionRow('light', I18n.t('light'), this.theme, (value: string) => {
          this.theme = value;
          AppSettings.setTheme(value as ThemeMode);
          this.closeSheet();
        })
        this.optionRow('dark', I18n.t('dark'), this.theme, (value: string) => {
          this.theme = value;
          AppSettings.setTheme(value as ThemeMode);
          this.closeSheet();
        })
      } else if (this.sheet === 'language') {
        Text(I18n.t('language'))
          .fontSize(18)
          .fontWeight(FontWeight.Medium)
          .fontColor(Theme.textPrimary(this.isDark))
          .width('100%')
        this.optionRow('system', I18n.t('followSystem'), this.language, (value: string) => {
          this.language = value;
          AppSettings.setLanguage(value as LanguageSetting);
          this.closeSheet();
        })
        this.optionRow('zh-Hans', I18n.t('zhHansName'), this.language, (value: string) => {
          this.language = value;
          AppSettings.setLanguage(value as LanguageSetting);
          this.closeSheet();
        })
        this.optionRow('zh-Hant', I18n.t('zhHantName'), this.language, (value: string) => {
          this.language = value;
          AppSettings.setLanguage(value as LanguageSetting);
          this.closeSheet();
        })
        this.optionRow('en', I18n.t('enName'), this.language, (value: string) => {
          this.language = value;
          AppSettings.setLanguage(value as LanguageSetting);
          this.closeSheet();
        })
      } else if (this.sheet === 'accountHost' || this.sheet === 'serverHost') {
        Text(I18n.t(this.sheet === 'accountHost' ? 'accountProxy' : 'serverProxy'))
          .fontSize(18)
          .fontWeight(FontWeight.Medium)
          .fontColor(Theme.textPrimary(this.isDark))
          .width('100%')
        Text(I18n.t('hostHint'))
          .fontSize(12)
          .fontColor(Theme.textSecondary(this.isDark))
          .width('100%')
        GlassInput({
          placeholder: I18n.t(this.sheet === 'accountHost' ? 'accountProxy' : 'serverProxy'),
          text: this.hostValue,
          isDark: this.isDark,
          inputType: InputType.URL,
          onTextChange: (value: string) => {
            this.hostValue = value;
          }
        })
        if (this.hostInvalid) {
          Text(I18n.t('invalidHost'))
            .fontSize(12)
            .fontColor(Theme.danger(this.isDark))
            .width('100%')
        }
        GlassPrimaryButton({
          title: I18n.t('save'),
          isDark: this.isDark,
          onTap: () => {
            this.saveHost();
          }
        })
      }
    }
    .width('100%')
    .alignItems(HorizontalAlign.Start)
    // 顶部留出拖动条（dragBar）占用的高度，标题才不会贴着弹层边缘；
    // 底部要让开手势条，否则最后一项会压到系统的横条上
    .padding({ left: 24, right: 24, top: 34, bottom: 32 + this.bottomInset })
  }

  build() {
    Stack() {
      Column() {
        GlassHeader({
          title: I18n.t('settings'),
          showBack: true,
          isDark: this.isDark,
          onBack: () => {
            this.path.pop();
          },
          onTitleTap: () => {
            this.tapTitle();
          }
        })

        Scroll() {
          Column({ space: 4 }) {
            if (this.currentSession() !== null) {
              this.sectionTitle('personalInfo')
              GlassCard({ isDark: this.isDark, radius: 20, padH: 6, padV: 6 }) {
                Column() {
                  this.rowLabel('username', this.accountText(), false, false)
                  this.rowLabel('emailLabel', this.emailText(), false, false)
                  Row() {
                    this.rowLabel('nickname', this.nameText(), false, true)
                  }
                  .width('100%')
                  .onClick(() => {
                    this.openSheet('nickname');
                  })
                  Row() {
                    this.rowLabel('changePassword', '', false, true)
                  }
                  .width('100%')
                  .onClick(() => {
                    this.path.pushPath({ name: Routes.CHANGE_PASSWORD, param: new RouteParam(0) });
                  })
                  Row() {
                    this.rowLabel('deleteAccount', '', true, true)
                  }
                  .width('100%')
                  .onClick(() => {
                    this.confirmDeleteAccount();
                  })
                  Row() {
                    Text(I18n.t('logout'))
                      .fontSize(14)
                      .fontColor(Theme.danger(this.isDark))
                  }
                  .width('100%')
                  .height(56)
                  .justifyContent(FlexAlign.Center)
                  .onClick(() => {
                    this.confirmLogout();
                  })
                }
                .width('100%')
              }
            }

            this.sectionTitle('appSection')
            GlassCard({ isDark: this.isDark, radius: 20, padH: 6, padV: 6 }) {
              Column() {
                Row() {
                  this.rowLabel('theme', this.themeLabel(), false, true)
                }
                .width('100%')
                .onClick(() => {
                  this.openSheet('theme');
                })
                Row() {
                  this.rowLabel('language', this.languageLabel(), false, true)
                }
                .width('100%')
                .onClick(() => {
                  this.openSheet('language');
                })
              }
              .width('100%')
            }

            if (this.developer) {
              this.sectionTitle('developerSettings')
              GlassCard({ isDark: this.isDark, radius: 20, padH: 6, padV: 6 }) {
                Column() {
                  Row() {
                    this.rowLabel('accountProxy', '', false, true)
                  }
                  .width('100%')
                  .onClick(() => {
                    this.openSheet('accountHost');
                  })
                  Row() {
                    this.rowLabel('serverProxy', '', false, true)
                  }
                  .width('100%')
                  .onClick(() => {
                    this.openSheet('serverHost');
                  })
                  Row() {
                    Column({ space: 2 }) {
                      Text(I18n.t('mockFailure'))
                        .fontSize(14)
                        .fontColor(Theme.textPrimary(this.isDark))
                      Text(I18n.t('mockFailureHint'))
                        .fontSize(11)
                        .fontColor(Theme.textSecondary(this.isDark))
                    }
                    .alignItems(HorizontalAlign.Start)
                    .layoutWeight(1)
                    Toggle({ type: ToggleType.Switch, isOn: this.mockFailure })
                      .selectedColor(Theme.accent(this.isDark))
                      .onChange((value: boolean) => {
                        this.mockFailure = value;
                        AppSettings.setMockFailure(value);
                        Logger.event(value ? '开启模拟网络异常' : '关闭模拟网络异常');
                      })
                  }
                  .width('100%')
                  .height(64)
                  .padding({ left: 12, right: 12 })
                  Row() {
                    this.rowLabel('disableDeveloper', '', true, false)
                  }
                  .width('100%')
                  .onClick(() => {
                    this.developer = false;
                    AppSettings.setDeveloper(false);
                  })
                }
                .width('100%')
              }
            }

            this.sectionTitle('other')
            GlassCard({ isDark: this.isDark, radius: 20, padH: 6, padV: 6 }) {
              Column() {
                this.rowLabel('appVersion', AppSettings.versionName, false, false)
                Row() {
                  this.rowLabel('aboutApp', '', false, true)
                }
                .width('100%')
                .onClick(() => {
                  this.path.pushPath({ name: Routes.ABOUT, param: new RouteParam(0) });
                })
                Row() {
                  this.rowLabel('privacyPolicy', '', false, true)
                }
                .width('100%')
                .onClick(() => {
                  this.openLegal(LegalLinks.PRIVACY);
                })
                Row() {
                  this.rowLabel('serviceAgreement', '', false, true)
                }
                .width('100%')
                .onClick(() => {
                  this.openLegal(LegalLinks.TERMS);
                })
              }
              .width('100%')
            }

            Text(I18n.t('copyright'))
              .fontSize(11)
              .fontColor(Theme.textSecondary(this.isDark))
              .width('100%')
              .textAlign(TextAlign.Center)
              .margin({ top: 16, bottom: 24 + this.bottomInset })
          }
          .width('100%')
          // Scroll 里的 Column 默认居中，这里显式左对齐，分组标题才不会跑到中间
          .alignItems(HorizontalAlign.Start)
          .padding({ left: 20, right: 20, top: 4 })
      }
      .width('100%')
      .layoutWeight(1)
      .scrollBar(BarState.Off)
      }
      .width('100%')
      .height('100%')

      if (this.confirmKind.length > 0) {
        ConfirmOverlay({
          isDark: this.isDark,
          title: this.confirmKind === 'logout' ? I18n.t('logoutConfirmTitle') : I18n.t('deleteAccountConfirmTitle'),
          message: this.confirmKind === 'logout' ? I18n.t('logoutConfirmMessage') : I18n.t('deleteAccountMessage'),
          confirmText: I18n.t('confirm'),
          danger: true,
          onCancel: () => {
            this.confirmKind = '';
          },
          onConfirm: () => {
            this.handleConfirm();
          }
        })
      }
    }
    .width('100%')
    .height('100%')
    .bindSheet($$this.showSheet, this.sheetContent(), {
      // 高度由内容决定：写死高度要么留白、要么把最后一项截掉
      detents: [SheetSize.FIT_CONTENT],
      dragBar: true,
      showClose: false,
      backgroundColor: Theme.glassSolid(this.isDark),
      onDisappear: () => {
        this.showSheet = false;
        this.sheet = 'none';
      }
    })
  }
}
