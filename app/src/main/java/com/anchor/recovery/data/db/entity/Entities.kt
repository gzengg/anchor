package com.anchor.recovery.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 打卡记录。date 为本地自然日 ISO 字符串（yyyy-MM-dd），唯一索引保证一天最多一条。
 */
@Entity(
    tableName = "check_in",
    indices = [Index(value = ["date"], unique = true)],
)
data class CheckInEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val note: String = "",
    val createdAt: Long,
)

/**
 * 复吸记录。emotions / triggers 为逗号分隔的标签文本（标签本身不含逗号）。
 */
@Entity(tableName = "relapse")
data class RelapseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val occurredAt: Long,
    val situation: String = "",
    val emotions: String = "",
    val triggers: String = "",
    val note: String = "",
)

/**
 * 一次渴求事件。tool 存 [com.anchor.recovery.core.model.UrgeTool] 的枚举名。
 */
@Entity(tableName = "urge_episode")
data class UrgeEpisodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val durationSec: Int,
    val peakIntensity: Int,
    val endIntensity: Int,
    val tool: String,
)

/**
 * 问卷结果。type 存 [com.anchor.recovery.core.model.AssessmentType] 的枚举名，
 * answersJson 为原始作答数组（便于日后重新计分）。
 */
@Entity(tableName = "assessment_result")
data class AssessmentResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val takenAt: Long,
    val totalScore: Int,
    val level: String,
    val answersJson: String,
)

/**
 * 里程碑达成记录（P2 徽章墙）。由打卡/复吸历史推导后写入，但**只增不删**：
 * 用户后来删掉当年的打卡，已经获得的徽章仍然保留（达成日期是发生过的事实）。
 *
 * 主键 = （里程碑天数 + streak 纪元起算日）：`milestoneDays` 就是里程碑 id
 * （见 `RebootFramework.milestones` 的 1/7/30/60/90）；复吸清零后进入新纪元再次达成时
 * 会多出一行，同一纪元内不会重复。日期列同为本地自然日 ISO 字符串。
 */
@Entity(tableName = "milestone_achievement", primaryKeys = ["milestoneDays", "eraStartDate"])
data class MilestoneAchievementEntity(
    val milestoneDays: Int,
    val eraStartDate: String,
    val achievedDate: String,
    val recordedAt: Long,
)
