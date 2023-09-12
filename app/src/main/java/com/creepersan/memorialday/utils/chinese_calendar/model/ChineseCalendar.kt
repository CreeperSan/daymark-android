package com.creepersan.memorialday.utils.chinese_calendar.model

/**
 * 农历信息
 * @property id
 * @property year 公历年份
 * @property month 公历月份
 * @property day 公历天
 * @property weekday 星期
 * @property lunarYear 农历年份
 * @property lunarMonth 农历月份
 * @property lunarDay 农历天
 * @property isLeapMonth 是否闰月
 * @property lunarYearName 农历年份名称
 * @property solarTerm 节气
 * @property zodiac 生肖
 */
data class ChineseCalendar(
    val id: Int,
    val year: Int,
    val month: Int,
    val day: Int,
    val weekday: Int,
    val lunarYear: Int,
    val lunarMonth: Int,
    val lunarDay: Int,
    val isLeapMonth: Boolean,
    val lunarYearName: String,
    val solarTerm: ChineseCalendarSolarTerm,
    val zodiac: ChineseCalendarZodiac,
)
