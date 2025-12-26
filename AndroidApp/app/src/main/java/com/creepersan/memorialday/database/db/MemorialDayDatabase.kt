package com.creepersan.memorialday.database.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.creepersan.memorialday.database.dao.MemorialDayDao
import com.creepersan.memorialday.database.model.MemorialDay

@Database(
    exportSchema = false,
    version = MemorialDayDatabase.VERSION_003_DATE_TYPE,
    entities = [
        MemorialDay::class,
    ],
)
abstract class MemorialDayDatabase: RoomDatabase() {

    companion object {
        const val VERSION_001_ORIGINAL = 1              /* 初版数据库 */
        const val VERSION_002_CHINESE_CALENDAR = 2      /* 增加农历支持 */
        const val VERSION_003_DATE_TYPE = 3             /* 调整农历字段实现 */

        const val DATABASE_NAME = "MemorialDay.db"
    }

    abstract fun memorialDayDao(): MemorialDayDao

}