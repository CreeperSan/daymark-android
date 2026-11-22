import { hilog } from '@kit.PerformanceAnalysisKit';

const DOMAIN: number = 0x0D01;
const TAG: string = 'Daymark';

/**
 * 统一事件日志。只记录事件名称与错误类型，密码、令牌与用户输入不进入日志，
 * 与 iOS 的 DaymarkLog 保持同样的脱敏约束。
 */
export class Logger {
  static event(message: string): void {
    hilog.info(DOMAIN, TAG, '%{public}s', message);
  }

  static warn(message: string): void {
    hilog.warn(DOMAIN, TAG, '%{public}s', message);
  }

  static error(message: string): void {
    hilog.error(DOMAIN, TAG, '%{public}s', message);
  }

  /** 网络明细只在调试构建输出，且需要调用方先完成脱敏。 */
  static network(message: string): void {
    hilog.debug(DOMAIN, 'DaymarkNetwork', '%{public}s', message);
  }

  /** JSON 日志仅保留结构，敏感字段统一替换。 */
  static redact(json: string): string {
    try {
      const value: Object = JSON.parse(json) as Object;
      return JSON.stringify(Logger.redactValue(value));
    } catch (err) {
      return '[非 JSON 内容已省略]';
    }
  }

  private static redactValue(value: Object): Object {
    if (value === null || value === undefined) {
      return value;
    }
    if (Array.isArray(value)) {
      const list: Object[] = value as Object[];
      return list.map((item: Object) => Logger.redactValue(item));
    }
    if (typeof value === 'object') {
      const source: Record<string, Object> = value as Record<string, Object>;
      const result: Record<string, Object> = {};
      const keys: string[] = Object.keys(source);
      for (let i = 0; i < keys.length; i++) {
        const key: string = keys[i];
        const normalized: string = key.toLowerCase();
        const sensitive: boolean = normalized.indexOf('password') >= 0 || normalized.indexOf('token') >= 0 ||
          normalized.indexOf('email') >= 0 || normalized.indexOf('account') >= 0 ||
          normalized.indexOf('secret') >= 0 || normalized.indexOf('cookie') >= 0;
        result[key] = sensitive ? '[已隐藏]' : Logger.redactValue(source[key]);
      }
      return result;
    }
    return value;
  }
}
