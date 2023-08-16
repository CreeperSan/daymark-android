package com.creepersan.memorialday.activity.main.model

import com.creepersan.memorialday.database.model.MemorialDay
import com.creepersan.memorialday.utils.FormatUtils
import java.util.Calendar
import java.util.Date

data class MemorialDayMainModel(
    val memorialDay: MemorialDay,
    val title: String,
    val time: String,
    val dayLeft: Int,
) {

    companion object {

        const val DAY_LEFT_UNKNOWN = Int.MIN_VALUE

        fun fromMemorialDay(memorialDay: MemorialDay): MemorialDayMainModel {
            val titleString = memorialDay.title
            val timeDisplay = FormatUtils.timestampToYYYYMMDD(memorialDay.time)
            var dayLeft = 0

            val currentCalendar = Calendar.getInstance()
            currentCalendar.set(Calendar.HOUR_OF_DAY, 0)
            currentCalendar.set(Calendar.MINUTE, 0)
            currentCalendar.set(Calendar.SECOND, 0)
            currentCalendar.set(Calendar.MILLISECOND, 0)

            val memorialDayCalendar = Calendar.getInstance()
            memorialDayCalendar.time = Date(memorialDay.time)
            memorialDayCalendar.set(Calendar.HOUR_OF_DAY, 0)
            memorialDayCalendar.set(Calendar.MINUTE, 0)
            memorialDayCalendar.set(Calendar.SECOND, 0)
            memorialDayCalendar.set(Calendar.MILLISECOND, 0)

            when (memorialDay.loop) {
                MemorialDay.LOOP_ONCE -> {
                    val dayBetween = (memorialDayCalendar.time.time - currentCalendar.time.time) / (1000 * 60 * 60 * 24)
                    dayLeft = dayBetween.toInt()
                }
                MemorialDay.LOOP_YEARLY -> {
                    memorialDayCalendar.set(Calendar.YEAR, currentCalendar.get(Calendar.YEAR))
                    val dayBetween = (memorialDayCalendar.time.time - currentCalendar.time.time) / (1000 * 60 * 60 * 24)
                    dayLeft = dayBetween.toInt()
                }
                else -> {
                    dayLeft = DAY_LEFT_UNKNOWN
                }
            }

            return MemorialDayMainModel(
                memorialDay = memorialDay,
                title = titleString,
                time = timeDisplay,
                dayLeft = dayLeft,
            )
        }

    }

}