package com.creepersan.memorialday.module.anniversary

import com.tyme.lunar.LunarDay
import com.tyme.lunar.LunarMonth
import com.tyme.lunar.LunarYear
import com.tyme.solar.SolarDay
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** 纪念日的日期差和列表排序。 */
object AnniversaryCountdown {
    /** 按本地日历日期计算天数；无法取得有效日期时返回 null。 */
    fun daysUntil(item: Anniversary, today: LocalDate, zone: ZoneId): Long? {
        val serverDate = try {
            // 服务端使用 0 表示没有下一次发生时间，负数时间戳仍可能是有效日期。
            if (item.nextTimestamp == 0L) null else Instant.ofEpochMilli(item.nextTimestamp).atZone(zone).toLocalDate()
        } catch (_: Exception) { null }

        val target = when (item.loopType) {
            Anniversary.LOOP_NONE -> serverDate ?: originalDate(item)
            Anniversary.LOOP_YEARLY, Anniversary.LOOP_MONTHLY -> {
                val samePeriod = serverDate != null && serverDate.year == today.year &&
                    (item.loopType == Anniversary.LOOP_YEARLY || serverDate.monthValue == today.monthValue)
                // 当期日期即使已经过去也保留服务端结果；未来日期同样优先采用服务端结果。
                if (serverDate != null && (samePeriod || !serverDate.isBefore(today))) serverDate
                else nextLocalDate(item, today) ?: serverDate
            }
            else -> serverDate
        } ?: return null
        return ChronoUnit.DAYS.between(today, target)
    }

    /** 将原始公历或农历日期转换为公历日期。 */
    private fun originalDate(item: Anniversary): LocalDate? {
        return try {
            if (item.calendarType == Anniversary.CALENDAR_LUNAR) {
                val solar = AnniversaryCalendar.convert(item.year, item.month, item.day, fromLunar = true, leap = item.isLeapMonth == 1)
                LocalDate.of(solar.year, solar.month, solar.day)
            } else if (item.calendarType == Anniversary.CALENDAR_SOLAR) {
                LocalDate.of(item.year, item.month, item.day)
            } else null
        } catch (_: Exception) { null }
    }

    /** 服务端日期已跨过周期或缺失时，从原始日期推算今天及以后的首次发生日。 */
    private fun nextLocalDate(item: Anniversary, today: LocalDate): LocalDate? {
        val origin = originalDate(item) ?: return null
        if (!origin.isBefore(today)) return origin
        return try {
            if (item.calendarType == Anniversary.CALENDAR_SOLAR) {
                val month = if (item.loopType == Anniversary.LOOP_MONTHLY) YearMonth.from(today) else YearMonth.of(today.year, item.month)
                val current = month.atDay(item.day.coerceAtMost(month.lengthOfMonth()))
                if (!current.isBefore(today)) current else {
                    val next = if (item.loopType == Anniversary.LOOP_MONTHLY) month.plusMonths(1) else month.plusYears(1)
                    next.atDay(item.day.coerceAtMost(next.lengthOfMonth()))
                }
            } else if (item.calendarType == Anniversary.CALENDAR_LUNAR) {
                val currentLunar = SolarDay.fromYmd(today.year, today.monthValue, today.dayOfMonth).getLunarDay()
                // 农历月循环包含闰月；年循环在目标年无原闰月时回退到对应普通月。
                (0..1).firstNotNullOfOrNull { offset ->
                    val month = if (item.loopType == Anniversary.LOOP_MONTHLY) {
                        currentLunar.getLunarMonth().next(offset)
                    } else {
                        val year = currentLunar.year + offset
                        val leap = item.isLeapMonth == 1 && LunarYear.fromYear(year).getLeapMonth() == item.month
                        LunarMonth.fromYm(year, if (leap) -item.month else item.month)
                    }
                    val lunar = LunarDay.fromYmd(month.year, month.getMonthWithLeap(), item.day.coerceAtMost(month.getDayCount()))
                    val solar = lunar.getSolarDay()
                    LocalDate.of(solar.year, solar.month, solar.day).takeUnless { it.isBefore(today) }
                }
            } else null
        } catch (_: Exception) { null }
    }

    /** 今天、未来、已过期、日期未知依次排列；同类按距离今天的天数升序。 */
    fun sort(items: List<Anniversary>, today: LocalDate, zone: ZoneId): List<Pair<Anniversary, Long?>> {
        return items.map { it to daysUntil(it, today, zone) }.sortedWith(compareBy(
            { (_, days) -> when { days == null -> 3; days == 0L -> 0; days > 0 -> 1; else -> 2 } },
            { (_, days) -> when { days == null -> 0L; days < 0 -> -days; else -> days } }
        ))
    }
}
