package com.creepersan.memorialday.module.anniversary

import com.tyme.lunar.LunarDay
import com.tyme.solar.SolarDay

/** 使用 tyme4kt 校验并转换原始日期；负数农历月份表示闰月。 */
object AnniversaryCalendar {
    /** 验证完整日期，非法日期和不存在的闰月均返回 false。 */
    fun isValid(year: Int, month: Int, day: Int, lunar: Boolean, leap: Boolean): Boolean {
        // 接口使用正数年月日，闰月由独立字段表示。
        if (year !in 1..9999 || month !in 1..12 || day < 1 || (!lunar && leap)) return false
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
