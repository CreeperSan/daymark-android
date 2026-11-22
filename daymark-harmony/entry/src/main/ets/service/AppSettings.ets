import { common, ConfigurationConstant, bundleManager } from '@kit.AbilityKit';
import { i18n } from '@kit.LocalizationKit';
import { KvStore } from '../core/storage/KvStore';
import { I18n, Lang } from '../core/i18n/Strings';
import { Logger } from '../core/Logger';

/** 应用级设置键。集中声明，避免各页面散落字符串。 */
export class Keys {
  static readonly THEME: string = 'application.theme';
  static readonly LANGUAGE: string = 'application.language';
  static readonly DEVELOPER: string = 'application.developer';
  static readonly ACCOUNT_HOST: string = 'application.accountHost';
  static readonly SERVER_HOST: string = 'application.serverHost';
  static readonly MOCK_FAILURE: string = 'application.mockFailure';
  static readonly ONBOARDING: string = 'application.onboardingComplete';
  static readonly REMEMBERED_ACCOUNT: string = 'application.rememberedAccount';
  static readonly SESSION: string = 'user_session.session';
  static readonly MOCK_ANNIVERSARIES: string = 'mock.anniversaries';
  static readonly MOCK_SEEDED: string = 'mock.seeded';
}

/** AppStorage 中使用到的响应式键。 */
export class StoreKeys {
  static readonly THEME_MODE: string = 'themeMode';
  static readonly LANGUAGE: string = 'language';
  static readonly SESSION_VERSION: string = 'sessionVersion';
  static readonly DATA_VERSION: string = 'dataVersion';
  static readonly TOAST: string = 'toastMessage';
}

export type ThemeMode = 'system' | 'light' | 'dark';
export type LanguageSetting = 'system' | 'zh-Hans' | 'zh-Hant' | 'en';

/**
 * 主题、语言、开发者选项。iOS 版用 @AppStorage 保存，这里落到 preferences，
 * 并把需要驱动界面的值同步到 AppStorage，页面用 @StorageLink 订阅即可。
 */
export class AppSettings {
  private static appContext: common.UIAbilityContext | null = null;
  static versionName: string = '1.0.0';
  static versionCode: number = 1;

  static init(context: common.Context): void {
    AppSettings.appContext = context as common.UIAbilityContext;
    KvStore.init(context);
    try {
      const info: bundleManager.BundleInfo =
        bundleManager.getBundleInfoForSelfSync(bundleManager.BundleFlag.GET_BUNDLE_INFO_DEFAULT);
      AppSettings.versionName = info.versionName;
      AppSettings.versionCode = info.versionCode;
    } catch (err) {
      Logger.warn('读取版本信息失败');
    }
    AppStorage.setOrCreate<string>(StoreKeys.THEME_MODE, AppSettings.theme());
    AppStorage.setOrCreate<string>(StoreKeys.LANGUAGE, AppSettings.language());
    AppSettings.applyLanguage();
    AppSettings.applyColorMode();
  }

  static theme(): ThemeMode {
    const value: string = KvStore.getString(Keys.THEME, 'system');
    if (value === 'light' || value === 'dark') {
      return value;
    }
    return 'system';
  }

  /**
   * 系统栏重绘回调。Ability 在窗口创建时注册，
   * 主题变化后需要同步刷新状态栏文字颜色，否则会出现浅色背景配白色图标。
   */
  static onSystemBarsDirty: () => void = () => {
  };

  static setTheme(value: ThemeMode): void {
    KvStore.set(Keys.THEME, value);
    AppStorage.setOrCreate<string>(StoreKeys.THEME_MODE, value);
    AppSettings.applyColorMode();
    // 主动刷新一次深浅色标记与系统栏，避免只依赖配置变更回调
    if (AppSettings.appContext !== null) {
      AppSettings.refreshDarkFlag(AppSettings.appContext);
    }
    AppSettings.onSystemBarsDirty();
    Logger.event('切换主题：' + value);
  }

  static language(): LanguageSetting {
    const value: string = KvStore.getString(Keys.LANGUAGE, 'system');
    if (value === 'zh-Hans' || value === 'zh-Hant' || value === 'en') {
      return value;
    }
    return 'system';
  }

