import { StoreKeys } from '../service/AppSettings';

/** 轻提示。iOS 用居中胶囊 2.5 秒后消失，这里用 AppStorage 驱动根容器上的浮层。 */
export class Toast {
  private static timer: number = -1;

  static show(message: string): void {
    AppStorage.setOrCreate<string>(StoreKeys.TOAST, message);
    if (Toast.timer !== -1) {
      clearTimeout(Toast.timer);
    }
    Toast.timer = setTimeout(() => {
      if (AppStorage.get<string>(StoreKeys.TOAST) === message) {
        AppStorage.setOrCreate<string>(StoreKeys.TOAST, '');
      }
      Toast.timer = -1;
    }, 2600);
  }

  static clear(): void {
    AppStorage.setOrCreate<string>(StoreKeys.TOAST, '');
  }
}
