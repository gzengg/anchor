package com.anchor.recovery.core.export

import com.anchor.recovery.core.model.AssessmentRecord
import com.anchor.recovery.core.model.AssessmentType
import com.anchor.recovery.core.model.CheckInRecord
import com.anchor.recovery.core.model.RelapseRecord
import com.anchor.recovery.core.model.UrgeEpisodeRecord
import com.anchor.recovery.core.model.UrgeTool
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/** 解析后的导入内容：已经是领域模型，与 JSON 大纲解耦。 */
data class ImportedData(
    val checkIns: List<CheckInRecord>,
    val relapses: List<RelapseRecord>,
    val urgeEpisodes: List<UrgeEpisodeRecord>,
    val assessments: List<AssessmentRecord>,
    val reminderEnabled: Boolean,
    val reminderTime: String,
    val motivationPrompts: List<String>,
) {
    val isEmpty: Boolean
        get() = checkIns.isEmpty() && relapses.isEmpty() &&
            urgeEpisodes.isEmpty() && assessments.isEmpty()

    fun countsLine(): String = DataExporter.countsLine(
        checkIns = checkIns.size,
        relapses = relapses.size,
        urgeEpisodes = urgeEpisodes.size,
        assessments = assessments.size,
    )
}

/** 导入判定：要么可以导入，要么带一句人话原因拒绝。 */
sealed interface ImportResult {

    /** 文件可导入。[exportedAt] 用于在确认弹窗里报出「这份文件是什么时候导的」。 */
    data class Ready(
        val data: ImportedData,
        val exportedAt: String,
        val schemaVersion: Int,
    ) : ImportResult

    data class Rejected(val reason: String) : ImportResult
}

/**
 * 解析导入文件。纯函数，可 JVM 测试。
 *
 * 三条硬规则：
 * 1. 版本不匹配一律拒绝——比当前新的格式直接提示升级，比 1 还小的版本号视为损坏文件；
 * 2. 解析失败不抛异常给界面，一律转成 [ImportResult.Rejected]；
 * 3. 单条记录时间戳解析失败 = 整份文件拒绝，不做「跳过坏记录」的静默降级，
 *    否则用户会以为导入成功而实际少了数据。
 */
object DataImporter {

    fun parse(text: String, supportedVersion: Int = DataExporter.SCHEMA_VERSION): ImportResult {
        if (text.isBlank()) return ImportResult.Rejected("文件是空的，没有可导入的数据。")

        val payload = runCatching { DataExporter.fromJson(text) }.getOrElse {
            return ImportResult.Rejected("这不是磐石导出的数据文件，读不出来。")
        }

        if (payload.schemaVersion < 1) {
            return ImportResult.Rejected("文件缺少版本号，无法确认格式。")
        }
        if (payload.schemaVersion > supportedVersion) {
            return ImportResult.Rejected(
                "文件来自更新的版本（格式 v${payload.schemaVersion}），当前应用只认到 v$supportedVersion，请先升级磐石。",
            )
        }

        val data = runCatching { payload.toImportedData() }.getOrElse { error ->
            return ImportResult.Rejected("文件里的记录读不出来（${error.message ?: "格式异常"}）。")
        }

        // 打卡以日期为主键、写入时用 IGNORE：同一天两条会被默默丢弃，所以先在这里卡住。
        val duplicatedDates = data.checkIns
            .groupingBy { it.date }
            .eachCount()
            .filterValues { it > 1 }
            .keys
        if (duplicatedDates.isNotEmpty()) {
            return ImportResult.Rejected(
                "文件里有重复的打卡日期（${duplicatedDates.sorted().joinToString("、")}），为免丢数据已拒绝导入。",
            )
        }

        return ImportResult.Ready(
            data = data,
            exportedAt = payload.exportedAt,
            schemaVersion = payload.schemaVersion,
        )
    }
}

private fun ExportPayload.toImportedData(): ImportedData = ImportedData(
    checkIns = checkIns.map { it.toRecord() },
    relapses = relapses.map { it.toRecord() },
    urgeEpisodes = urgeEpisodes.map { it.toRecord() },
    assessments = assessments.map { it.toRecord() },
    reminderEnabled = settings.reminderEnabled,
    reminderTime = settings.reminderTime,
    motivationPrompts = settings.motivationPrompts,
)

private fun ExportCheckIn.toRecord(): CheckInRecord = CheckInRecord(
    date = parseDate(date, "打卡日期"),
    note = note,
    createdAt = parseInstant(createdAt, "打卡时间"),
)

private fun ExportRelapse.toRecord(): RelapseRecord = RelapseRecord(
    id = id,
    occurredAt = parseInstant(occurredAt, "破戒时间"),
    situation = situation,
    emotions = emotions,
    triggers = triggers,
    note = note,
)

private fun ExportUrgeEpisode.toRecord(): UrgeEpisodeRecord = UrgeEpisodeRecord(
    id = id,
    startedAt = parseInstant(startedAt, "渴求开始时间"),
    durationSec = durationSec,
    peakIntensity = peakIntensity,
    endIntensity = endIntensity,
    tool = UrgeTool.of(tool),
)

private fun ExportAssessment.toRecord(): AssessmentRecord = AssessmentRecord(
    id = id,
    type = AssessmentType.of(type),
    takenAt = parseInstant(takenAt, "问卷时间"),
    totalScore = totalScore,
    level = level,
    answers = answers,
)

private fun parseDate(raw: String, field: String): LocalDate =
    runCatching { LocalDate.parse(raw) }.getOrElse {
        throw IllegalArgumentException("$field「$raw」不是有效日期")
    }

private fun parseInstant(raw: String, field: String): Instant =
    runCatching { Instant.parse(raw) }.getOrElse {
        throw IllegalArgumentException("$field「$raw」不是有效时间")
    }
