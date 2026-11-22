import { I18n } from '../core/i18n/Strings';

/** 农历月份与日期名称；非中文环境退回数字，避免硬编码中文。 */
export class LunarLabels {
  private static readonly MONTHS: string[] = ['正月', '二月', '三月', '四月', '五月', '六月', '七月', '八月', '九月', '十月', '冬月', '腊月'];
  private static readonly DIGITS: string[] = ['', '一', '二', '三', '四', '五', '六', '七', '八', '九'];
  private static readonly STEMS: string[] = ['甲', '乙', '丙', '丁', '戊', '己', '庚', '辛', '壬', '癸'];
  private static readonly BRANCHES: string[] = ['子', '丑', '寅', '卯', '辰', '巳', '午', '未', '申', '酉', '戌', '亥'];
  private static readonly ZODIACS: string[] = ['鼠', '牛', '虎', '兔', '龙', '蛇', '马', '羊', '猴', '鸡', '狗', '猪'];
  private static readonly ZODIACS_HANT: string[] = ['鼠', '牛', '虎', '兔', '龍', '蛇', '馬', '羊', '猴', '雞', '狗', '豬'];

  static month(value: number): string {
    if (!I18n.isChinese()) {
      return value.toString();
    }
    return value >= 1 && value <= 12 ? LunarLabels.MONTHS[value - 1] : value.toString();
  }

  static day(value: number): string {
    if (!I18n.isChinese()) {
      return value.toString();
    }
    if (value === 10) {
      return '初十';
    }
    if (value === 20) {
      return '二十';
    }
    if (value === 30) {
      return '三十';
    }
    if (value >= 1 && value <= 9) {
      return '初' + LunarLabels.DIGITS[value];
    }
    if (value >= 11 && value <= 19) {
      return '十' + LunarLabels.DIGITS[value - 10];
    }
    if (value >= 21 && value <= 29) {
      return '廿' + LunarLabels.DIGITS[value - 20];
    }
    return value.toString();
  }

  /** 滚轮月份标签：闰月加上前缀，例如“闰六月”。 */
  static monthLabel(signedMonth: number): string {
    const value: number = signedMonth < 0 ? -signedMonth : signedMonth;
    const name: string = LunarLabels.month(value);
    return signedMonth < 0 ? I18n.t('leapMonthPrefix') + name : name;
  }

  /**
   * 农历年份标签，例如“甲辰龙年（2024）”。
   * 干支以公历 4 年为甲子起点，因此偏移量按 (year - 4) 取模；
   * 非中文环境直接给公历年，避免出现无法阅读的汉字。
   */
  static yearLabel(year: number): string {
    if (!I18n.isChinese()) {
      return year.toString();
    }
    const offset: number = ((year - 4) % 60 + 60) % 60;
    const zodiacs: string[] = I18n.language() === 'zh-Hant' ? LunarLabels.ZODIACS_HANT : LunarLabels.ZODIACS;
    return LunarLabels.STEMS[offset % 10] + LunarLabels.BRANCHES[offset % 12] +
      zodiacs[offset % 12] + '年（' + year.toString() + '）';
  }

  /** 农历完整写法，例如“闰六月初八”。 */
  static fullDate(month: number, day: number, leap: boolean): string {
    const prefix: string = leap ? I18n.t('leapMonthPrefix') : '';
    if (!I18n.isChinese()) {
      return prefix + month.toString() + '/' + day.toString();
    }
    return prefix + LunarLabels.month(month) + LunarLabels.day(day);
  }
}
