package com.creepersan.memorialday.database

import androidx.room.Room
import com.creepersan.memorialday.application.MemorialDayApplication
import com.creepersan.memorialday.database.db.MemorialDayDatabase

class DatabaseManager {

    val memorialDatabase by lazy {
        Room.databaseBuilder(
            MemorialDayApplication.instance,
            MemorialDayDatabase::class.java,
            MemorialDayDatabase.DATABASE_NAME
        ).build()
    }


}