package com.creepersan.memorialday.utils.chinese_calendar

import io.reactivex.rxjava3.core.Observable

object BaseCalendarUtils {

    fun getAvailableYear(): Observable<List<Int>> {
        return Observable.just(IntRange(1901, 2100).toList())
    }

    fun getAvailableMonth(): Observable<List<Int>> {
        return Observable.just(IntRange(1, 12).toList())
    }


}