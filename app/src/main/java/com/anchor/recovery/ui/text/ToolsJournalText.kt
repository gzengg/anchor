package com.anchor.recovery.ui.text

import androidx.annotation.StringRes
import com.anchor.recovery.R
import com.anchor.recovery.core.model.AssessmentType
import com.anchor.recovery.core.model.UrgeTool
import com.anchor.recovery.core.relapse.DayPart
import com.anchor.recovery.core.urge.BreathingCue
import com.anchor.recovery.core.urge.UrgeSurfingStage

/**
 * `:core` 工具 / 日志侧枚举 → 展示文案资源的映射。
 *
 * `:core` 不持有中文文案（它连 `android.*` 都不能依赖），只保留阶段顺序、时段口径与枚举名。
 * 文案在 `values/strings_tools_journal.xml`。这里的 `when` 不写 `else`（枚举分支）：
 * `:core` 新增枚举值时编译期就会失败，避免出现「有状态没文案」。
 */

@StringRes
fun urgeSurfingStageLabelRes(stage: UrgeSurfingStage): Int = when (stage) {
    UrgeSurfingStage.INTRO -> R.string.urge_stage_label_intro
    UrgeSurfingStage.BREATHE -> R.string.urge_stage_label_breathe
    UrgeSurfingStage.OBSERVE -> R.string.urge_stage_label_observe
    UrgeSurfingStage.RIDE -> R.string.urge_stage_label_ride
    UrgeSurfingStage.RATE_AGAIN -> R.string.urge_stage_label_rate_again
    UrgeSurfingStage.DONE -> R.string.urge_stage_label_done
}

@StringRes
fun urgeSurfingStageHintRes(stage: UrgeSurfingStage): Int = when (stage) {
    UrgeSurfingStage.INTRO -> R.string.urge_stage_hint_intro
    UrgeSurfingStage.BREATHE -> R.string.urge_stage_hint_breathe
    UrgeSurfingStage.OBSERVE -> R.string.urge_stage_hint_observe
    UrgeSurfingStage.RIDE -> R.string.urge_stage_hint_ride
    UrgeSurfingStage.RATE_AGAIN -> R.string.urge_stage_hint_rate_again
    UrgeSurfingStage.DONE -> R.string.urge_stage_hint_done
}

/**
 * 呼吸相位文案。[phase] 是 `:core` 的 Int 常量（不是枚举，无法穷尽），
 * 未知相位与 `:core` 原实现保持一致，按呼气渲染。
 */
@StringRes
fun breathingPhaseLabelRes(phase: Int): Int = when (phase) {
    BreathingCue.PHASE_IN -> R.string.breathing_phase_in
    BreathingCue.PHASE_HOLD -> R.string.breathing_phase_hold
    else -> R.string.breathing_phase_out
}

/** 时段完整区间名；当前界面只渲染紧凑名 [dayPartShortRes]，区间口径一并保留在资源里。 */
@StringRes
fun dayPartLabelRes(part: DayPart): Int = when (part) {
    DayPart.MORNING -> R.string.daypart_morning
    DayPart.AFTERNOON -> R.string.daypart_afternoon
    DayPart.EVENING -> R.string.daypart_evening
    DayPart.LATE_NIGHT -> R.string.daypart_late_night
}

/** 时段紧凑名（"上午 05–11" → "上午"），统计卡用。 */
@StringRes
fun dayPartShortRes(part: DayPart): Int = when (part) {
    DayPart.MORNING -> R.string.daypart_morning_short
    DayPart.AFTERNOON -> R.string.daypart_afternoon_short
    DayPart.EVENING -> R.string.daypart_evening_short
    DayPart.LATE_NIGHT -> R.string.daypart_late_night_short
}

@StringRes
fun urgeToolLabelRes(tool: UrgeTool): Int = when (tool) {
    UrgeTool.URGE_SURFING -> R.string.urge_tool_label_surfing
    UrgeTool.DELAY_TOOL -> R.string.urge_tool_label_delay
}

@StringRes
fun assessmentTypeLabelRes(type: AssessmentType): Int = when (type) {
    AssessmentType.CSBD -> R.string.assessment_type_label_csbd
    AssessmentType.MORAL -> R.string.assessment_type_label_moral
}
