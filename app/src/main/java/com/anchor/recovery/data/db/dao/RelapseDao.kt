package com.anchor.recovery.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.anchor.recovery.data.db.entity.RelapseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RelapseDao {

    @Query("SELECT * FROM relapse ORDER BY occurredAt DESC")
    fun observeAll(): Flow<List<RelapseEntity>>

    @Query("SELECT * FROM relapse ORDER BY occurredAt DESC")
    suspend fun all(): List<RelapseEntity>

    @Insert
    suspend fun insert(entity: RelapseEntity): Long

    @Query("DELETE FROM relapse WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM relapse")
    suspend fun clear()
}
