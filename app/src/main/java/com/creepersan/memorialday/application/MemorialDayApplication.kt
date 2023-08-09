package com.creepersan.memorialday.application

import android.app.Application
import com.creepersan.memorialday.database.DatabaseManager

class MemorialDayApplication : Application() {

    companion object {
        lateinit var instance: MemorialDayApplication
    }

    lateinit var databaseManager: DatabaseManager

    override fun onCreate() {
        super.onCreate()

        instance = this

        initDatabase()
    }

    private fun initDatabase() {
        databaseManager = DatabaseManager()
    }

}


