package com.anchor.recovery.core.relapse

import com.anchor.recovery.core.clock.Clock
import com.anchor.recovery.core.clock.SystemClock
import com.anchor.recovery.core.model.RelapseRecord
import kotlinx.datetime.Instant
import kotlinx.datetime.toLocalDateTime

/** 复吸记录表单用到的固定标签，同时作为统计口径（避免各页面各写一套）。 */
object RelapseTags {
    val emotions: List<String> = listOf(
        "焦虑",
        "烦躁",
        "孤独",
        "无聊",
        "低落",
        "压力大",
        "疲惫",
        "愤怒",
        "兴奋",
    )

    val triggers: List<String> = listOf(
        "深夜独处",
        "压力事件",
        "社交媒体",
        "饮酒",
        "闲暇无聊",
        "情绪低落",
        "失眠",
        "看到相关内容",
        "社交场合",
    )
}

/** 一天被切成四个时段，用于“复吸时段分布”。 */
enum class DayPart(val label: String) {
    MORNING("上午 05–11"),
    AFTERNOON("下午 12–17"),
    EVENING("晚上 18–22"),
    LATE_NIGHT("深夜 23–04"),
}

data class FrequencyItem(val label: String, val count: Int)

/** F5 统计卡的数据：频次 Top-N、时段分布、复吸间隔中位数。 */
data class RelapseInsight(
    val totalRelapses: Int = 0,
    val emotionTop: List<FrequencyItem> = emptyList(),
    val triggerTop: List<FrequencyItem> = emptyList(),
    val dayPartCounts: Map<DayPart, Int> = emptyMap(),
    val medianIntervalHours: Double? = null,
    val noteCount: Int = 0,
) {
    val isEmpty: Boolean get() = totalRelapses == 0

    /** 出现最多的时段；无记录时返回 null。 */
    val busiestDayPart: DayPart?
        get() = dayPartCounts.filterValues { it > 0 }.maxByOrNull { it.value }?.key
}

/**
 * F5 触发因素分析：纯统计，不涉及任何临床判断。
 *
 * 全部标签都是用户自己选的，统计结果只用于“回看规律”，页面文案不得做因果解释。
 */
class TriggerAnalyzer(
    private val clock: Clock = SystemClock(),
    val topN: Int = 3,
) {

    fun analyze(relapses: List<RelapseRecord>): RelapseInsight {
        if (relapses.isEmpty()) return RelapseInsight()

        val sorted = relapses.sortedBy { it.occurredAt }
        val dayPartCounts = DayPart.entries.associateWith { part ->
            sorted.count { dayPartOf(it.occurredAt) == part }
        }
        return RelapseInsight(
            totalRelapses = sorted.size,
            emotionTop = topFrequencies(sorted.flatMap { it.emotions }),
            triggerTop = topFrequencies(sorted.flatMap { it.triggers }),
            dayPartCounts = dayPartCounts,
            medianIntervalHours = medianIntervalHours(sorted.map { it.occurredAt }),
            noteCount = sorted.count { it.note.isNotBlank() },
        )
    }

    /** 频次排序：次数降序，次数相同按中文/字母序，保证结果稳定可测。 */
    fun topFrequencies(values: List<String>, limit: Int = topN): List<FrequencyItem> =
        values.asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .groupingBy { it }
            .eachCount()
            .map { (label, count) -> FrequencyItem(label, count) }
            .sortedWith(compareByDescending<FrequencyItem> { it.count }.thenBy { it.label })
            .take(limit)

    /** 相邻两次复吸的间隔（小时）中位数；不足两条记录时返回 null。 */
    fun medianIntervalHours(instants: List<Instant>): Double? {
        if (instants.size < 2) return null
        val sorted = instants.sorted()
        val intervals = sorted.zipWithNext { previous, next ->
            (next.epochSeconds - previous.epochSeconds).toDouble() / 3600.0
        }
        return median(intervals)
    }

    /** 中位数：偶数个数取中间两个的平均值。 */
    fun median(values: List<Double>): Double? {
        if (values.isEmpty()) return null
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) {
            sorted[middle]
        } else {
            (sorted[middle - 1] + sorted[middle]) / 2.0
        }
    }

    fun dayPartOf(instant: Instant): DayPart {
        val hour = instant.toLocalDateTime(clock.timeZone).hour
        return when (hour) {
            in 5..11 -> DayPart.MORNING
            in 12..17 -> DayPart.AFTERNOON
            in 18..22 -> DayPart.EVENING
            else -> DayPart.LATE_NIGHT
        }
    }
}
