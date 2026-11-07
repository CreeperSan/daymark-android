package com.creepersan.memorialday.module.anniversary

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** 验证日期差、闰月及列表顺序。 */
class AnniversaryCountdownTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val today = LocalDate.of(2026, 11, 8)

    /** 创建不循环的公历纪念日。 */
    private fun solar(id: Long, date: LocalDate) = Anniversary(id = id, year = date.year, month = date.monthValue, day = date.dayOfMonth, loopType = Anniversary.LOOP_NONE)

    @Test fun sortsTodayThenUpcomingThenPastAndUnknown() {
        val entries = listOf(
            solar(1, today.minusDays(8)), solar(2, today.plusDays(5)), solar(3, today),
            solar(4, today.minusDays(1)), solar(5, today.plusDays(1)),
            Anniversary(id = 6, loopType = Anniversary.LOOP_YEARLY, nextTimestamp = 0)
        )
        val result = AnniversaryCountdown.sort(entries, today, zone)
        assertEquals(listOf(3L, 5L, 2L, 4L, 1L, 6L), result.map { it.first.id })
        assertEquals(listOf(0L, 1L, 5L, -1L, -8L, null), result.map { it.second })
    }

    @Test fun convertsLunarLeapMonthForNonRepeatingDate() {
        val item = Anniversary(year = 2023, month = 2, day = 1, isLeapMonth = 1, calendarType = Anniversary.CALENDAR_LUNAR, loopType = Anniversary.LOOP_NONE)
        assertEquals(1L, AnniversaryCountdown.daysUntil(item, LocalDate.of(2023, 3, 21), zone))
        assertEquals(0L, AnniversaryCountdown.daysUntil(item, LocalDate.of(2023, 3, 22), zone))
        assertEquals(-1L, AnniversaryCountdown.daysUntil(item, LocalDate.of(2023, 3, 23), zone))
    }

    @Test fun recurringTimestampUsesLocalDateAcrossDaylightSaving() {
        val newYork = ZoneId.of("America/New_York")
        val target = LocalDate.of(2024, 3, 11).atStartOfDay(newYork).toInstant().toEpochMilli()
        val item = Anniversary(loopType = Anniversary.LOOP_YEARLY, nextTimestamp = target)
        assertEquals(1L, AnniversaryCountdown.daysUntil(item, LocalDate.of(2024, 3, 10), newYork))
        assertNull(AnniversaryCountdown.daysUntil(item.copy(nextTimestamp = 0), today, zone))
    }

    @Test fun supportsPastDatesBeforeUnixEpoch() {
        assertEquals(-1L, AnniversaryCountdown.daysUntil(solar(1, LocalDate.of(1969, 12, 31)), LocalDate.of(1970, 1, 1), zone))
    }
    /** 将指定日期构造成服务端返回的毫秒时间戳。 */
    private fun timestamp(date: LocalDate): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()

    @Test fun yearlyUsesCurrentPeriodServerDateAndCalculatesExpiredPeriod() {
        val item = Anniversary(year = 2000, month = 12, day = 1, loopType = Anniversary.LOOP_YEARLY)
        val lastYear = item.copy(nextTimestamp = timestamp(LocalDate.of(2025, 12, 1)))
        assertEquals(23L, AnniversaryCountdown.daysUntil(lastYear, today, zone))
        val currentYear = item.copy(nextTimestamp = timestamp(LocalDate.of(2026, 10, 1)))
        assertEquals(-38L, AnniversaryCountdown.daysUntil(currentYear, today, zone))
        // 即使属于下一周期，尚未到达的服务端结果仍优先使用。
        val future = item.copy(nextTimestamp = timestamp(LocalDate.of(2027, 2, 1)))
        assertEquals(85L, AnniversaryCountdown.daysUntil(future, today, zone))
    }

    @Test fun monthlyUsesYearAndMonthAndClampsEndOfMonth() {
        val current = LocalDate.of(2026, 2, 20)
        val item = Anniversary(year = 2020, month = 1, day = 31, loopType = Anniversary.LOOP_MONTHLY)
        assertEquals(8L, AnniversaryCountdown.daysUntil(item.copy(nextTimestamp = timestamp(LocalDate.of(2025, 2, 28))), current, zone))
        assertEquals(-5L, AnniversaryCountdown.daysUntil(item.copy(nextTimestamp = timestamp(LocalDate.of(2026, 2, 15))), current, zone))
        assertEquals(28L, AnniversaryCountdown.daysUntil(item.copy(nextTimestamp = 0), LocalDate.of(2026, 3, 3), zone))
    }

    @Test fun lunarYearlyFallsBackToNormalMonthWhenLeapMonthAbsent() {
        val item = Anniversary(year = 2023, month = 2, day = 1, isLeapMonth = 1, calendarType = Anniversary.CALENDAR_LUNAR, loopType = Anniversary.LOOP_YEARLY, nextTimestamp = timestamp(LocalDate.of(2023, 3, 22)))
        // 2024 年没有闰二月，农历二月初一为公历 3 月 10 日。
        assertEquals(1L, AnniversaryCountdown.daysUntil(item, LocalDate.of(2024, 3, 9), zone))
    }

    @Test fun lunarMonthlyIncludesLeapMonthAndKeepsOriginalDay() {
        val item = Anniversary(year = 2023, month = 2, day = 1, calendarType = Anniversary.CALENDAR_LUNAR, loopType = Anniversary.LOOP_MONTHLY, nextTimestamp = timestamp(LocalDate.of(2023, 2, 20)))
        // 2023 年二月之后是闰二月，其初一为 3 月 22 日。
        assertEquals(1L, AnniversaryCountdown.daysUntil(item, LocalDate.of(2023, 3, 21), zone))
    }

    @Test fun nonRecurringPrefersServerAndFallbackDoesNotPrecedeOrigin() {
        val nonRepeating = solar(1, LocalDate.of(2026, 1, 1)).copy(nextTimestamp = timestamp(LocalDate.of(2026, 11, 9)))
        assertEquals(1L, AnniversaryCountdown.daysUntil(nonRepeating, today, zone))
        val futureOrigin = Anniversary(year = 2028, month = 2, day = 29, loopType = Anniversary.LOOP_YEARLY, nextTimestamp = timestamp(LocalDate.of(2025, 1, 1)))
        assertEquals(java.time.temporal.ChronoUnit.DAYS.between(today, LocalDate.of(2028, 2, 29)), AnniversaryCountdown.daysUntil(futureOrigin, today, zone))
    }

}
