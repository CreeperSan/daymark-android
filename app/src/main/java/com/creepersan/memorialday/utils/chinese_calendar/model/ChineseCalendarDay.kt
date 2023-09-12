package com.creepersan.memorialday.utils.chinese_calendar.model

data class ChineseCalendarDay (
    val year: Int,
    val month: Int,
    val isLeapMonth: Boolean,
    val day: Int,
    val name: String,
    val solarTerm: ChineseCalendarSolarTerm?,
)