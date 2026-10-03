package com.anchor.recovery.core.streak

/**
 * 90 天重启里程碑：1 / 7 / 30 / 60 / 90 天。
 *
 * 徽章名与提示语是用户可见文案，已外移到 `:app`（`values/strings_core.xml`，
 * 按天数映射为 `milestone_days_title_*` / `milestone_days_hint_*`）；本模块只保留
 * 天数口径与判定逻辑，避免文案与判定逻辑两处漂移。
 *
 * 文案中性要求（不得出现「大脑已恢复」「保证」之类承诺）由 `:app` 侧
 * `MilestoneCopyComplianceTest` 对资源文案断言——规则随文案一起搬走了。
 */
data class RebootMilestone(val days: Int)

object RebootFramework {

    const val GOAL_DAYS = 90

    val milestones: List<RebootMilestone> = listOf(
        RebootMilestone(1),
        RebootMilestone(7),
        RebootMilestone(30),
        RebootMilestone(60),
        RebootMilestone(90),
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
