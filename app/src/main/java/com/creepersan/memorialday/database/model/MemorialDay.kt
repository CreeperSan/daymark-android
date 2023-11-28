package com.creepersan.memorialday.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class MemorialDay (
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "time") val time: Long,
    @Deprecated("字段已废弃") @ColumnInfo(name = "lunar_time") val lunarTime: String = "", /* （已废弃，为空字符串）储存格式为：1901-*05-23 ，其中月份前面的*代表是闰月 */
    @ColumnInfo(name = "time_type") val timeType: Int,  // 时间类型
    @ColumnInfo(name = "loop") val loop: Int,
) {

    companion object {

        const val LOOP_ONCE = 1
        const val LOOP_YEARLY = 2

        const val TIME_TYPE_GREGORIAN = 0
        const val TIME_TYPE_LUNAR = 1

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
