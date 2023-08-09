package com.creepersan.memorialday.database.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.creepersan.memorialday.database.dao.MemorialDayDao
import com.creepersan.memorialday.database.model.MemorialDay

@Database(entities = [MemorialDay::class], version = MemorialDayDatabase.VERSION_001_ORIGINAL, exportSchema = false)
abstract class MemorialDayDatabase: RoomDatabase() {

    companion object {
        const val VERSION_001_ORIGINAL = 1

        const val DATABASE_NAME = "MemorialDay.db"
    }

    abstract fun memorialDayDao(): MemorialDayDao

}