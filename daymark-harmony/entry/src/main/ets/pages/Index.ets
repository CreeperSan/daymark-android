import { Theme } from '../view/Theme';
import { I18n } from '../core/i18n/Strings';
import { Logger } from '../core/Logger';
import { AppSettings, StoreKeys } from '../service/AppSettings';
import { SessionManager } from '../service/SessionManager';
import { AnniversaryStore } from '../service/AnniversaryStore';
import { GlassBackground } from '../view/components/Glass';
import { OnboardingView } from '../view/OnboardingView';
import { LoginView } from '../view/LoginView';
import { HomeView } from '../view/HomeView';
import { SettingsView } from '../view/SettingsView';
import { EditView } from '../view/EditView';
import { AccountView } from '../view/AccountView';
import { ChangePasswordView } from '../view/ChangePasswordView';
import { AboutView } from '../view/AboutView';
import { Routes, RouteParam } from '../view/Routes';

/**
 * 应用根容器：统一提供渐变背景、导航栈与全局轻提示。
 * 与 iOS 的 DaymarkApp 一致——未完成引导显示引导页，未登录显示登录页，已登录显示首页。
 */
@Entry
@Component
struct Index {
  @StorageProp('isDark') isDark: boolean = false;
  /** 状态栏高度（vp），窗口改为全屏布局后由页面自己让出顶部空间 */
  @StorageProp('topInset') topInset: number = 0;
  @StorageLink('sessionVersion') @Watch('onSessionChanged') sessionVersion: number = 0;
  @StorageLink('toastMessage') toast: string = '';
  @StorageLink('language') @Watch('onLanguageChanged') language: string = 'system';
  @State path: NavPathStack = new NavPathStack();
  @State onboarding: boolean = false;
  @State sessionId: number = 0;
  /**
   * 界面代次。语言变化时递增，配合外层 ForEach 用新 key 重建整棵导航树。
   * 原因：页面里的文字是构建时求值的，语言对象换了之后已存在的页面不会自动重建。
   */
  @State uiEpoch: number = 0;
  private session: SessionManager = SessionManager.shared();

  aboutToAppear(): void {
    AppSettings.init(getContext(this));
    this.session.init();
    this.onboarding = AppSettings.onboardingComplete();
    this.sessionId = this.session.session === null ? 0 : this.session.session.id;
    Logger.event('应用界面显示，已登录=' + (this.sessionId > 0).toString() +
      '，引导完成=' + this.onboarding.toString() +
      '，语言=' + I18n.language());
  }

  /** 账号切换时清空导航栈，避免上一个账号的编辑页残留。 */
  onSessionChanged(): void {
    const id: number = this.session.session === null ? 0 : this.session.session.id;
    if (id !== this.sessionId) {
      this.sessionId = id;
      this.path.clear();
      AnniversaryStore.shared().clear();
      Logger.event(id === 0 ? '会话已清除，返回登录页' : '会话已建立，进入首页');
    }
  }

  /** 语言变化后重建界面，保证所有页面文案立即刷新。 */
  onLanguageChanged(): void {
    this.uiEpoch = this.uiEpoch + 1;
    Logger.event('语言设置变化，界面重建：' + this.language + ' → ' + I18n.language());
  }

  private finishOnboarding(): void {
    AppSettings.setOnboardingComplete(true);
    this.onboarding = true;
    Logger.event('引导页已完成');
  }

  /**
   * 二级页面的背景。
   * NavDestination 默认透明，转场时会同时看到上一页的内容，看起来像两页叠在一起，
   * 所以每个目的页都要自带一层不透明背景。
   *
   * 关键点：这层背景必须和根背景用同一个组件（GlassBackground），并且从窗口 y=0 开始绘制。
   * 之前用「纯线性渐变」代替，渐变里没有根背景那三团柔光，且起点被状态栏顶下去一截，
   * 于是状态栏下方出现一道明显的颜色断层。
   */
  @Builder
  pageBackground() {
    GlassBackground({ isDark: this.isDark })
      .expandSafeArea([SafeAreaType.SYSTEM], [SafeAreaEdge.TOP, SafeAreaEdge.BOTTOM])
  }

