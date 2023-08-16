package com.creepersan.memorialday.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class MemorialDay (
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "time") val time: Long,
    @ColumnInfo(name = "loop") val loop: Int,
) {

    companion object {

        const val LOOP_ONCE = 1
        const val LOOP_YEARLY = 2

        fun empty(): MemorialDay = MemorialDay(
            id = 0,
            title = "",
            time = System.currentTimeMillis(),
            loop = LOOP_YEARLY,
        )

    }

}
