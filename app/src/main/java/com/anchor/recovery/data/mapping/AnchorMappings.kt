package com.anchor.recovery.data.mapping

import com.anchor.recovery.core.model.AssessmentRecord
import com.anchor.recovery.core.model.AssessmentType
import com.anchor.recovery.core.model.CheckInRecord
import com.anchor.recovery.core.model.RelapseRecord
import com.anchor.recovery.core.model.UrgeEpisodeRecord
import com.anchor.recovery.core.model.UrgeTool
import com.anchor.recovery.core.streak.MilestoneAchievement
import com.anchor.recovery.data.db.entity.AssessmentResultEntity
import com.anchor.recovery.data.db.entity.CheckInEntity
import com.anchor.recovery.data.db.entity.MilestoneAchievementEntity
import com.anchor.recovery.data.db.entity.RelapseEntity
import com.anchor.recovery.data.db.entity.UrgeEpisodeEntity
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Entity ↔ 领域模型映射。全部为纯函数，放在 :app 的 JVM 单测里直接验证。
 *
 * 时间列统一存毫秒时间戳，自然日列统一存 ISO 字符串（yyyy-MM-dd）。
 */
private val answersCodec = Json { ignoreUnknownKeys = true }

fun CheckInEntity.toDomain(): CheckInRecord = CheckInRecord(
    date = LocalDate.parse(date),
    note = note,
    createdAt = Instant.fromEpochMilliseconds(createdAt),
)

fun CheckInRecord.toEntity(): CheckInEntity = CheckInEntity(
    date = date.toString(),
    note = note,
    createdAt = createdAt.toEpochMilliseconds(),
)

fun RelapseEntity.toDomain(): RelapseRecord = RelapseRecord(
    id = id,
    occurredAt = Instant.fromEpochMilliseconds(occurredAt),
    situation = situation,
    emotions = decodeLabels(emotions),
    triggers = decodeLabels(triggers),
    note = note,
)

fun RelapseRecord.toEntity(): RelapseEntity = RelapseEntity(
    id = id,
    occurredAt = occurredAt.toEpochMilliseconds(),
    situation = situation,
    emotions = encodeLabels(emotions),
    triggers = encodeLabels(triggers),
    note = note,
)

fun UrgeEpisodeEntity.toDomain(): UrgeEpisodeRecord = UrgeEpisodeRecord(
    id = id,
    startedAt = Instant.fromEpochMilliseconds(startedAt),
    durationSec = durationSec,
    peakIntensity = peakIntensity,
    endIntensity = endIntensity,
    tool = UrgeTool.of(tool),
)

fun UrgeEpisodeRecord.toEntity(): UrgeEpisodeEntity = UrgeEpisodeEntity(
    id = id,
    startedAt = startedAt.toEpochMilliseconds(),
    durationSec = durationSec,
    peakIntensity = peakIntensity,
    endIntensity = endIntensity,
    tool = tool.name,
)

fun AssessmentResultEntity.toDomain(): AssessmentRecord = AssessmentRecord(
    id = id,
    type = AssessmentType.of(type),
    takenAt = Instant.fromEpochMilliseconds(takenAt),
    totalScore = totalScore,
    level = level,
    answers = decodeAnswers(answersJson),
)

fun AssessmentRecord.toEntity(): AssessmentResultEntity = AssessmentResultEntity(
    id = id,
    type = type.name,
    takenAt = takenAt.toEpochMilliseconds(),
    totalScore = totalScore,
    level = level,
    answersJson = encodeAnswers(answers),
)

/** 标签列表 → 逗号分隔文本（过滤空项，避免出现 `a,,b`）。 */fun encodeLabels(labels: List<String>): String =
    labels.map { it.trim() }.filter { it.isNotEmpty() }.joinToString(",")

/** 逗号分隔文本 → 标签列表。 */
fun decodeLabels(raw: String): List<String> =
    raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }

/** 作答数组 → JSON；解析失败时退回空数组（历史数据不完整不应导致崩溃）。 */
fun encodeAnswers(answers: List<Int>): String =
    answersCodec.encodeToString(ListSerializer(Int.serializer()), answers)

fun decodeAnswers(raw: String): List<Int> =
    runCatching { answersCodec.decodeFromString(ListSerializer(Int.serializer()), raw) }
        .getOrDefault(emptyList())

fun MilestoneAchievementEntity.toDomain(): MilestoneAchievement = MilestoneAchievement(
    days = milestoneDays,
    eraStartDate = LocalDate.parse(eraStartDate),
    achievedDate = LocalDate.parse(achievedDate),
)

/** @param recordedAt 落库时刻（毫秒）；达成日期本身由历史推导，不用当前时间。 */
fun MilestoneAchievement.toEntity(recordedAt: Long): MilestoneAchievementEntity =
    MilestoneAchievementEntity(
        milestoneDays = days,
        eraStartDate = eraStartDate.toString(),
        achievedDate = achievedDate.toString(),
        recordedAt = recordedAt,
    )
