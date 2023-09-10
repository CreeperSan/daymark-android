package com.creepersan.memorialday.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 原版数据库升级增加农历支持
 * 1. 增加农历日历字段，类型为 TEXT，储存格式为：1901-05-23
 */
class MemorialDatabaseMigration_1_2: Migration(1, 2) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE MemorialDay ADD COLUMN lunar_time TEXT NOT NULL DEFAULT ''")
    }

}