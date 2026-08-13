package com.creepersan.memorialday.database

import androidx.room.Room
import com.creepersan.memorialday.application.DaymarkApplication
import com.creepersan.memorialday.database.db.MemorialDayDatabase
import com.creepersan.memorialday.database.migration.MemorialDatabaseMigration_1_2
import com.creepersan.memorialday.database.migration.MemorialDatabaseMigration_2_3

class DatabaseManager {

    val memorialDatabase by lazy {
        Room.databaseBuilder(
            DaymarkApplication.instance,
            MemorialDayDatabase::class.java,
            MemorialDayDatabase.DATABASE_NAME
        ).addMigrations(
            MemorialDatabaseMigration_1_2(),
            MemorialDatabaseMigration_2_3(),
        ).build()
    }


}