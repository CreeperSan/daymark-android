package com.creepersan.memorialday.module.log

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

object Logger {

    fun initialize(): Flow<Unit> {
        return flow { emit(Unit) }
    }

    fun info(tag: String, message: String) {
        Log.i(tag, message)
    }

    fun warning(tag: String, message: String) {
        Log.w(tag, message)
    }

    fun error(tag: String, message: String) {
        Log.e(tag, message)
    }

}