package com.anchor.recovery.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.anchor.recovery.data.db.entity.AssessmentResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssessmentResultDao {

    @Query("SELECT * FROM assessment_result ORDER BY takenAt DESC")
    fun observeAll(): Flow<List<AssessmentResultEntity>>

    @Query("SELECT * FROM assessment_result WHERE type = :type ORDER BY takenAt DESC LIMIT 1")
    suspend fun latestOfType(type: String): AssessmentResultEntity?

    @Insert
    suspend fun insert(entity: AssessmentResultEntity): Long

    @Query("DELETE FROM assessment_result")
    suspend fun clear()
}
