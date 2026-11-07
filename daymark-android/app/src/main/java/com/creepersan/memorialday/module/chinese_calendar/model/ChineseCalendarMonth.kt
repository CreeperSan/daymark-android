package com.creepersan.memorialday.module.chinese_calendar.model


/**
 * 农历月份信息
 * @param year 农历年份
 * @param month 农历月份
 * @param name 农历月份名称
 * @param isLeap 是否为闰月
 */
data class ChineseCalendarMonth(
    val year: Int,
    val month: Int,
    val name: String,
    val isLeap: Boolean,
)

/**
 * 公历月份信息
 * @param year 年份
 * @param month 月份
 * @param name 月份名称
 */
data class CalendarMonth(
    val year: Int,
    val month: Int,
    val name: String,
)


