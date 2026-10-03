package com.anchor.recovery.core.streak

/**
 * 90 天重启里程碑：1 / 7 / 30 / 60 / 90 天。
 *
 * 文案刻意保持中性：里程碑是**自我记录的参照点**，不是医学疗效承诺
 * （不得出现「大脑已恢复」「治愈」之类未经证实或过度病理化的表述）。
 */
data class RebootMilestone(
    val days: Int,
    val title: String,
    val hint: String,
)

object RebootFramework {

    const val GOAL_DAYS = 90

    val milestones: List<RebootMilestone> = listOf(
        RebootMilestone(1, "第 1 天", "迈出第一步。记录本身就是有效动作，不需要别的条件。"),
        RebootMilestone(7, "第 1 周", "一周里渴求可能反复出现，这是常见波动，不代表前功尽弃。"),
        RebootMilestone(30, "第 1 个月", "一个月通常已经形成固定的记录习惯，可以回看自己的触发规律。"),
        RebootMilestone(60, "第 2 个月", "两个月后，留意哪些情境仍然高风险，提前准备替代动作。"),
        RebootMilestone(90, "90 天重启", "90 天是社群常用的阶段参照，不是医学意义上的「痊愈」判定。"),
    )

    /** 已达成（含当天）的里程碑。 */
    fun reached(currentDays: Int): List<RebootMilestone> =
        milestones.filter { currentDays >= it.days }

    /** 下一个尚未达成的里程碑；全部达成后返回 null。 */
    fun next(currentDays: Int): RebootMilestone? =
        milestones.firstOrNull { currentDays < it.days }

    /** 距 90 天目标的进度，范围 0f..1f。 */
    fun progress(currentDays: Int): Float =
        (currentDays.coerceIn(0, GOAL_DAYS).toFloat() / GOAL_DAYS)
}
