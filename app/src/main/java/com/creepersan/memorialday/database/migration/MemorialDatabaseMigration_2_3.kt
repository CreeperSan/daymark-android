package com.creepersan.memorialday.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 原版数据库升级增加农历支持
 * 1. 版本2增加的农历字段 废弃，但是先留在数据库中先不删除
 * 2. 增加日期类型字段（用于区分农历和公历）
 */
class MemorialDatabaseMigration_2_3: Migration(2, 3) {

    override fun migrate(database: SupportSQLiteDatabase) {
        // 增加日期类型字段（用于区分农历和公历）
        database.execSQL("ALTER TABLE MemorialDay ADD COLUMN time_type INTEGER NOT NULL DEFAULT 0")
    }

}
