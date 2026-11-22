import { AbilityConstant, Configuration, ConfigurationConstant, UIAbility, Want } from '@kit.AbilityKit';
import { hilog } from '@kit.PerformanceAnalysisKit';
import { window } from '@kit.ArkUI';
import { AppSettings } from '../service/AppSettings';
import { SessionManager } from '../service/SessionManager';
import { Logger } from '../core/Logger';

const DOMAIN: number = 0x0D01;

/**
 * 应用入口。这里只做三件事：初始化存储与设置、装载首页、同步系统栏颜色。
 * 业务状态全部在 service 层，Ability 不持有业务数据。
 */
export default class EntryAbility extends UIAbility {
  private stage: window.WindowStage | null = null;

  onCreate(want: Want, launchParam: AbilityConstant.LaunchParam): void {
    AppSettings.init(this.context);
    SessionManager.shared().init();
    Logger.event('应用启动');
  }

  onDestroy(): void {
    Logger.event('应用退出');
  }

  onWindowStageCreate(windowStage: window.WindowStage): void {
    this.stage = windowStage;
    // 主题变化时由 AppSettings 通知这里重绘系统栏
    AppSettings.onSystemBarsDirty = () => {
      this.applySystemBars();
    };
    windowStage.loadContent('pages/Index', (err) => {
      if (err.code) {
        Logger.error('首页加载失败：' + JSON.stringify(err));
        return;
      }
      Logger.event('首页加载完成');
      this.applySystemBars();
    });
  }

  /** 配置变化（系统深浅色或应用内切换主题）时刷新状态栏文字颜色。 */
  onConfigurationUpdate(newConfig: Configuration): void {
    AppSettings.refreshDarkFlag(this.context);
    this.applySystemBars();
  }

  /**
   * 配置沉浸式系统栏：
   * 1. 窗口采用全屏布局，页面背景可以延伸到状态栏与底部手势条下方，避免出现白边；
   * 2. 状态栏、导航栏底色透明，只保留图标文字，图标颜色跟随深浅色主题；
   * 3. 把安全区高度写入 AppStorage，页面按需取用（个别页面需要自己控制顶部留白）。
   */
  private applySystemBars(): void {
    if (this.stage === null) {
      return;
    }
    try {
      const mainWindow: window.Window = this.stage.getMainWindowSync();
      const dark: boolean = AppStorage.get<boolean>('isDark') === true;
      const contentColor: string = dark ? '#FFFFFF' : '#4A2511';

      // 全屏布局：内容可绘制到状态栏/导航栏区域，由页面自己用 expandSafeArea 控制
      mainWindow.setWindowLayoutFullScreen(true).catch((err: Error) => {
        Logger.warn('设置全屏布局失败：' + err.message);
      });

      // 系统栏透明，让渐变背景透出来
      mainWindow.setWindowSystemBarProperties({
        statusBarColor: '#00000000',
        statusBarContentColor: contentColor,
        navigationBarColor: '#00000000',
        navigationBarContentColor: contentColor
      }).catch((err: Error) => {
        Logger.warn('设置系统栏颜色失败：' + err.message);
      });

      // 记录安全区尺寸，供页面计算顶部/底部留白
      const avoid: window.AvoidArea = mainWindow.getWindowAvoidArea(window.AvoidAreaType.TYPE_SYSTEM);
      const topInset: number = px2vp(avoid.topRect.height);
      const bottomInset: number = px2vp(avoid.bottomRect.height);
      AppStorage.setOrCreate('topInset', topInset);
      AppStorage.setOrCreate('bottomInset', bottomInset);
      Logger.event('沉浸式系统栏已配置，状态栏高度=' + topInset.toFixed(1) + 'vp，底部安全区=' +
        bottomInset.toFixed(1) + 'vp');
    } catch (err) {
      Logger.warn('配置沉浸式系统栏失败');
    }
  }

  onWindowStageDestroy(): void {
    this.stage = null;
    hilog.info(DOMAIN, 'Daymark', '%{public}s', '窗口销毁');
  }
}