  /** 路由表：所有页面自己绘制标题栏，因此统一隐藏系统标题栏。 */
  @Builder
  destination(name: string, param: object) {
    NavDestination() {
      Stack({ alignContent: Alignment.TopStart }) {
        this.pageBackground()

        // 背景铺满整窗，正文自己让出状态栏高度：Navigation 不再统一加 padding，
        // 否则目的页背景会被推下去，露出一条根背景形成断层。
        Column() {
          if (name === Routes.SETTINGS) {
            SettingsView({ isDark: this.isDark, langTag: this.language, path: this.path })
          } else if (name === Routes.EDIT) {
            EditView({ isDark: this.isDark, langTag: this.language, path: this.path, anniversaryId: (param as RouteParam).id })
          } else if (name === Routes.REGISTER) {
            AccountView({ isDark: this.isDark, langTag: this.language, path: this.path, isRegister: true })
          } else if (name === Routes.RESET) {
            AccountView({ isDark: this.isDark, langTag: this.language, path: this.path, isRegister: false })
          } else if (name === Routes.CHANGE_PASSWORD) {
            ChangePasswordView({ isDark: this.isDark, langTag: this.language, path: this.path })
          } else if (name === Routes.ABOUT) {
            AboutView({ isDark: this.isDark, langTag: this.language, path: this.path })
          }
        }
        .width('100%')
        .height('100%')
        .padding({ top: this.topInset })
      }
      .width('100%')
      .height('100%')
    }
    .hideTitleBar(true)
    .backgroundColor('#00000000')
  }

  build() {
    Stack({ alignContent: Alignment.Center }) {
      GlassBackground({ isDark: this.isDark })
        .expandSafeArea([SafeAreaType.SYSTEM], [SafeAreaEdge.TOP, SafeAreaEdge.BOTTOM])

      // key 跟随 uiEpoch 变化：切换语言时用新的 key 重建整棵导航树，保证文案立即刷新
      ForEach([this.uiEpoch], (epoch: number) => {
        Navigation(this.path) {
          // 只有导航根页（引导/登录/首页）需要让出状态栏；
          // Navigation 自身的 padding 会同时作用到目的页，导致目的页背景从状态栏下方才开始绘制，
          // 于是页面背景和根背景之间出现一道颜色断层。这里改成只给根页加留白。
          Column() {
            if (!this.onboarding) {
              OnboardingView({
                isDark: this.isDark,
                langTag: this.language,
                onFinish: () => {
                  this.finishOnboarding();
                },
                onLogin: () => {
                  this.finishOnboarding();
                }
              })
            } else if (this.sessionId === 0) {
              LoginView({ isDark: this.isDark, langTag: this.language, path: this.path })
            } else {
              HomeView({ isDark: this.isDark, langTag: this.language, path: this.path })
            }
          }
          .width('100%')
          .height('100%')
          .padding({ top: this.topInset })
        }
        .hideTitleBar(true)
        .mode(NavigationMode.Stack)
        .navDestination(this.destination)
        .backgroundColor('#00000000')
        .width('100%')
        .height('100%')
      }, (epoch: number) => 'nav_' + epoch.toString())

      if (this.toast.length > 0) {
        Text(this.toast)
          .fontSize(16)
          .fontColor(this.isDark ? '#F1DED3' : '#5D433C')
          .textAlign(TextAlign.Center)
          .padding({ left: 28, right: 28, top: 18, bottom: 18 })
          .backgroundColor(this.isDark ? '#F23A3038' : '#F2F4E7DE')
          .borderRadius(28)
          .constraintSize({ maxWidth: '86%' })
          .hitTestBehavior(HitTestMode.None)
          .transition(TransitionEffect.OPACITY.animation({ duration: 180 }))
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor(Theme.backgroundColors(this.isDark)[0])
  }
}
