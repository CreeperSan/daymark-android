package com.creepersan.memorialday.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.creepersan.memorialday.database.model.MemorialDay

@Dao
interface MemorialDayDao {

    @Query("SELECT * FROM MemorialDay")
    fun getAll(): List<MemorialDay>

    @Query("SELECT * FROM MemorialDay WHERE id = :id LIMIT 1")
    fun findMemorialDay(id: Long): MemorialDay?

    @Insert
    fun insert(vararg memorialDay: MemorialDay)

    @Update
    fun update(vararg memorialDay: MemorialDay)

    @Delete
    fun delete(memorialDay: MemorialDay)

}