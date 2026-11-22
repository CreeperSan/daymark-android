import { Anniversary, CalendarKind, RepeatKind } from '../model/Anniversary';
import { DateUtils } from '../common/DateUtils';
import { LunarCalendar, LunarMonthEntry, LunarParts } from './LunarCalendar';
import { LunarLabels } from './LunarLabels';
import { I18n } from '../core/i18n/Strings';

/**
 * 纪念日日期换算与倒计时。逻辑与 Android 的 AnniversaryCountdown、iOS 的
 * AnniversaryCalendar 对齐：优先采用服务端的下一次发生时间，过期的循环日期在本地推算。
 */
export class AnniversaryCalendar {
  /** 原始日期：农历按闰月信息换算成公历，未知历法返回 null。 */
  static originalDate(item: Anniversary): Date | null {
    if (item.calendarType === CalendarKind.LUNAR) {
      return LunarCalendar.solarDate(item.year, item.month, item.day, item.isLeapMonth === 1);
    }
    if (item.calendarType === CalendarKind.SOLAR) {
      return DateUtils.solarDate(item.year, item.month, item.day);
    }
    return null;
  }

  /** 距离今天的天数；无法确定日期时返回 null。 */
  static daysUntil(item: Anniversary, today: Date): number | null {
    const base: Date = DateUtils.startOfDay(today);
    let serverDate: Date | null = null;
    if (item.nextTimestamp !== 0) {
      const parsed: Date = new Date(item.nextTimestamp);
      if (!isNaN(parsed.getTime())) {
        serverDate = DateUtils.startOfDay(parsed);
      }
    }
    let target: Date | null = null;
    if (item.loopType === RepeatKind.NONE) {
      target = serverDate !== null ? serverDate : AnniversaryCalendar.originalDate(item);
    } else if (item.loopType === RepeatKind.YEARLY || item.loopType === RepeatKind.MONTHLY) {
      let samePeriod: boolean = false;
      if (serverDate !== null) {
        const sameYear: boolean = serverDate.getFullYear() === base.getFullYear();
        samePeriod = item.loopType === RepeatKind.YEARLY ? sameYear :
          sameYear && serverDate.getMonth() === base.getMonth();
      }
      if (serverDate !== null && (samePeriod || serverDate.getTime() >= base.getTime())) {
        target = serverDate;
      } else {
        const local: Date | null = AnniversaryCalendar.nextLocalDate(item, base);
        target = local !== null ? local : serverDate;
      }
    } else {
      target = serverDate;
    }
    if (target === null) {
      return null;
    }
    return DateUtils.daysBetween(base, target);
  }

  /** 服务端日期缺失或已跨过周期时，推算今天及以后的首次发生日。 */
  static nextLocalDate(item: Anniversary, today: Date): Date | null {
    const base: Date = DateUtils.startOfDay(today);
    const origin: Date | null = AnniversaryCalendar.originalDate(item);
    if (origin === null) {
      return null;
    }
    if (origin.getTime() >= base.getTime()) {
      return origin;
    }
    if (item.calendarType === CalendarKind.SOLAR) {
      return AnniversaryCalendar.nextSolarDate(item, base);
    }
    if (item.calendarType === CalendarKind.LUNAR) {
      return AnniversaryCalendar.nextLunarDate(item, base);
    }
    return null;
  }

  private static nextSolarDate(item: Anniversary, base: Date): Date | null {
    const monthly: boolean = item.loopType === RepeatKind.MONTHLY;
    const month: number = monthly ? base.getMonth() + 1 : item.month;
    const start: Date = new Date(base.getFullYear(), month - 1, 1);
    for (let offset = 0; offset <= 1; offset++) {
      const period: Date = monthly ? DateUtils.addMonths(start, offset) : new Date(start.getFullYear() + offset, start.getMonth(), 1);
      const count: number = DateUtils.daysInSolarMonth(period.getFullYear(), period.getMonth() + 1);
      const day: number = item.day > count ? count : item.day;
      const date: Date = new Date(period.getFullYear(), period.getMonth(), day);
      if (date.getTime() >= base.getTime()) {
        return date;
      }
    }
    return null;
  }

