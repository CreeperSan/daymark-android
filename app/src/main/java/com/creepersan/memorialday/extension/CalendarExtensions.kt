package com.creepersan.memorialday.extension

import com.creepersan.memorialday.utils.chinese_calendar.model.ChineseCalendar
import java.util.Calendar


fun Calendar.setTime(
    year: Int? = null,
    month: Int? = null,
    day: Int? = null,
    hour: Int? = null,
    minute: Int? = null,
    second: Int? = null,
    millisecond: Int? = null,
): Calendar{
    if (year != null) set(Calendar.YEAR, year)
    if (month != null) set(Calendar.MONTH, month)
    if (day != null) set(Calendar.DAY_OF_MONTH, day)
    if (hour != null) set(Calendar.HOUR_OF_DAY, hour)
    if (minute != null) set(Calendar.MINUTE, minute)
    if (second != null) set(Calendar.SECOND, second)
    if (millisecond != null) set(Calendar.MILLISECOND, millisecond)
    return this
}

