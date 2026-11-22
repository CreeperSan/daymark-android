import { Theme, Metrics } from '../Theme';
import { I18n } from '../../core/i18n/Strings';
import { CalendarKind } from '../../model/Anniversary';
import { LunarCalendar, LunarParts } from '../../calendar/LunarCalendar';
import { LunarLabels } from '../../calendar/LunarLabels';
import { DateUtils } from '../../common/DateUtils';
import { WheelColumn } from './WheelColumn';
import { Logger } from '../../core/Logger';

const MIN_YEAR: number = 1900;
const MAX_YEAR: number = 2100;
const ROW_HEIGHT: number = 40;
const WHEEL_HEIGHT: number = 200;

/**
 * 日期选择弹层：公历与农历共用三列滚轮，农历月份列表包含闰月（如“闰六月”）。
 *
 * 历法不在这里切换——由外部的分段控件决定后再打开弹层，
 * 弹层内再放一份切换控件会出现“两处都能改历法”的歧义，用户改完也分不清哪个生效。
 * 农历年份列显示干支与生肖，例如“甲辰龙年（2024）”。
 */
@Component
export struct DatePickerSheet {
  @Prop initialTime: number = 0;
  @Prop initialType: string = CalendarKind.SOLAR;
  @Prop isDark: boolean = false;
  onCancel: () => void = () => {
  };
  onConfirm: (time: number, type: string) => void = () => {
  };

  @State type: string = CalendarKind.SOLAR;
  @State year: number = 2026;
  @State signedMonth: number = 1;
  @State day: number = 1;
  @State years: string[] = [];
  @State signedMonths: number[] = [];
  @State monthItems: string[] = [];
  @State dayItems: string[] = [];
  @StorageProp('bottomInset') bottomInset: number = 0;
  @State yearIndex: number = 0;
  @State monthIndex: number = 0;
  @State dayIndex: number = 0;

  aboutToAppear(): void {
    this.type = this.initialType === CalendarKind.LUNAR ? CalendarKind.LUNAR : CalendarKind.SOLAR;
    const list: string[] = [];
    for (let y = MIN_YEAR; y <= MAX_YEAR; y++) {
      list.push(this.type === CalendarKind.LUNAR ? LunarLabels.yearLabel(y) : y.toString());
    }
    this.years = list;
    const date: Date = new Date(this.initialTime);
    if (this.type === CalendarKind.LUNAR) {
      const parts: LunarParts | null = LunarCalendar.parts(date);
      if (parts !== null) {
        this.year = parts.year;
        this.signedMonth = parts.leap ? -parts.month : parts.month;
        this.day = parts.day;
      } else {
        this.type = CalendarKind.SOLAR;
        this.readSolar(date);
      }
    } else {
      this.readSolar(date);
    }
    this.year = this.year < MIN_YEAR ? MIN_YEAR : (this.year > MAX_YEAR ? MAX_YEAR : this.year);
    this.rebuild();
    Logger.event('打开日期选择：' + this.type + ' ' + this.year.toString() + '年' +
      this.absMonth().toString() + '月' + this.day.toString() + '日' +
      (this.signedMonth < 0 ? '（闰月）' : ''));
  }

  private readSolar(date: Date): void {
    this.year = date.getFullYear();
    this.signedMonth = date.getMonth() + 1;
    this.day = date.getDate();
  }

  private absMonth(): number {
    return this.signedMonth < 0 ? -this.signedMonth : this.signedMonth;
  }

  private currentDate(): Date {
    if (this.type === CalendarKind.LUNAR) {
      const date: Date | null = LunarCalendar.solarDate(this.year, this.absMonth(), this.day, this.signedMonth < 0);
      return date !== null ? date : DateUtils.today();
    }
    const date: Date | null = DateUtils.solarDate(this.year, this.absMonth(), this.day);
    return date !== null ? date : DateUtils.today();
  }

