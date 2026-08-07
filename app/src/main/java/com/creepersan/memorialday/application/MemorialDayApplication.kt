package com.creepersan.memorialday.application

import android.app.Application
import com.creepersan.memorialday.database.DatabaseManager
import com.creepersan.memorialday.module.storage.kv.KVStorageManager

class MemorialDayApplication : Application() {

    companion object {
        lateinit var instance: MemorialDayApplication
    }

    lateinit var databaseManager: DatabaseManager

    override fun onCreate() {
        super.onCreate()

        instance = this

        // 默认注册全部 KV 后端，业务定义通过 KVType 选择具体实现。
        KVStorageManager.initialize(this)
        initDatabase()
    }

    private fun initDatabase() {
        databaseManager = DatabaseManager()
    }

}