  private static nextLunarDate(item: Anniversary, base: Date): Date | null {
    if (item.loopType === RepeatKind.MONTHLY) {
      return AnniversaryCalendar.nextLunarMonthDate(item, base);
    }
    const todayParts: LunarParts | null = LunarCalendar.parts(base);
    if (todayParts === null) {
      return null;
    }
    for (let offset = 0; offset <= 1; offset++) {
      const targetYear: number = todayParts.year + offset;
      const leap: boolean = item.isLeapMonth === 1 && LunarCalendar.hasLeapMonth(targetYear, item.month);
      let date: Date | null = LunarCalendar.solarDate(targetYear, item.month, item.day, leap);
      if (date === null) {
        // 农历月只有 29 或 30 天，第 30 天不存在时取月末。
        const fallbackDay: number = item.day > 29 ? 29 : item.day;
        date = LunarCalendar.solarDate(targetYear, item.month, fallbackDay, leap);
      }
      if (date !== null && date.getTime() >= base.getTime()) {
        return date;
      }
    }
    return null;
  }

  /** 农历每月循环：从当前农历月开始，闰月也参与循环。 */
  private static nextLunarMonthDate(item: Anniversary, base: Date): Date | null {
    const todayParts: LunarParts | null = LunarCalendar.parts(base);
    if (todayParts === null) {
      return null;
    }
    const candidates: LunarMonthEntry[] = [];
    const current: LunarMonthEntry[] = LunarCalendar.months(todayParts.year);
    let started: boolean = false;
    for (let i = 0; i < current.length; i++) {
      const entry: LunarMonthEntry = current[i];
      if (!started) {
        const first: Date = new Date(entry.firstYear, entry.firstMonth - 1, entry.firstDay);
        const last: Date = new Date(entry.firstYear, entry.firstMonth - 1, entry.firstDay + entry.dayCount - 1);
        started = base.getTime() >= first.getTime() && base.getTime() <= last.getTime();
      }
      if (started) {
        candidates.push(entry);
      }
    }
    const nextYear: LunarMonthEntry[] = LunarCalendar.months(todayParts.year + 1);
    for (let i = 0; i < nextYear.length && candidates.length < current.length + 2; i++) {
      candidates.push(nextYear[i]);
    }
    for (let i = 0; i < candidates.length; i++) {
      const entry: LunarMonthEntry = candidates[i];
      const day: number = item.day > entry.dayCount ? entry.dayCount : item.day;
      const date: Date = new Date(entry.firstYear, entry.firstMonth - 1, entry.firstDay + day - 1);
      if (date.getTime() >= base.getTime()) {
        return date;
      }
    }
    return null;
  }

  /** 首页与编辑页共用的日期描述。 */
  static dateText(item: Anniversary, original: Date | null): string {
    if (original === null) {
      return I18n.t('dateUnavailable');
    }
    const text: string = DateUtils.formatDot(original);
    if (item.calendarType === CalendarKind.LUNAR) {
      const parts: LunarParts | null = LunarCalendar.parts(original);
      if (parts === null) {
        return text;
      }
      return text + ' · ' + I18n.t('lunarPrefix') + ' ' + LunarLabels.fullDate(parts.month, parts.day, parts.leap);
    }
    return text + ' · ' + DateUtils.weekday(original);
  }

  /** 编辑页的日期行：公历显示选择值，农历同时显示对应的农历写法。 */
  static selectedDateText(date: Date, calendarType: string): string {
    const text: string = DateUtils.formatDot(date);
    if (calendarType === CalendarKind.LUNAR) {
      const parts: LunarParts | null = LunarCalendar.parts(date);
      if (parts === null) {
        return text;
      }
      return text + ' · ' + I18n.t('lunarPrefix') + ' ' + LunarLabels.fullDate(parts.month, parts.day, parts.leap);
    }
    return text;
  }
}
