import { i18n } from '@kit.LocalizationKit';
import { Logger } from '../core/Logger';

/** 农历日期：year 为农历年（以正月初一所在公历年计），leap 表示闰月。 */
export class LunarParts {
  year: number = 0;
  month: number = 0;
  day: number = 0;
  leap: boolean = false;
}

/** 农历年中的一个月份，signedMonth 为负数时表示闰月。 */
export class LunarMonthEntry {
  signedMonth: number = 0;
  firstYear: number = 0;
  firstMonth: number = 0;
  firstDay: number = 0;
  dayCount: number = 0;
}

/**
 * 农历换算。优先使用 HarmonyOS 内置的中国农历（i18n.Calendar 的 chinese 历法），
 * 原生能力只需要“公历 → 农历”这一个方向，反向换算用农历年表搜索完成，
 * 这样不必依赖 API 26 才提供的 ChineseCalendar，也不会引入第三方日历库。
 */
export class LunarCalendar {
  static readonly MIN_YEAR: number = 1900;
  static readonly MAX_YEAR: number = 2100;
  private static calendar: i18n.Calendar | null = null;
  private static partCache: Map<string, LunarParts> = new Map<string, LunarParts>();
  private static yearCache: Map<number, LunarMonthEntry[]> = new Map<number, LunarMonthEntry[]>();
  private static available: boolean = true;

  private static instance(): i18n.Calendar | null {
    if (!LunarCalendar.available) {
      return null;
    }
    if (LunarCalendar.calendar === null) {
      try {
        LunarCalendar.calendar = i18n.getCalendar('zh-CN', 'chinese');
      } catch (err) {
        LunarCalendar.available = false;
        Logger.error('内置农历不可用');
        return null;
      }
    }
    return LunarCalendar.calendar;
  }

  /** 内置农历返回的 year 是 1-60 的干支序号，需要按公历年还原成农历年。 */
  private static cyclicalIndex(year: number): number {
    const value: number = (year - 4) % 60;
    return (value < 0 ? value + 60 : value) + 1;
  }

  /** 已知干支序号落在公历年与上一年之间，用公历年反查得到真实农历年。 */
  private static resolveLunarYear(cyclical: number, solarYear: number): number {
    if (LunarCalendar.cyclicalIndex(solarYear) === cyclical) {
      return solarYear;
    }
    if (LunarCalendar.cyclicalIndex(solarYear - 1) === cyclical) {
      return solarYear - 1;
    }
    return solarYear;
  }

  /** 公历 → 农历。取不到时返回 null，调用方按“日期不可用”处理。 */
  static parts(date: Date): LunarParts | null {
    const key: string = date.getFullYear().toString() + '-' + date.getMonth().toString() + '-' + date.getDate().toString();
    const cached: LunarParts | undefined = LunarCalendar.partCache.get(key);
    if (cached !== undefined) {
      return cached;
    }
    const calendar: i18n.Calendar | null = LunarCalendar.instance();
    if (calendar === null) {
      return null;
    }
    try {
      calendar.setTime(date.getTime());
      const result: LunarParts = new LunarParts();
      result.year = LunarCalendar.resolveLunarYear(calendar.get('year'), date.getFullYear());
      result.month = calendar.get('month') + 1;
      result.day = calendar.get('date');
      result.leap = calendar.get('is_leap_month') === 1;
      if (result.year <= 0 || result.month <= 0 || result.day <= 0) {
        return null;
      }
      LunarCalendar.partCache.set(key, result);
      return result;
    } catch (err) {
      Logger.warn('农历换算失败');
      return null;
    }
  }

  /**
   * 农历年表：一次扫描该农历年覆盖的公历区间，得到按顺序排列的月份、
   * 每月起始公历日与天数。后续查询均为 O(1)，列表渲染不会反复扫描。
   */
  static months(year: number): LunarMonthEntry[] {
    const cached: LunarMonthEntry[] | undefined = LunarCalendar.yearCache.get(year);
    if (cached !== undefined) {
      return cached;
    }
    const result: LunarMonthEntry[] = [];
    const end: number = new Date(year + 1, 2, 1).getTime();
    for (let offset = 0; offset < 460; offset++) {
      const date: Date = new Date(year, 0, 1 + offset);
      if (date.getTime() >= end) {
        break;
      }
      const parts: LunarParts | null = LunarCalendar.parts(date);
      if (parts === null || parts.year !== year) {
        continue;
      }
      const signed: number = parts.leap ? -parts.month : parts.month;
      let entry: LunarMonthEntry | null = null;
      for (let i = 0; i < result.length; i++) {
        if (result[i].signedMonth === signed) {
          entry = result[i];
          break;
        }
      }
      if (entry === null) {
        entry = new LunarMonthEntry();
        entry.signedMonth = signed;
        entry.firstYear = date.getFullYear();
        entry.firstMonth = date.getMonth() + 1;
        entry.firstDay = date.getDate();
        result.push(entry);
      }
      entry.dayCount = entry.dayCount + 1;
    }
    LunarCalendar.yearCache.set(year, result);
    return result;
  }

  /** 农历月列表，闰月以负数表示并紧跟同名平月，与 Android 接口保持一致。 */
  static signedMonths(year: number): number[] {
    const entries: LunarMonthEntry[] = LunarCalendar.months(year);
    const result: number[] = [];
    for (let i = 0; i < entries.length; i++) {
      result.push(entries[i].signedMonth);
    }
    return result;
  }

  static daysInMonth(year: number, month: number, leap: boolean): number {
    const signed: number = leap ? -month : month;
    const entries: LunarMonthEntry[] = LunarCalendar.months(year);
    for (let i = 0; i < entries.length; i++) {
      if (entries[i].signedMonth === signed) {
        return entries[i].dayCount;
      }
    }
    return 0;
  }

  /** 农历 → 公历。闰月必须与选择一致，不存在的闰月返回 null。 */
  static solarDate(year: number, month: number, day: number, leap: boolean): Date | null {
    if (year < LunarCalendar.MIN_YEAR || year > LunarCalendar.MAX_YEAR || month < 1 || month > 12 || day < 1) {
      return null;
    }
    const signed: number = leap ? -month : month;
    const entries: LunarMonthEntry[] = LunarCalendar.months(year);
    for (let i = 0; i < entries.length; i++) {
      const entry: LunarMonthEntry = entries[i];
      if (entry.signedMonth !== signed || day > entry.dayCount) {
        continue;
      }
      const result: Date = new Date(entry.firstYear, entry.firstMonth - 1, entry.firstDay + day - 1);
      return result;
    }
    return null;
  }

  /** 某农历年是否存在闰月，用于年份切换时回退到同一普通月。 */
  static hasLeapMonth(year: number, month: number): boolean {
    return LunarCalendar.daysInMonth(year, month, true) > 0;
  }

  static availableNow(): boolean {
    return LunarCalendar.instance() !== null;
  }
}
