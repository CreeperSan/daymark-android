package com.creepersan.memorialday.activity.main.viewmodel

import android.icu.util.Calendar
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.creepersan.memorialday.application.MemorialDayApplication
import com.creepersan.memorialday.constance.MemorialDayScheduler
import com.creepersan.memorialday.database.model.MemorialDay
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


sealed class MainUIEvent {
    object Idle: MainUIEvent()
    object LoadDataFinish: MainUIEvent()
}


class MainViewModel: ViewModel() {

    val memorialDayListChannel = Channel<List<MemorialDay>>()
    val uiChannel = Channel<MainUIEvent>()

    private var loadDataDisposable: Disposable? = null

    fun loadData() {
        loadDataDisposable = Observable.just(Unit).observeOn(MemorialDayScheduler.database).map {
            MemorialDayApplication.instance.databaseManager.memorialDatabase.memorialDayDao()
        }.map {  dao ->
            dao.getAll()
        }.map { rawMemorialDayList ->
            val currentCalendar = Calendar.getInstance()
            currentCalendar.set(Calendar.HOUR_OF_DAY, 0)
            currentCalendar.set(Calendar.MINUTE, 0)
            currentCalendar.set(Calendar.SECOND, 0)
            currentCalendar.set(Calendar.MILLISECOND, 0)
            val currentTimeStamp = currentCalendar.time.time
            val commingList = ArrayList<MemorialDay>()
            val expiredList = ArrayList<MemorialDay>()
            for (tmpMemorialDay in rawMemorialDayList) {
                if (tmpMemorialDay.time >= currentTimeStamp) {
                    commingList.add(tmpMemorialDay)
                } else {
                    expiredList.add(tmpMemorialDay)
                }
            }
            commingList.sortBy { it.time }
            expiredList.sortBy { it.time }
            commingList + expiredList
        }.observeOn(MemorialDayScheduler.ui).subscribe({ memorialDayList ->
            viewModelScope.launch { memorialDayListChannel.send(memorialDayList) }
        }, { error ->
            error.printStackTrace()
        }, {
            viewModelScope.launch { uiChannel.send(MainUIEvent.LoadDataFinish) }
        })
    }


}