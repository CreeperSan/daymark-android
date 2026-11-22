/** 服务端纪念日字段，保持 snake_case 以便直接映射接口与本地缓存。 */
export interface AnniversaryDto {
  id?: number;
  name?: string;
  description?: string;
  calendar_type?: string;
  loop_type?: string;
  year?: number;
  month?: number;
  day?: number;
  is_leap_month?: number;
  next_timestamp?: number;
}

export class CalendarKind {
  static readonly SOLAR: string = 'solar';
  static readonly LUNAR: string = 'lunar';
}

export class RepeatKind {
  static readonly NONE: string = 'none';
  static readonly MONTHLY: string = 'monthly';
  static readonly YEARLY: string = 'yearly';
}

/**
 * 纪念日：日期字段保留用户选择的公历或农历原始值，
 * 与 Android / iOS 的服务端结构一致，切换平台不会丢闰月信息。
 */
export class Anniversary {
  id: number = 0;
  name: string = '';
  description: string = '';
  calendarType: string = CalendarKind.SOLAR;
  loopType: string = RepeatKind.YEARLY;
  year: number = 0;
  month: number = 0;
  day: number = 0;
  isLeapMonth: number = 0;
  nextTimestamp: number = 0;

  copy(): Anniversary {
    const result: Anniversary = new Anniversary();
    result.id = this.id;
    result.name = this.name;
    result.description = this.description;
    result.calendarType = this.calendarType;
    result.loopType = this.loopType;
    result.year = this.year;
    result.month = this.month;
    result.day = this.day;
    result.isLeapMonth = this.isLeapMonth;
    result.nextTimestamp = this.nextTimestamp;
    return result;
  }

  static fromDto(dto: AnniversaryDto): Anniversary {
    const result: Anniversary = new Anniversary();
    result.id = dto.id === undefined ? 0 : dto.id;
    result.name = dto.name === undefined ? '' : dto.name;
    result.description = dto.description === undefined ? '' : dto.description;
    result.calendarType = dto.calendar_type === undefined ? CalendarKind.SOLAR : dto.calendar_type;
    result.loopType = dto.loop_type === undefined ? RepeatKind.YEARLY : dto.loop_type;
    result.year = dto.year === undefined ? 0 : dto.year;
    result.month = dto.month === undefined ? 0 : dto.month;
    result.day = dto.day === undefined ? 0 : dto.day;
    result.isLeapMonth = dto.is_leap_month === undefined ? 0 : dto.is_leap_month;
    result.nextTimestamp = dto.next_timestamp === undefined ? 0 : dto.next_timestamp;
    return result;
  }

  toDto(): AnniversaryDto {
    const dto: AnniversaryDto = {
      id: this.id,
      name: this.name,
      description: this.description,
      calendar_type: this.calendarType,
      loop_type: this.loopType,
      year: this.year,
      month: this.month,
      day: this.day,
      is_leap_month: this.isLeapMonth,
      next_timestamp: this.nextTimestamp
    };
    return dto;
  }
}
