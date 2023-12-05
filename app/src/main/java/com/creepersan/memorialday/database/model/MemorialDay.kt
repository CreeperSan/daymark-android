package com.creepersan.memorialday.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class MemorialDay (
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "time") val time: Long,                      // 储存的公历日期时间戳
    @Deprecated("字段已废弃")
    @ColumnInfo(name = "lunar_time") val lunarTime: String = "",    // 已废弃，为空字符串
    @ColumnInfo(name = "time_type") val timeType: Int,              // 时间类型
    @ColumnInfo(name = "loop") val loop: Int,                       // 循环方式
) {

    companion object {

        const val LOOP_ONCE = 1             // 循环方式 - 单次
        const val LOOP_YEARLY = 2           // 循环方式 - 每年

        const val TIME_TYPE_GREGORIAN = 0   // 历法 - 公历
        const val TIME_TYPE_LUNAR = 1       // 历法 - 农历

        fun empty(): MemorialDay = MemorialDay(
            id = 0,
            title = "",
            time = System.currentTimeMillis(),
            lunarTime = "",
            timeType = TIME_TYPE_GREGORIAN,
            loop = LOOP_YEARLY,
        )

    }

}
