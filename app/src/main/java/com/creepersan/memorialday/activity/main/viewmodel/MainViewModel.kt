package com.creepersan.memorialday.activity.main.viewmodel

import android.icu.util.Calendar
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.creepersan.memorialday.activity.main.model.MemorialDayMainModel
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

    val memorialDayListChannel = Channel<List<MemorialDayMainModel>>()
    val uiChannel = Channel<MainUIEvent>()

    private var loadDataDisposable: Disposable? = null

    @Suppress("KotlinConstantConditions")
    fun loadData() {
        loadDataDisposable = Observable.just(Unit).observeOn(MemorialDayScheduler.database).map {
            MemorialDayApplication.instance.databaseManager.memorialDatabase.memorialDayDao()
        }.map {  dao ->
            dao.getAll()
        }.map { rawMemorialDayList ->
            val todayList = ArrayList<MemorialDayMainModel>()
            val comingList = ArrayList<MemorialDayMainModel>()
            val passedList = ArrayList<MemorialDayMainModel>()
            val unknownList = ArrayList<MemorialDayMainModel>()
            for (memorialDay in rawMemorialDayList) {
                val memorialDayMainModel = MemorialDayMainModel.fromMemorialDay(memorialDay)
                when {
                    memorialDayMainModel.dayLeft == MemorialDayMainModel.DAY_LEFT_UNKNOWN -> {
                        unknownList.add(memorialDayMainModel)
                    }
                    memorialDayMainModel.dayLeft == 0 -> {
                        todayList.add(memorialDayMainModel)
                    }
                    memorialDayMainModel.dayLeft > 0 -> {
                        comingList.add(memorialDayMainModel)
                    }
                    memorialDayMainModel.dayLeft < 0 -> {
                        passedList.add(memorialDayMainModel)
                    }
                }
            }
            todayList + comingList + passedList + unknownList
        }.observeOn(MemorialDayScheduler.ui).subscribe({ memorialDayList ->
            viewModelScope.launch { memorialDayListChannel.send(memorialDayList) }
        }, { error ->
            error.printStackTrace()
        }, {
            viewModelScope.launch { uiChannel.send(MainUIEvent.LoadDataFinish) }
        })
    }


}