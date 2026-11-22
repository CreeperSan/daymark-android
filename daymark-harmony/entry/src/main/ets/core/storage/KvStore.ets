import { preferences } from '@kit.ArkData';
import { common } from '@kit.AbilityKit';
import { Logger } from '../Logger';

const STORE_NAME: string = 'daymark';

/**
 * 键值对存储。封装 HarmonyOS preferences，对业务层只暴露类型化读写，
 * 上层不关心底层是 preferences 还是以后的数据库。
 */
export class KvStore {
  private static store: preferences.Preferences | null = null;

  static init(context: common.Context): void {
    if (KvStore.store !== null) {
      return;
    }
    const options: preferences.Options = { name: STORE_NAME };
    KvStore.store = preferences.getPreferencesSync(context, options);
    Logger.event('键值存储已就绪：' + STORE_NAME);
  }

  private static ready(): preferences.Preferences {
    if (KvStore.store === null) {
      // 正常流程不会走到这里：EntryAbility 启动时已完成初始化
      Logger.error('键值存储尚未初始化，读写会被拒绝');
      throw new Error('KvStore 尚未初始化');
    }
    return KvStore.store;
  }

  static getString(key: string, fallback: string = ''): string {
    const value: preferences.ValueType = KvStore.ready().getSync(key, fallback);
    return typeof value === 'string' ? value : fallback;
  }

  static getBool(key: string, fallback: boolean = false): boolean {
    const value: preferences.ValueType = KvStore.ready().getSync(key, fallback);
    return typeof value === 'boolean' ? value : fallback;
  }

  static getInt(key: string, fallback: number = 0): number {
    const value: preferences.ValueType = KvStore.ready().getSync(key, fallback);
    return typeof value === 'number' ? value : fallback;
  }

  static set(key: string, value: string | number | boolean): void {
    const store: preferences.Preferences = KvStore.ready();
    store.putSync(key, value);
    store.flushSync();
  }

  static has(key: string): boolean {
    return KvStore.ready().hasSync(key);
  }

  static remove(key: string): void {
    const store: preferences.Preferences = KvStore.ready();
    store.deleteSync(key);
    store.flushSync();
  }
}
