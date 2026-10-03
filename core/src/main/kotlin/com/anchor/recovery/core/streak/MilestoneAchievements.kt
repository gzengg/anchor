package com.anchor.recovery.core.streak

import com.anchor.recovery.core.clock.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/**
 * 一次里程碑达成记录。
 *
 * @param days 里程碑天数（1 / 7 / 30 / 60 / 90）
 * @param eraStartDate 达成所在 streak 纪元的起算日（该段连续打卡的第一天）
 * @param achievedDate 达成日：该纪元内连续计数首次达到 [days] 的自然日
 */
data class MilestoneAchievement(
    val days: Int,
    val eraStartDate: LocalDate,
    val achievedDate: LocalDate,
)

/**
 * 徽章墙上单个里程碑的展示状态。
 *
 * @param achieved 历史上是否达成过（达成记录跨纪元保留）
 * @param latestAchievedDate 最近一次达成日；从未达成为 null
 * @param eraCount 历史达成次数（按纪元去重，同一纪元内的重复达成只计一次）
 * @param currentEraReached 当前纪元内是否已达成（当前 streak 为 0 时为 false）
 * @param daysRemaining 当前纪元距该里程碑还差的自然日数（已达成或已清零时为 0）
 * @param progress 相对该里程碑的当前纪元进度，范围 0f..1f
 */
data class MilestoneStatus(
    val milestone: RebootMilestone,
    val achieved: Boolean,
    val latestAchievedDate: LocalDate?,
    val eraCount: Int,
    val currentEraReached: Boolean,
    val daysRemaining: Int,
    val progress: Float,
)

/**
 * 里程碑达成判定（纯逻辑，无 Android 依赖）。
 *
 * 判定规则与 [StreakCalculator] 完全一致，保证「徽章墙点亮的里程碑」不会超过
 * 首页展示的最长连续天数：
 * 1. 打卡按**自然日**去重升序；**漏打一整天即断**，断签后从 1 重新计数，属于新纪元。
 * 2. **复吸当天清零**：复吸日本身在清零前的连续计数仍然有效（与 `longestDays` 口径一致），
 *    次日从 1 起算 → 复吸日结束当前纪元。
 * 3. 纪元以「起算日（该段第一次打卡的日期）」标识；**达成记录跨纪元保留**，
 *    新纪元再次达成会新增一条记录，因此同一里程碑可以有多条记录。
 * 4. 同一纪元内同一里程碑只记一次。
 *
 * 用途与边界：本对象只把「已发生的打卡/复吸历史」翻译成达成记录，
 * 不负责持久化（落库由 `:app` 的 `milestone_achievement` 表完成），也不做任何疗效承诺。
 */
object MilestoneAchievements {

    /** 里程碑天数升序列表。 */
    val MILESTONE_DAYS: List<Int> = RebootFramework.milestones.map { it.days }

    /**
     * 推导全部达成记录（跨纪元），按达成时间升序返回；可重复调用，结果稳定。
     */
    fun detect(checkInDates: List<LocalDate>, relapseDates: Set<LocalDate>): List<MilestoneAchievement> =
        walk(checkInDates, relapseDates).achievements

    /**
     * 汇总徽章墙状态。
     *
     * @param currentDays 当前连续打卡天数（由 [StreakCalculator] 计算，口径见其 KDoc）
     * @param extraAchievements 已落库、但当前历史推导不出来的达成记录（例如用户后来删掉了当年的打卡）。
     *   传进来后与现算结果合并：**已获得的徽章不会因为删记录而熄灭**。
     */
    fun wall(
        checkInDates: List<LocalDate>,
        relapseDates: Set<LocalDate>,
        currentDays: Int,
        extraAchievements: List<MilestoneAchievement> = emptyList(),
    ): List<MilestoneStatus> {
        val result = walk(checkInDates, relapseDates)
        // 当前 streak 为 0 时没有「当前纪元」，历史达成记录仍然保留。
        val currentEraStart = if (currentDays > 0) result.lastEraStartDate else null
        val records = (result.achievements + extraAchievements).distinct()
        return RebootFramework.milestones.map { milestone ->
            val matched = records.filter { it.days == milestone.days }
            val latest = matched.maxWithOrNull(
                compareBy({ it.achievedDate.toEpochDays() }, { it.eraStartDate.toEpochDays() }),
            )
            MilestoneStatus(
                milestone = milestone,
                achieved = matched.isNotEmpty(),
                latestAchievedDate = latest?.achievedDate,
                eraCount = matched.map { it.eraStartDate }.distinct().size,
                currentEraReached = currentEraStart != null &&
                    matched.any { it.eraStartDate == currentEraStart },
                daysRemaining = (milestone.days - currentDays).coerceAtLeast(0),
                progress = (currentDays.toFloat() / milestone.days).coerceIn(0f, 1f),
            )
        }
    }

    private class Walk(
        val achievements: List<MilestoneAchievement>,
        val lastEraStartDate: LocalDate?,
    )

    private fun walk(checkInDates: List<LocalDate>, relapseDates: Set<LocalDate>): Walk {
        val days = checkInDates.distinct().sortedBy { it.toEpochDays() }
        if (days.isEmpty()) return Walk(emptyList(), null)

        val achievements = mutableListOf<MilestoneAchievement>()
        val milestoneDays = MILESTONE_DAYS.toSet()
        var counter = 0
        var previous: LocalDate? = null
        var previousWasRelapse = false
        var eraStart: LocalDate? = null
        val reachedInEra = mutableSetOf<Int>()

        for (day in days) {
            val contiguous = previous != null && day.toEpochDays() == previous.toEpochDays() + 1
            // 断签或上一日是复吸日（计数已清零）都从 1 重新起算，即新纪元。
            if (!contiguous || previousWasRelapse) {
                eraStart = day
                reachedInEra.clear()
            }
            counter = if (contiguous) counter + 1 else 1

            // 清零前的计数有效：复吸日当天达成的里程碑照常记录。
            if (counter in milestoneDays && reachedInEra.add(counter)) {
                val start = eraStart ?: day
                achievements += MilestoneAchievement(days = counter, eraStartDate = start, achievedDate = day)
            }
            if (day in relapseDates) counter = 0

            previous = day
            previousWasRelapse = day in relapseDates
        }
        return Walk(achievements, eraStart)
    }
}

/** 注入时钟的便捷封装：负责把 [Instant] 复吸时刻换算成自然日。 */
class MilestoneTracker(private val clock: Clock) {

    fun detect(checkInDates: List<LocalDate>, relapseInstants: List<Instant>): List<MilestoneAchievement> =
        MilestoneAchievements.detect(checkInDates, relapseDatesOf(relapseInstants))

    fun wall(
        checkInDates: List<LocalDate>,
        relapseInstants: List<Instant>,
        currentDays: Int,
        extraAchievements: List<MilestoneAchievement> = emptyList(),
    ): List<MilestoneStatus> =
        MilestoneAchievements.wall(
            checkInDates = checkInDates,
            relapseDates = relapseDatesOf(relapseInstants),
            currentDays = currentDays,
            extraAchievements = extraAchievements,
        )

    private fun relapseDatesOf(relapseInstants: List<Instant>): Set<LocalDate> =
        relapseInstants.map { clock.localDateOf(it) }.toSet()
}
