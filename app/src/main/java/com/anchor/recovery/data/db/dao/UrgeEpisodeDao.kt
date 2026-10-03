package com.anchor.recovery.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.anchor.recovery.data.db.entity.UrgeEpisodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UrgeEpisodeDao {

    @Query("SELECT * FROM urge_episode ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<UrgeEpisodeEntity>>

    @Query("SELECT * FROM urge_episode ORDER BY startedAt DESC")
    suspend fun all(): List<UrgeEpisodeEntity>

    @Insert
    suspend fun insert(entity: UrgeEpisodeEntity): Long

    @Query("DELETE FROM urge_episode WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM urge_episode")
    suspend fun clear()
}
