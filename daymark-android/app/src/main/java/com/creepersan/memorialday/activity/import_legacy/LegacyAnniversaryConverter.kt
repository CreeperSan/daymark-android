package com.creepersan.memorialday.activity.import_legacy

import com.creepersan.memorialday.database.model.MemorialDay
import com.creepersan.memorialday.module.anniversary.Anniversary
import com.creepersan.memorialday.module.anniversary.AnniversaryCalendar
import java.time.Instant
import java.time.ZoneId

/** 将经典版保存的公历时间戳还原为新版所需的日历日期。 */
object LegacyAnniversaryConverter {
    fun convert(item: MemorialDay, zone: ZoneId = ZoneId.systemDefault()): Anniversary {
        val solar = Instant.ofEpochMilli(item.time).atZone(zone).toLocalDate()
        val lunar = item.timeType == MemorialDay.TIME_TYPE_LUNAR
        val date = if (lunar) AnniversaryCalendar.convert(solar.year, solar.monthValue, solar.dayOfMonth, false, false) else null
        return Anniversary(name = item.title, calendarType = if (lunar) Anniversary.CALENDAR_LUNAR else Anniversary.CALENDAR_SOLAR,
            loopType = if (item.loop == MemorialDay.LOOP_YEARLY) Anniversary.LOOP_YEARLY else Anniversary.LOOP_NONE,
            year = date?.year ?: solar.year, month = date?.month ?: solar.monthValue, day = date?.day ?: solar.dayOfMonth, isLeapMonth = if (date?.leap == true) 1 else 0)
    }
}
