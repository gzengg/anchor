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
