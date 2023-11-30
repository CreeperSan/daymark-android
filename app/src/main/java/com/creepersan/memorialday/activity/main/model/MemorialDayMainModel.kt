package com.creepersan.memorialday.activity.main.model

import android.util.Log
import com.creepersan.memorialday.database.model.MemorialDay
import com.creepersan.memorialday.utils.FormatUtils
import com.creepersan.memorialday.utils.chinese_calendar.ChineseCalendarUtils
import com.creepersan.memorialday.utils.chinese_calendar.model.ChineseCalendar
import com.creepersan.memorialday.utils.chinese_calendar.model.getFullGregorianDayString
import com.creepersan.memorialday.utils.chinese_calendar.model.getFullLunarDayString
import io.reactivex.rxjava3.core.Observable
import java.lang.Exception
import java.util.Calendar
import java.util.Date
import java.util.Optional

data class MemorialDayMainModel(
    val memorialDay: MemorialDay,
    val title: String,
    val time: String,
    val dayLeft: Int,
) {

    companion object {

        const val TAG = "MemorialDayMainModel"

        const val DAY_LEFT_UNKNOWN = Int.MIN_VALUE


        /**
         * 数据库的纪念日数据转换
         * @param memorialDay 纪念日数据
         * @param todayChineseCalendar 今天的农历数据库数据，可以为空，为空时会自动获取
         */
        fun fromMemorialDayObservable(
            memorialDay: MemorialDay,
            todayChineseCalendar: ChineseCalendar? = null
        ): Observable<MemorialDayMainModel> {

            return Observable.just(Unit).concatMap {
                val tmpCalendar = Calendar.getInstance()
                val todayYear = tmpCalendar.get(Calendar.YEAR)
                val todayMonth = tmpCalendar.get(Calendar.MONTH) + 1
                val todayDay = tmpCalendar.get(Calendar.DAY_OF_MONTH)
                tmpCalendar.time = Date(memorialDay.time)
                val memorialDayYear = tmpCalendar.get(Calendar.YEAR)
                val memorialDayMonth = tmpCalendar.get(Calendar.MONTH) + 1
                val memorialDayDay = tmpCalendar.get(Calendar.DAY_OF_MONTH)

                val observableToday = if (todayChineseCalendar == null) {
                    ChineseCalendarUtils.getFromCalendar(todayYear, todayMonth, todayDay)
                        .map { it.get() }
                        .doOnError { e -> Log.e(TAG, "获取今日农历日期失败"); e.printStackTrace() }
                } else {
                    Observable.just(todayChineseCalendar)
                }

                val observableMemorialDay = ChineseCalendarUtils.getFromCalendar(memorialDayYear, memorialDayMonth, memorialDayDay)
                    .map { it.get() }
                    .doOnError { e -> Log.e(TAG, "获取纪念日农历日期失败"); e.printStackTrace() }

                return@concatMap Observable.merge(listOf(
                    observableToday, observableMemorialDay
                )).toList().toObservable()
            }.concatMap { calendarList ->
                val todayCalendar = calendarList[0]
                val memorialCalendar = calendarList[1]

                return@concatMap when {
                    /* 【公历】单次 */
                    memorialDay.loop == MemorialDay.LOOP_ONCE && memorialDay.timeType == MemorialDay.TIME_TYPE_GREGORIAN -> {
                        Observable.just(MemorialDayMainModel(
                            memorialDay = memorialDay,
                            title = memorialDay.title,
                            time = memorialCalendar.getFullGregorianDayString(),
                            dayLeft = memorialCalendar.id - todayCalendar.id,
                        ))
                    }
                    /* 【农历】单次 */
                    memorialDay.loop == MemorialDay.LOOP_ONCE && memorialDay.timeType == MemorialDay.TIME_TYPE_LUNAR -> {
                        Observable.just(MemorialDayMainModel(
                            memorialDay = memorialDay,
                            title = memorialDay.title,
                            time = memorialCalendar.getFullLunarDayString(),
                            dayLeft = memorialCalendar.id - todayCalendar.id,
                        ))
                    }
                    /* 【公历】每年 */
                    memorialDay.loop == MemorialDay.LOOP_YEARLY && memorialDay.timeType == MemorialDay.TIME_TYPE_GREGORIAN -> {
                        ChineseCalendarUtils.getFromCalendar(
                            memorialCalendar.year + 1, memorialCalendar.month, memorialCalendar.day
                        ).map { it.get() }.map { targetCalendar ->
                            MemorialDayMainModel(
                                memorialDay = memorialDay,
                                title = memorialDay.title,
                                time = FormatUtils.timestampToYYYYMMDD(memorialDay.time),
                                dayLeft = memorialCalendar.id - todayCalendar.id,
                            )
                        }
                    }
                    /* 【农历】每年 */
                    memorialDay.loop == MemorialDay.LOOP_YEARLY && memorialDay.timeType == MemorialDay.TIME_TYPE_LUNAR -> {
                        Observable.just(Unit).concatMap {
                            ChineseCalendarUtils.getChineseCalendarWithParams(
                                lunarYear = memorialCalendar.year + 1,
                                lunarMonth = memorialCalendar.month,
                                lunarDay = memorialCalendar.day,
                            )
                        }.map { resultList ->
                            val targetCalendar = resultList.firstOrNull()
                            if (targetCalendar == null) {
                                throw Exception("获取下一个周期事件失败")
                            }
                            // FIXME 还需要处理闰月的问题
                            MemorialDayMainModel(
                                memorialDay = memorialDay,
                                title = memorialDay.title,
                                time = memorialCalendar.getFullLunarDayString(),
                                dayLeft = targetCalendar.id - memorialCalendar.id,
                            )
                        }
                    }
                    else -> throw Exception("预料之外的获取下一个周期事件")
                }
            }.onErrorReturn { error ->
                error.printStackTrace()
                MemorialDayMainModel(
                    memorialDay = memorialDay,
                    title = memorialDay.title,
                    time = "计算失败，请升级应用或联系开发者",
                    dayLeft = DAY_LEFT_UNKNOWN,
                )
            }
        }


    }

}