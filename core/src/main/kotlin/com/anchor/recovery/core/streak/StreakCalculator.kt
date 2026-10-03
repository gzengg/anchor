package com.anchor.recovery.core.streak

import com.anchor.recovery.core.clock.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/**
 * 打卡/连击汇总状态。
 *
 * @param currentDays 当前连续打卡天数（见 [StreakCalculator] 的判定规则）
 * @param longestDays 历史最长连续打卡天数（跨复吸重置追踪）
 * @param todayCheckedIn 今天是否已打卡
 * @param totalCheckInDays 去重后的打卡自然日总数
 * @param lastCheckInDate 最近一次打卡的自然日
 * @param lastRelapseDate 最近一次复吸所在自然日
 * @param daysSinceLastRelapse 距最近一次复吸的自然日数（无复吸则为 null）
 */
data class StreakState(
    val currentDays: Int,
    val longestDays: Int,
    val todayCheckedIn: Boolean,
    val totalCheckInDays: Int,
    val lastCheckInDate: LocalDate?,
    val lastRelapseDate: LocalDate?,
    val daysSinceLastRelapse: Int?,
) {
    companion object {
        val EMPTY = StreakState(
            currentDays = 0,
            longestDays = 0,
            todayCheckedIn = false,
            totalCheckInDays = 0,
            lastCheckInDate = null,
            lastRelapseDate = null,
            daysSinceLastRelapse = null,
        )
    }
}

/**
 * 连续打卡天数计算（纯逻辑，无 Android 依赖）。
 *
 * 判定规则（与产品口径一致）：
 * 1. 打卡按**自然日**去重（同一天多次打卡只算一次），按日期升序扫描。
 * 2. **漏打一整天即断**：若相邻两次打卡日期不是「前一天→后一天」，计数器从新的一天的 1 重新开始。
 * 3. **复吸当天清零**：若某打卡日同时是复吸日，该日计数为 0（当前 streak 不包含复吸日），
 *    次日重新从 1 起算；但**复吸前一刻的连续计数计入历史最长**（用户确实连打了 N 天）。
 * 4. 当前 streak 以「今天」或「昨天」为锚点：今天没打但昨天打了，视为当天尚未结束、streak 仍然有效；
 *    最近一次打卡早于昨天则当前 streak 为 0。
 * 5. 最长 streak 在所有已打卡日期上取最大值，因此会跨复吸/断签重置持续追踪。
 */
class StreakCalculator(private val clock: Clock) {

    fun compute(checkInDates: List<LocalDate>, relapseInstants: List<Instant>): StreakState {
        val today = clock.today()
        val days = checkInDates.distinct().sortedBy { it.toEpochDays() }
        if (days.isEmpty()) {
            val lastRelapseDate = relapseInstants.map { clock.localDateOf(it) }.maxOrNull()
            return StreakState.EMPTY.copy(
                lastRelapseDate = lastRelapseDate,
                daysSinceLastRelapse = lastRelapseDate?.let { it.dayGapTo(today) },
            )
        }

        val relapseDates = relapseInstants.map { clock.localDateOf(it) }.toSet()
        val streakValues = LinkedHashMap<LocalDate, Int>(days.size)
        var counter = 0
        var longestDays = 0
        var previous: LocalDate? = null

        for (day in days) {
            val contiguous = previous != null && day.toEpochDays() == previous.toEpochDays() + 1
            counter = if (contiguous) counter + 1 else 1
            // 复吸日：清零前的计数仍然代表用户真实达成的连续天数
            if (counter > longestDays) longestDays = counter
            if (day in relapseDates) counter = 0
            streakValues[day] = counter
            previous = day
        }

        val lastCheckInDate = days.last()
        val currentDays = currentStreak(streakValues, days, today, relapseDates)

        val lastRelapseDate = relapseDates.maxOrNull()
        return StreakState(
            currentDays = currentDays,
            longestDays = longestDays,
            todayCheckedIn = today in streakValues,
            totalCheckInDays = days.size,
            lastCheckInDate = lastCheckInDate,
            lastRelapseDate = lastRelapseDate,
            daysSinceLastRelapse = lastRelapseDate?.let { it.dayGapTo(today) },
        )
    }

    private fun currentStreak(
        streakValues: Map<LocalDate, Int>,
        days: List<LocalDate>,
        today: LocalDate,
        relapseDates: Set<LocalDate>,
    ): Int {
        if (today in relapseDates) return 0
        streakValues[today]?.let { return it }

        val yesterday = today.minusDays(1)
        if (yesterday in relapseDates) return 0
        if (streakValues[yesterday] == null) return 0

        // 昨天打过卡但今天还没打：streak 尚未断，按昨天的计数展示。
        val hasGapBeforeYesterday = yesterday !in days
        return if (hasGapBeforeYesterday) 0 else streakValues.getValue(yesterday)
    }
}

internal fun LocalDate.minusDays(days: Int): LocalDate = LocalDate.fromEpochDays(toEpochDays() - days)

/** [this] 到 [other] 的自然日差（负数表示 other 更早）。 */
internal fun LocalDate.dayGapTo(other: LocalDate): Int = other.toEpochDays() - toEpochDays()
