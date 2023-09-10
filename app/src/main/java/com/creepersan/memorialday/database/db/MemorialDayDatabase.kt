package com.creepersan.memorialday.database.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.creepersan.memorialday.database.dao.MemorialDayDao
import com.creepersan.memorialday.database.model.MemorialDay

@Database(
    exportSchema = false,
    version = MemorialDayDatabase.VERSION_002_CHINESE_CALENDAR,
    entities = [
        MemorialDay::class,
    ],
)
abstract class MemorialDayDatabase: RoomDatabase() {

    companion object {
        const val VERSION_001_ORIGINAL = 1              /* 初版数据库 */
        const val VERSION_002_CHINESE_CALENDAR = 2      /* 增加农历支持 */

        const val DATABASE_NAME = "MemorialDay.db"
    }

    abstract fun memorialDayDao(): MemorialDayDao

}