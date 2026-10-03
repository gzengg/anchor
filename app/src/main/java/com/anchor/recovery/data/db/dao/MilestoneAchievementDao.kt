package com.anchor.recovery.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.anchor.recovery.data.db.entity.MilestoneAchievementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MilestoneAchievementDao {

    @Query("SELECT * FROM milestone_achievement ORDER BY milestoneDays ASC, achievedDate ASC")
    fun observeAll(): Flow<List<MilestoneAchievementEntity>>

    @Query("SELECT * FROM milestone_achievement ORDER BY milestoneDays ASC, achievedDate ASC")
    suspend fun all(): List<MilestoneAchievementEntity>

    /**
     * 只增不删：主键（里程碑天数 + 纪元起算日）重复时静默忽略，
     * 因此每次启动/打卡后重复补写不会产生重复行。
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(rows: List<MilestoneAchievementEntity>): List<Long>

    @Query("DELETE FROM milestone_achievement WHERE eraStartDate = :eraStartDate")
    suspend fun deleteByEra(eraStartDate: String)

    @Query("DELETE FROM milestone_achievement")
    suspend fun clear()
}
