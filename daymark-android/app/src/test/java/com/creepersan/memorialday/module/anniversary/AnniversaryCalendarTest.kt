package com.creepersan.memorialday.module.anniversary

import org.junit.Assert.*
import org.junit.Test

/** 验证用户输入的公历、农历和闰月转换。 */
class AnniversaryCalendarTest {
    @Test fun lunarLeapMonthRoundTrips() {
        // 2023 年闰二月初一对应公历 3 月 22 日。
        assertTrue(AnniversaryCalendar.isValid(2023, 2, 1, lunar = true, leap = true))
        assertEquals(CalendarDate(2023, 3, 22, false), AnniversaryCalendar.convert(2023, 2, 1, fromLunar = true, leap = true))
        assertEquals(CalendarDate(2023, 2, 1, true), AnniversaryCalendar.convert(2023, 3, 22, fromLunar = false, leap = false))
        assertNotEquals(AnniversaryCalendar.convert(2023, 2, 1, true, false), AnniversaryCalendar.convert(2023, 2, 1, true, true))
    }

    @Test fun rejectsInvalidLeapMonthsAndDates() {
        assertFalse(AnniversaryCalendar.isValid(2023, 3, 1, lunar = true, leap = true))
        assertFalse(AnniversaryCalendar.isValid(2024, 2, 1, lunar = true, leap = true))
        assertFalse(AnniversaryCalendar.isValid(2023, 2, 30, lunar = true, leap = true))
        assertFalse(AnniversaryCalendar.isValid(2023, -2, 1, lunar = true, leap = false))
        assertTrue(AnniversaryCalendar.isValid(2024, 2, 29, lunar = false, leap = false))
        assertFalse(AnniversaryCalendar.isValid(2023, 2, 29, lunar = false, leap = false))
        assertFalse(AnniversaryCalendar.isValid(2024, 2, 29, lunar = false, leap = true))
    }

    @Test fun pickerMonthsFollowTheYearAndKeepLeapMonthBesideRegularMonth() {
        assertEquals(listOf(1, 2, -2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12), AnniversaryCalendar.monthsInYear(2023, lunar = true))
        assertEquals((1..12).toList(), AnniversaryCalendar.monthsInYear(2024, lunar = true))
        assertEquals(listOf(5, 6, -6, 7), AnniversaryCalendar.monthsInYear(2025, lunar = true).subList(4, 8))
    }

    @Test fun pickerDaysMatchLeapYearsAndLunarLeapMonths() {
        assertEquals(28, AnniversaryCalendar.daysInMonth(2023, 2, lunar = false))
        assertEquals(29, AnniversaryCalendar.daysInMonth(2024, 2, lunar = false))
        assertEquals(30, AnniversaryCalendar.daysInMonth(2023, 2, lunar = true))
        assertEquals(29, AnniversaryCalendar.daysInMonth(2023, -2, lunar = true))
    }
}
