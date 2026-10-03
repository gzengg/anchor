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

    /**
     * 拒绝导入。[args] 是展示参数（版本号、字段名、重复日期等），
     * 由界面按 [reason] 取资源文案（`:app` 的 `importRejectionText`）。
     */
    data class Rejected(val reason: ImportRejection, val args: List<Any> = emptyList()) : ImportResult
}

/** 导入被拒绝的原因码；展示文案在 `:app`，core 不持有中文文案。 */
enum class ImportRejection {
    EMPTY_FILE,
    NOT_ANCHOR_FILE,
    MISSING_VERSION,
    UNSUPPORTED_VERSION,
    PARSE_ERROR,
    UNKNOWN_PARSE_ERROR,
    DUPLICATE_DATES,
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
        if (text.isBlank()) return ImportResult.Rejected(ImportRejection.EMPTY_FILE)

        val payload = runCatching { DataExporter.fromJson(text) }.getOrElse {
            return ImportResult.Rejected(ImportRejection.NOT_ANCHOR_FILE)
        }

        if (payload.schemaVersion < 1) {
            return ImportResult.Rejected(ImportRejection.MISSING_VERSION)
        }
        if (payload.schemaVersion > supportedVersion) {
            return ImportResult.Rejected(
                ImportRejection.UNSUPPORTED_VERSION,
                args = listOf(payload.schemaVersion, supportedVersion),
            )
        }

        val data = runCatching { payload.toImportedData() }.getOrElse { error ->
            val detail = error.message
            return if (detail == null) {
                ImportResult.Rejected(ImportRejection.UNKNOWN_PARSE_ERROR)
            } else {
                ImportResult.Rejected(ImportRejection.PARSE_ERROR, args = listOf(detail))
            }
        }

        // 打卡以日期为主键、写入时用 IGNORE：同一天两条会被默默丢弃，所以先在这里卡住。
        val duplicatedDates = data.checkIns
            .groupingBy { it.date }
            .eachCount()
            .filterValues { it > 1 }
            .keys
        if (duplicatedDates.isNotEmpty()) {
            return ImportResult.Rejected(
                ImportRejection.DUPLICATE_DATES,
                args = listOf(duplicatedDates.sorted().joinToString("、")),
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
