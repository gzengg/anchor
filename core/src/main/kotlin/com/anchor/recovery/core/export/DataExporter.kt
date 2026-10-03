package com.anchor.recovery.core.export

import com.anchor.recovery.core.model.AssessmentRecord
import com.anchor.recovery.core.model.CheckInRecord
import com.anchor.recovery.core.model.RelapseRecord
import com.anchor.recovery.core.model.UrgeEpisodeRecord
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 导出文件的固定大纲（schema）。字段名写进文件后不宜再改，改动要同时升 [DataExporter.SCHEMA_VERSION]。
 *
 * 时间一律导出为 ISO 8601 字符串（打卡日期 `yyyy-MM-dd`，时刻带 `Z`），
 * 便于用户自己看懂、也便于日后导入时解析。
 */
@Serializable
data class ExportPayload(
    val schemaVersion: Int,
    val app: String,
    val appVersion: String,
    val exportedAt: String,
    val checkIns: List<ExportCheckIn>,
    val relapses: List<ExportRelapse>,
    val urgeEpisodes: List<ExportUrgeEpisode>,
    val assessments: List<ExportAssessment>,
    val settings: ExportSettings,
) {
    /** 与用户核对导出了多少条数据。 */
    fun countsLine(): String = DataExporter.countsLine(
        checkIns = checkIns.size,
        relapses = relapses.size,
        urgeEpisodes = urgeEpisodes.size,
        assessments = assessments.size,
    )
}

@Serializable
data class ExportCheckIn(
    val date: String,
    val note: String,
    val createdAt: String,
)

@Serializable
data class ExportRelapse(
    val id: Long,
    val occurredAt: String,
    val situation: String,
    val emotions: List<String>,
    val triggers: List<String>,
    val note: String,
)

@Serializable
data class ExportUrgeEpisode(
    val id: Long,
    val startedAt: String,
    val durationSec: Int,
    val peakIntensity: Int,
    val endIntensity: Int,
    val tool: String,
)

@Serializable
data class ExportAssessment(
    val id: Long,
    val type: String,
    val takenAt: String,
    val totalScore: Int,
    val level: String,
    val answers: List<Int>,
)

@Serializable
data class ExportSettings(
    val reminderEnabled: Boolean,
    val reminderTime: String,
    val motivationPrompts: List<String>,
)

/**
 * 把仓库里的记录装配成导出 JSON。纯函数（时间也由调用方传入），可 JVM 测试。
 */
object DataExporter {

    const val SCHEMA_VERSION = 1

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun build(
        appName: String,
        appVersion: String,
        exportedAt: Instant,
        checkIns: List<CheckInRecord>,
        relapses: List<RelapseRecord>,
        urgeEpisodes: List<UrgeEpisodeRecord>,
        assessments: List<AssessmentRecord>,
        reminderEnabled: Boolean,
        reminderTime: String,
        motivationPrompts: List<String>,
    ): ExportPayload = ExportPayload(
        schemaVersion = SCHEMA_VERSION,
        app = appName,
        appVersion = appVersion,
        exportedAt = exportedAt.toString(),
        checkIns = checkIns.map {
            ExportCheckIn(date = it.date.toString(), note = it.note, createdAt = it.createdAt.toString())
        },
        relapses = relapses.map {
            ExportRelapse(
                id = it.id,
                occurredAt = it.occurredAt.toString(),
                situation = it.situation,
                emotions = it.emotions,
                triggers = it.triggers,
                note = it.note,
            )
        },
        urgeEpisodes = urgeEpisodes.map {
            ExportUrgeEpisode(
                id = it.id,
                startedAt = it.startedAt.toString(),
                durationSec = it.durationSec,
                peakIntensity = it.peakIntensity,
                endIntensity = it.endIntensity,
                tool = it.tool.name,
            )
        },
        assessments = assessments.map {
            ExportAssessment(
                id = it.id,
                type = it.type.name,
                takenAt = it.takenAt.toString(),
                totalScore = it.totalScore,
                level = it.level,
                answers = it.answers,
            )
        },
        settings = ExportSettings(
            reminderEnabled = reminderEnabled,
            reminderTime = reminderTime,
            motivationPrompts = motivationPrompts,
        ),
    )

    fun toJson(payload: ExportPayload): String = json.encodeToString(payload)

    /** 记录条数文案（设置页与导出文件共用同一份措辞）。 */
    fun countsLine(checkIns: Int, relapses: Int, urgeEpisodes: Int, assessments: Int): String =
        "打卡 $checkIns 天 · 复吸 $relapses 次 · 渴求 $urgeEpisodes 次 · 问卷 $assessments 份"

    fun fromJson(text: String): ExportPayload = json.decodeFromString(text)

    /** 文件名固定带导出日期，便于按时间排序；不带任何敏感词。 */
    fun fileName(exportedAt: Instant): String = "anchor-export-${exportedAt.toString().take(10)}.json"
}
