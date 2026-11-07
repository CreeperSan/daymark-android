package com.creepersan.memorialday.activity.main.model

import android.util.Log
import com.creepersan.memorialday.database.model.MemorialDay
import com.creepersan.memorialday.module.CompareResult
import com.creepersan.memorialday.module.CompareUtils
import com.creepersan.memorialday.module.FormatUtils
import com.creepersan.memorialday.module.chinese_calendar.ChineseCalendarUtils
import com.creepersan.memorialday.module.chinese_calendar.definition.NumberEqual
import com.creepersan.memorialday.module.chinese_calendar.definition.NumberLessThenOrEqual
import com.creepersan.memorialday.module.chinese_calendar.definition.Sort
import com.creepersan.memorialday.module.chinese_calendar.model.ChineseCalendar
import com.creepersan.memorialday.module.chinese_calendar.model.getFullGregorianDayString
import com.creepersan.memorialday.module.chinese_calendar.model.getFullLunarDayString
import io.reactivex.rxjava3.core.Observable
import java.lang.Exception
import java.util.Calendar
import java.util.Date

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
                val observableToday = if (todayChineseCalendar == null) {
                    val todayJavaCalendar = Calendar.getInstance()
                    todayJavaCalendar.time = Date(System.currentTimeMillis())
                    val todayYear = todayJavaCalendar.get(Calendar.YEAR)
                    val todayMonth = todayJavaCalendar.get(Calendar.MONTH) + 1
                    val todayDay = todayJavaCalendar.get(Calendar.DAY_OF_MONTH)
                    ChineseCalendarUtils.getFromCalendar(todayYear, todayMonth, todayDay)
                        .map { it.get() }
                        .doOnError { e -> Log.e(TAG, "获取今日农历日期失败"); e.printStackTrace() }
                } else {
                    Observable.just(todayChineseCalendar)
                }

                val observableMemorialDay = Observable.just(Unit).concatMap {
                    val memorialDayJavaCalendar = Calendar.getInstance()
                    memorialDayJavaCalendar.time = Date(memorialDay.time)
                    val memorialDayYear = memorialDayJavaCalendar.get(Calendar.YEAR)
                    val memorialDayMonth = memorialDayJavaCalendar.get(Calendar.MONTH) + 1
                    val memorialDayDay = memorialDayJavaCalendar.get(Calendar.DAY_OF_MONTH)
                    ChineseCalendarUtils.getFromCalendar(memorialDayYear, memorialDayMonth, memorialDayDay)
                }.map { it.get() }
                    .doOnError { e -> Log.e(TAG, "获取纪念日农历日期失败"); e.printStackTrace() }

                return@concatMap Observable.zip(observableToday, observableMemorialDay) { todayCalendar, memorialDayCalendar ->
                    return@zip listOf(todayCalendar, memorialDayCalendar)
                }
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
                        return@concatMap Observable.just(Unit).concatMap {
                            val compareResult = CompareUtils.compareCalendar(
                                month1 = todayCalendar.month,
                                day1 = todayCalendar.day,
                                month2 = memorialCalendar.month,
                                day2 = memorialCalendar.day,
                            )
                            return@concatMap when (compareResult) {
                                CompareResult.Equal,
                                CompareResult.Less -> {
                                    /* 本年的还没有过，那么就是今年的 */
                                    ChineseCalendarUtils.getChineseCalendarWithParams(
                                        year = NumberEqual(todayCalendar.year),
                                        month = NumberLessThenOrEqual(memorialCalendar.month),
                                        day = NumberLessThenOrEqual(memorialCalendar.day),
                                        sort = Sort.Desc,
                                        limit = 3,
                                    )
                                }
                                CompareResult.Greater -> {
                                    /* 今年的已经过了，那么使用下一年的 */
                                    ChineseCalendarUtils.getChineseCalendarWithParams(
                                        year = NumberEqual(todayCalendar.year + 1),
                                        month = NumberLessThenOrEqual(memorialCalendar.month),
                                        day = NumberLessThenOrEqual(memorialCalendar.day),
                                        sort = Sort.Desc,
                                        limit = 3,
                                    )
                                }
                                else -> throw Exception("获取目标事件发生异常，无法判断是否超过当天")
                            }
                        }.map { it.first() }.map { targetCalendar ->
                            return@map MemorialDayMainModel(
                                memorialDay = memorialDay,
                                title = memorialDay.title,
                                time = FormatUtils.timestampToYYYYMMDD(memorialDay.time),
                                dayLeft = targetCalendar.id - todayCalendar.id,
                            )
                        }.doOnError { error ->
                            error.printStackTrace()
                        }
                    }
                    /* 【农历】每年 */
                    memorialDay.loop == MemorialDay.LOOP_YEARLY && memorialDay.timeType == MemorialDay.TIME_TYPE_LUNAR -> {
                        Observable.just(Unit).concatMap {
                            val compareResult = CompareUtils.compareChineseCalendar(
                                month1 = todayCalendar.lunarMonth,
                                day1 = todayCalendar.lunarDay,
//                                isLeapMonth1 = todayCalendar.isLeapMonth, // 闰月不用比较，因为通常不会在闰月的时候过节
                                month2 = memorialCalendar.lunarMonth,
                                day2 = memorialCalendar.lunarDay,
//                                isLeapMonth2 = memorialCalendar.isLeapMonth, // 闰月不用比较，因为通常不会在闰月的时候过节
                            )
                            return@concatMap when(compareResult) {
                                CompareResult.Equal,
                                CompareResult.Less -> {
                                    /* 本年的还没有过，那么就是今年的 */
                                    ChineseCalendarUtils.getChineseCalendarWithParams(
                                        lunarYear = NumberEqual(todayCalendar.lunarYear),
                                        lunarMonth = NumberLessThenOrEqual(memorialCalendar.lunarMonth),
                                        lunarDay = NumberLessThenOrEqual(memorialCalendar.lunarDay),
                                        isLeapMonth = false,
                                        sort = Sort.Desc,
                                        limit = 3,
                                    )
                                }
                                CompareResult.Greater -> {
                                    /* 今年的已经过了，那么使用下一年的 */
                                    ChineseCalendarUtils.getChineseCalendarWithParams(
                                        lunarYear = NumberEqual(todayCalendar.lunarYear + 1),
                                        lunarMonth = NumberLessThenOrEqual(memorialCalendar.lunarMonth),
                                        lunarDay = NumberLessThenOrEqual(memorialCalendar.lunarDay),
                                        isLeapMonth = false,
                                        sort = Sort.Desc,
                                        limit = 3,
                                    )
                                }
                                else -> throw Exception("获取目标事件发生异常，无法判断是否超过当天")
                            }
                        }.map { it.first() }.map { targetCalendar ->
                            return@map MemorialDayMainModel(
                                memorialDay = memorialDay,
                                title = memorialDay.title,
                                time = memorialCalendar.getFullLunarDayString(),
                                dayLeft = targetCalendar.id - todayCalendar.id,
                            )
                        }.doOnError { error ->
                            error.printStackTrace()
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