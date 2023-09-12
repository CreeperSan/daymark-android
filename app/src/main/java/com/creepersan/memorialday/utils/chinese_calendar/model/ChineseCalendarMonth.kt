package com.creepersan.memorialday.utils.chinese_calendar.model


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
