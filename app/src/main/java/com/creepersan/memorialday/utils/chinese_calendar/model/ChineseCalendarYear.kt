package com.creepersan.memorialday.utils.chinese_calendar.model


/**
 * 农历年份信息
 * @param year 年份
 * @param name 年份名称
 * @param zodiac 属相
 */
data class ChineseCalendarYear(
    val year: Int,
    val name: String,
    val zodiac: ChineseCalendarZodiac,
)


/**
 * 公历年份信息
 * @param year 年份
 * @param name 年份名称
 */
data class CalendarYear(
    val year: Int,
    val name: String,
)
