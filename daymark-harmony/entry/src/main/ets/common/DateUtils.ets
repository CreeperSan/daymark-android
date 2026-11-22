import { I18n } from '../core/i18n/Strings';

/** 本地日历工具。所有计算按设备本地时区的自然日进行，避免夏令时造成天数偏差。 */
export class DateUtils {
  static readonly DAY_MS: number = 86400000;

  static startOfDay(date: Date): Date {
    return new Date(date.getFullYear(), date.getMonth(), date.getDate());
  }

  static today(): Date {
    return DateUtils.startOfDay(new Date());
  }

  /** 以 UTC 归一化的天序号做差，跨时区与时制变化都不会产生 ±1 天误差。 */
  static dayNumber(date: Date): number {
    return Math.round(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()) / DateUtils.DAY_MS);
  }

  static daysBetween(from: Date, to: Date): number {
    return DateUtils.dayNumber(to) - DateUtils.dayNumber(from);
  }

  static addDays(date: Date, days: number): Date {
    return new Date(date.getFullYear(), date.getMonth(), date.getDate() + days);
  }

  static addMonths(date: Date, months: number): Date {
    return new Date(date.getFullYear(), date.getMonth() + months, date.getDate());
  }

  static sameDay(a: Date, b: Date): boolean {
    return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
  }

  /** 构造并校验公历日期，非法日期（如 2 月 30 日）返回 null。 */
  static solarDate(year: number, month: number, day: number): Date | null {
    if (year < 1 || month < 1 || month > 12 || day < 1 || day > 31) {
      return null;
    }
    const date: Date = new Date(year, month - 1, day);
    if (date.getFullYear() !== year || date.getMonth() !== month - 1 || date.getDate() !== day) {
      return null;
    }
    return date;
  }

  static daysInSolarMonth(year: number, month: number): number {
    return new Date(year, month, 0).getDate();
  }

  static pad(value: number): string {
    return value < 10 ? '0' + value.toString() : value.toString();
  }

  /** 设计稿使用的 yyyy.MM.dd 格式。 */
  static formatDot(date: Date): string {
    return date.getFullYear().toString() + '.' + DateUtils.pad(date.getMonth() + 1) + '.' + DateUtils.pad(date.getDate());
  }

  static weekday(date: Date): string {
    return I18n.weekdayShort(date.getDay());
  }

  /** 带上午/下午的 ISO 时间，供日志使用。 */
  static timestamp(date: Date): string {
    return DateUtils.formatDot(date) + ' ' + DateUtils.pad(date.getHours()) + ':' + DateUtils.pad(date.getMinutes()) +
      ':' + DateUtils.pad(date.getSeconds());
  }
}
