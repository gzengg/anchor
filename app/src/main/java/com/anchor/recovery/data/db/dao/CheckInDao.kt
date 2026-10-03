package com.anchor.recovery.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anchor.recovery.data.db.entity.CheckInEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckInDao {

    @Query("SELECT * FROM check_in ORDER BY date ASC")
    fun observeAll(): Flow<List<CheckInEntity>>

    @Query("SELECT * FROM check_in ORDER BY date ASC")
    suspend fun all(): List<CheckInEntity>

    @Query("SELECT * FROM check_in WHERE date = :date LIMIT 1")
    suspend fun byDate(date: String): CheckInEntity?

    /** 同日重复打卡不抛异常：IGNORE 配合 date 唯一索引，落库结果由返回值（-1）判定。 */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: CheckInEntity): Long

    @Query("DELETE FROM check_in WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Query("DELETE FROM check_in")
    suspend fun clear()
}
