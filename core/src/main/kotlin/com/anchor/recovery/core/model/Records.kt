package com.anchor.recovery.core.model

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/**
 * 领域模型：:core 只描述"一条记录是什么"，不关心它在 Room/JSON 里怎么存。
 * :app 的 Repository 负责 Entity ↔ 这些模型的映射。
 */

/** 一次打卡（自然日粒度）。 */
data class CheckInRecord(
    val date: LocalDate,
    val note: String = "",
    val createdAt: Instant,
)

/** 一次复吸记录。emotions / triggers 是用户多选的标签文本。 */
data class RelapseRecord(
    val id: Long = 0,
    val occurredAt: Instant,
    val situation: String = "",
    val emotions: List<String> = emptyList(),
    val triggers: List<String> = emptyList(),
    val note: String = "",
)

/** 渴求应对工具：[URGE_SURFING] 对应 F3 冲浪流程，[DELAY_TOOL] 对应 F4 十分钟延时。 */
enum class UrgeTool(val label: String) {
    URGE_SURFING("渴求冲浪"),
    DELAY_TOOL("十分钟延时"),
    ;

    companion object {
        /** 数据库里读到未知枚举名时兜底，不崩。 */
        fun of(name: String?): UrgeTool = entries.firstOrNull { it.name == name } ?: URGE_SURFING
    }
}

/** 一次渴求事件（无论是否完成工具）。 */
data class UrgeEpisodeRecord(
    val id: Long = 0,
    val startedAt: Instant,
    val durationSec: Int,
    val peakIntensity: Int,
    val endIntensity: Int,
    val tool: UrgeTool,
)

/** 问卷类型。 */
enum class AssessmentType(val label: String) {
    CSBD("成瘾倾向自评"),
    MORAL("道德冲突评估"),
    ;

    companion object {
        fun of(name: String?): AssessmentType = entries.firstOrNull { it.name == name } ?: CSBD
    }
}

/** 一次问卷结果（answers 为原始作答，顺序与题项一致）。 */
data class AssessmentRecord(
    val id: Long = 0,
    val type: AssessmentType,
    val takenAt: Instant,
    val totalScore: Int,
    val level: String,
    val answers: List<Int> = emptyList(),
)