  /** 重建月份与日期列表，并修正越界的选中项。 */
  private rebuild(): void {
    const months: number[] = [];
    const items: string[] = [];
    if (this.type === CalendarKind.LUNAR) {
      const signed: number[] = LunarCalendar.signedMonths(this.year);
      for (let i = 0; i < signed.length; i++) {
        months.push(signed[i]);
        items.push(LunarLabels.monthLabel(signed[i]));
      }
    } else {
      for (let m = 1; m <= 12; m++) {
        months.push(m);
        items.push(m.toString());
      }
    }
    if (months.length === 0) {
      months.push(1);
      items.push('1');
    }
    let index: number = months.indexOf(this.signedMonth);
    if (index < 0) {
      const normal: number = this.absMonth();
      const fallback: number = months.indexOf(normal);
      index = fallback >= 0 ? fallback : 0;
      this.signedMonth = months[index];
    }
    let count: number = 0;
    if (this.type === CalendarKind.LUNAR) {
      count = LunarCalendar.daysInMonth(this.year, this.absMonth(), this.signedMonth < 0);
    } else {
      count = DateUtils.daysInSolarMonth(this.year, this.absMonth());
    }
    if (count <= 0) {
      count = 30;
    }
    if (this.day > count) {
      this.day = count;
    }
    const days: string[] = [];
    for (let d = 1; d <= count; d++) {
      days.push(this.type === CalendarKind.LUNAR ? LunarLabels.day(d) : d.toString());
    }
    this.signedMonths = months;
    this.monthItems = items;
    this.dayItems = days;
    this.yearIndex = this.year - MIN_YEAR;
    this.monthIndex = index;
    this.dayIndex = this.day - 1;
  }

  build() {
    Column({ space: 8 }) {
      Row() {
        Text(I18n.t('cancel'))
          .fontSize(15)
          .fontColor(Theme.textSecondary(this.isDark))
          .padding({ top: 6, bottom: 6, right: 12 })
          .onClick(() => {
            this.onCancel();
          })
        Text(I18n.t('selectDate'))
          .fontSize(16)
          .fontWeight(FontWeight.Medium)
          .fontColor(Theme.textPrimary(this.isDark))
          .textAlign(TextAlign.Center)
          .layoutWeight(1)
        Text(I18n.t('confirm'))
          .fontSize(15)
          .fontColor(Theme.accent(this.isDark))
          .padding({ top: 6, bottom: 6, left: 12 })
          .onClick(() => {
            this.onConfirm(this.currentDate().getTime(), this.type);
          })
      }
      .width('100%')

      Stack() {
        Column()
          .width('100%')
          .height(ROW_HEIGHT)
          .backgroundColor(Theme.withAlpha(Theme.accent(this.isDark), 0.12))
          .borderRadius(10)
        Row() {
          // 农历年份标签较长（如“甲辰龙年（2024）”），年份列给更宽的权重
          WheelColumn({
            items: this.years,
            index: this.yearIndex,
            rowHeight: ROW_HEIGHT,
            viewHeight: WHEEL_HEIGHT,
            itemFontSize: this.type === CalendarKind.LUNAR ? 13 : 15,
            activeFontSize: this.type === CalendarKind.LUNAR ? 15 : 18,
            isDark: this.isDark,
            onSettle: (index: number) => {
              this.year = MIN_YEAR + index;
              this.rebuild();
            }
          }).layoutWeight(this.type === CalendarKind.LUNAR ? 1.9 : 1)
          WheelColumn({
            items: this.monthItems,
            index: this.monthIndex,
            rowHeight: ROW_HEIGHT,
            viewHeight: WHEEL_HEIGHT,
            isDark: this.isDark,
            onSettle: (index: number) => {
              this.signedMonth = this.signedMonths[index];
              this.rebuild();
            }
          }).layoutWeight(1)
          WheelColumn({
            items: this.dayItems,
            index: this.dayIndex,
            rowHeight: ROW_HEIGHT,
            viewHeight: WHEEL_HEIGHT,
            isDark: this.isDark,
            onSettle: (index: number) => {
              this.day = index + 1;
            }
          }).layoutWeight(1)
        }
        .width('100%')
      }
      .width('100%')
      .height(WHEEL_HEIGHT)
      .alignContent(Alignment.Center)
    }
    .width('100%')
    // 顶部留出拖动条（dragBar）占用的高度；底部让开手势条
    .padding({ left: 20, right: 20, top: 28, bottom: 16 + this.bottomInset })
  }
}

/** 弹层高度，供 bindSheet 使用。 */
export const DATE_SHEET_HEIGHT: number = 300;