  static setLanguage(value: LanguageSetting): void {
    KvStore.set(Keys.LANGUAGE, value);
    AppStorage.setOrCreate<string>(StoreKeys.LANGUAGE, value);
    AppSettings.applyLanguage();
    Logger.event('切换语言：' + value);
  }

  /** 跟随系统时按系统语言选择最接近的字典。 */
  static resolvedLanguage(): Lang {
    const setting: LanguageSetting = AppSettings.language();
    if (setting !== 'system') {
      return setting;
    }
    try {
      const locale: string = i18n.System.getSystemLocale();
      const upper: string = locale.toUpperCase();
      if (upper.indexOf('HANT') >= 0 || upper.indexOf('-TW') >= 0 || upper.indexOf('-HK') >= 0 ||
        upper.indexOf('-MO') >= 0) {
        return 'zh-Hant';
      }
      if (locale.toLowerCase().indexOf('zh') === 0) {
        return 'zh-Hans';
      }
    } catch (err) {
      Logger.warn('读取系统语言失败');
    }
    return 'en';
  }

  static applyLanguage(): void {
    const resolved: Lang = AppSettings.resolvedLanguage();
    I18n.setLanguage(resolved);
    Logger.event('应用语言已切换为：' + resolved + '（当前存储值=' + AppSettings.language() + '）');
  }

  static applyColorMode(): void {
    const context: common.UIAbilityContext | null = AppSettings.appContext;
    if (context === null) {
      return;
    }
    const theme: ThemeMode = AppSettings.theme();
    let mode: ConfigurationConstant.ColorMode = ConfigurationConstant.ColorMode.COLOR_MODE_NOT_SET;
    if (theme === 'dark') {
      mode = ConfigurationConstant.ColorMode.COLOR_MODE_DARK;
    } else if (theme === 'light') {
      mode = ConfigurationConstant.ColorMode.COLOR_MODE_LIGHT;
    }
    try {
      context.getApplicationContext().setColorMode(mode);
    } catch (err) {
      Logger.warn('设置颜色模式失败');
    }
    AppSettings.refreshDarkFlag(context);
  }

  /** 把“是否暗色”写入 AppStorage，页面用 @StorageProp 订阅即可，无需各自读取配置。 */
  static refreshDarkFlag(context: common.UIAbilityContext): void {
    const theme: ThemeMode = AppSettings.theme();
    let dark: boolean = false;
    if (theme === 'dark') {
      dark = true;
    } else if (theme === 'light') {
      dark = false;
    } else {
      try {
        dark = context.config.colorMode === ConfigurationConstant.ColorMode.COLOR_MODE_DARK;
      } catch (err) {
        dark = false;
      }
    }
    AppStorage.setOrCreate<boolean>('isDark', dark);
  }

  static developer(): boolean {
    return KvStore.getBool(Keys.DEVELOPER, false);
  }

  static setDeveloper(value: boolean): void {
    KvStore.set(Keys.DEVELOPER, value);
    if (!value) {
      KvStore.set(Keys.MOCK_FAILURE, false);
    }
    AppStorage.setOrCreate<string>(StoreKeys.DATA_VERSION, Date.now().toString());
    Logger.event(value ? '开启开发者模式' : '关闭开发者模式');
  }

  static mockFailure(): boolean {
    return KvStore.getBool(Keys.MOCK_FAILURE, false);
  }

  static setMockFailure(value: boolean): void {
    KvStore.set(Keys.MOCK_FAILURE, value);
  }

  static accountHost(): string {
    return KvStore.getString(Keys.ACCOUNT_HOST, '');
  }

  static setAccountHost(value: string): void {
    KvStore.set(Keys.ACCOUNT_HOST, value);
  }

  static serverHost(): string {
    return KvStore.getString(Keys.SERVER_HOST, '');
  }

  static setServerHost(value: string): void {
    KvStore.set(Keys.SERVER_HOST, value);
  }

  static onboardingComplete(): boolean {
    return KvStore.getBool(Keys.ONBOARDING, false);
  }

  static setOnboardingComplete(value: boolean): void {
    KvStore.set(Keys.ONBOARDING, value);
  }

  static rememberedAccount(): string {
    return KvStore.getString(Keys.REMEMBERED_ACCOUNT, '');
  }

  static setRememberedAccount(value: string): void {
    KvStore.set(Keys.REMEMBERED_ACCOUNT, value);
  }
}
