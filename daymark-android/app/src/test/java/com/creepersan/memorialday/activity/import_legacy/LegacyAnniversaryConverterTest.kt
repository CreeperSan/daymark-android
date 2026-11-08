package com.creepersan.memorialday.activity.import_legacy

import com.creepersan.memorialday.database.model.MemorialDay
import com.creepersan.memorialday.module.anniversary.Anniversary
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** 经典公历和农历时间戳导入时应保留日期及重复方式。 */
class LegacyAnniversaryConverterTest {
    private val zone = ZoneId.of("Asia/Shanghai")

    private fun item(date: LocalDate, type: Int, loop: Int) = MemorialDay(1, "测试", date.atStartOfDay(zone).toInstant().toEpochMilli(), timeType = type, loop = loop)

    @Test fun solarDateIsPreserved() {
        val result = LegacyAnniversaryConverter.convert(item(LocalDate.of(2023, 5, 20), MemorialDay.TIME_TYPE_GREGORIAN, MemorialDay.LOOP_ONCE), zone)
        assertEquals(Anniversary.CALENDAR_SOLAR, result.calendarType)
        assertEquals(Anniversary.LOOP_NONE, result.loopType)
        assertEquals(listOf(2023, 5, 20), listOf(result.year, result.month, result.day))
    }

    @Test fun lunarLeapMonthIsPreserved() {
        val result = LegacyAnniversaryConverter.convert(item(LocalDate.of(2023, 3, 22), MemorialDay.TIME_TYPE_LUNAR, MemorialDay.LOOP_YEARLY), zone)
        assertEquals(Anniversary.CALENDAR_LUNAR, result.calendarType)
        assertEquals(Anniversary.LOOP_YEARLY, result.loopType)
        assertEquals(listOf(2023, 2, 1, 1), listOf(result.year, result.month, result.day, result.isLeapMonth))
    }
}
