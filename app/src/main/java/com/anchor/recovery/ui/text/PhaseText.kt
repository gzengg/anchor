package com.anchor.recovery.ui.text

import androidx.annotation.StringRes
import com.anchor.recovery.R
import com.anchor.recovery.core.phase.PhaseNote
import com.anchor.recovery.core.phase.PhaseTextKey
import com.anchor.recovery.core.phase.WithdrawalPhase

/**
 * `:core` 阶段文案 key / 天数 → 展示资源。
 *
 * `:core` 不持有中文文案（它连 `android.*` 都不能依赖），只给出稳定 key 与天数口径；
 * 文案本体在 `values/strings_phases.xml`。这里留一个 `when` 不写 `else` 的总表：
 * `:core` 新增 key 时编译期就会失败，避免出现「有状态没文案」。
 *
 * 天数区间文案（"第 3–7 天"）由 [phaseDayRangeRes] + [phaseDayRangeArgs] 渲染，
 * 不在 `:core` 里拼中文字符串。文案参数按 `res + args` 传递，状态层不需要 Context。
 */

@StringRes
fun phaseTextRes(key: PhaseTextKey): Int = when (key) {
    PhaseTextKey.ACUTE_NAME -> R.string.phase_acute_name
    PhaseTextKey.ACUTE_HEADLINE -> R.string.phase_acute_headline
    PhaseTextKey.ACUTE_EXPECTATION_1 -> R.string.phase_acute_expectation_1
    PhaseTextKey.ACUTE_EXPECTATION_2 -> R.string.phase_acute_expectation_2
    PhaseTextKey.ACUTE_EXPECTATION_3 -> R.string.phase_acute_expectation_3
    PhaseTextKey.ACUTE_COPING_1 -> R.string.phase_acute_coping_1
    PhaseTextKey.ACUTE_COPING_2 -> R.string.phase_acute_coping_2
    PhaseTextKey.ACUTE_COPING_3 -> R.string.phase_acute_coping_3
    PhaseTextKey.ACUTE_COPING_4 -> R.string.phase_acute_coping_4
    PhaseTextKey.ACUTE_CAUTION -> R.string.phase_acute_caution

    PhaseTextKey.FLUCTUATION_NAME -> R.string.phase_fluctuation_name
    PhaseTextKey.FLUCTUATION_HEADLINE -> R.string.phase_fluctuation_headline
    PhaseTextKey.FLUCTUATION_EXPECTATION_1 -> R.string.phase_fluctuation_expectation_1
    PhaseTextKey.FLUCTUATION_EXPECTATION_2 -> R.string.phase_fluctuation_expectation_2
    PhaseTextKey.FLUCTUATION_EXPECTATION_3 -> R.string.phase_fluctuation_expectation_3
    PhaseTextKey.FLUCTUATION_COPING_1 -> R.string.phase_fluctuation_coping_1
    PhaseTextKey.FLUCTUATION_COPING_2 -> R.string.phase_fluctuation_coping_2
    PhaseTextKey.FLUCTUATION_COPING_3 -> R.string.phase_fluctuation_coping_3
    PhaseTextKey.FLUCTUATION_COPING_4 -> R.string.phase_fluctuation_coping_4

    PhaseTextKey.REPAIR_NAME -> R.string.phase_repair_name
    PhaseTextKey.REPAIR_HEADLINE -> R.string.phase_repair_headline
    PhaseTextKey.REPAIR_EXPECTATION_1 -> R.string.phase_repair_expectation_1
    PhaseTextKey.REPAIR_EXPECTATION_2 -> R.string.phase_repair_expectation_2
    PhaseTextKey.REPAIR_EXPECTATION_3 -> R.string.phase_repair_expectation_3
    PhaseTextKey.REPAIR_COPING_1 -> R.string.phase_repair_coping_1
    PhaseTextKey.REPAIR_COPING_2 -> R.string.phase_repair_coping_2
    PhaseTextKey.REPAIR_COPING_3 -> R.string.phase_repair_coping_3

    PhaseTextKey.RECONNECT_NAME -> R.string.phase_reconnect_name
    PhaseTextKey.RECONNECT_HEADLINE -> R.string.phase_reconnect_headline
    PhaseTextKey.RECONNECT_EXPECTATION_1 -> R.string.phase_reconnect_expectation_1
    PhaseTextKey.RECONNECT_EXPECTATION_2 -> R.string.phase_reconnect_expectation_2
    PhaseTextKey.RECONNECT_COPING_1 -> R.string.phase_reconnect_coping_1
    PhaseTextKey.RECONNECT_COPING_2 -> R.string.phase_reconnect_coping_2
    PhaseTextKey.RECONNECT_COPING_3 -> R.string.phase_reconnect_coping_3
    PhaseTextKey.RECONNECT_CAUTION -> R.string.phase_reconnect_caution

    PhaseTextKey.CONSOLIDATION_NAME -> R.string.phase_consolidation_name
    PhaseTextKey.CONSOLIDATION_HEADLINE -> R.string.phase_consolidation_headline
    PhaseTextKey.CONSOLIDATION_EXPECTATION_1 -> R.string.phase_consolidation_expectation_1
    PhaseTextKey.CONSOLIDATION_EXPECTATION_2 -> R.string.phase_consolidation_expectation_2
    PhaseTextKey.CONSOLIDATION_COPING_1 -> R.string.phase_consolidation_coping_1
    PhaseTextKey.CONSOLIDATION_COPING_2 -> R.string.phase_consolidation_coping_2
    PhaseTextKey.CONSOLIDATION_COPING_3 -> R.string.phase_consolidation_coping_3
}

@StringRes
fun phaseNameRes(phase: WithdrawalPhase): Int = phaseTextRes(phase.nameKey)

@StringRes
fun phaseHeadlineRes(phase: WithdrawalPhase): Int = phaseTextRes(phase.headlineKey)

@StringRes
fun phaseNoteRes(note: PhaseNote): Int = phaseTextRes(note.textKey)

/** 阶段没有 caution 时返回 null（由界面决定不渲染警示条）。 */
@StringRes
fun phaseCautionRes(phase: WithdrawalPhase): Int? = phase.cautionKey?.let(::phaseTextRes)

/** 阶段天数区间文案：闭合区间用 `第 %1$d–%2$d 天`，无上界用 `第 %1$d 天起`。 */
@StringRes
fun phaseDayRangeRes(phase: WithdrawalPhase): Int =
    if (phase.maxDay == null) R.string.phase_day_range_open else R.string.phase_day_range_closed

/** 与 [phaseDayRangeRes] 配套的天数参数。 */
fun phaseDayRangeArgs(phase: WithdrawalPhase): List<Any> {
    // 取到局部变量再判空：避免依赖跨模块属性的 smart cast。
    val maxDay = phase.maxDay

    return if (maxDay == null) listOf(phase.minDay) else listOf(phase.minDay, maxDay)
}

/** 卡片底部的求助提示（所有阶段共用）。 */
@StringRes
fun phaseHelpSeekingNoticeRes(): Int = R.string.phase_help_seeking_notice
