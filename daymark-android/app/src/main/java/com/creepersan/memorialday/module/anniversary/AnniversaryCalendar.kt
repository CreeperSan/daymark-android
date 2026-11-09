package com.creepersan.memorialday.module.anniversary

import com.tyme.lunar.LunarDay
import com.tyme.lunar.LunarMonth
import com.tyme.lunar.LunarYear
import com.tyme.solar.SolarDay
import java.time.YearMonth

/** 使用 tyme4kt 校验并转换原始日期；负数农历月份表示闰月。 */
object AnniversaryCalendar {
    const val MIN_YEAR = 1
    const val MAX_YEAR = 9999
    const val MONTHS_PER_YEAR = 12
    private val regularMonths = (1..MONTHS_PER_YEAR).toList()

    /** 返回一年中按顺序出现的月份，农历闰月以负数表示并紧跟同名平月。 */
    fun monthsInYear(year: Int, lunar: Boolean): List<Int> =
        if (lunar) LunarYear.fromYear(year).getMonths().map { it.getMonthWithLeap() } else regularMonths

    /** 返回当月实际天数，兼顾公历闰年和农历平月、闰月。 */
    fun daysInMonth(year: Int, signedMonth: Int, lunar: Boolean): Int =
        if (lunar) LunarMonth.fromYm(year, signedMonth).getDayCount() else YearMonth.of(year, signedMonth).lengthOfMonth()

    /** 验证完整日期，非法日期和不存在的闰月均返回 false。 */
    fun isValid(year: Int, month: Int, day: Int, lunar: Boolean, leap: Boolean): Boolean {
        // 接口使用正数年月日，闰月由独立字段表示。
        if (year !in MIN_YEAR..MAX_YEAR || month !in 1..MONTHS_PER_YEAR || day < 1 || (!lunar && leap)) return false
        return try {
            if (lunar) LunarDay.fromYmd(year, if (leap) -month else month, day) else SolarDay.fromYmd(year, month, day)
            true
        } catch (_: Exception) { false }
    }

    /** 将有效日期转换为另一种日历，保留公历对应的同一天。 */
    fun convert(year: Int, month: Int, day: Int, fromLunar: Boolean, leap: Boolean): CalendarDate {
        if (fromLunar) {
            val solar = LunarDay.fromYmd(year, if (leap) -month else month, day).getSolarDay()
            return CalendarDate(solar.year, solar.month, solar.day, false)
        }
        val lunar = SolarDay.fromYmd(year, month, day).getLunarDay()
        return CalendarDate(lunar.year, kotlin.math.abs(lunar.month), lunar.day, lunar.month < 0)
    }
}

/** 供日期输入界面使用的日期字段。 */
data class CalendarDate(val year: Int, val month: Int, val day: Int, val leap: Boolean)
