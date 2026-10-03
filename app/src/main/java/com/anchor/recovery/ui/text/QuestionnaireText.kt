package com.anchor.recovery.ui.text

import androidx.annotation.StringRes
import com.anchor.recovery.R
import com.anchor.recovery.core.assessment.CsbdDimension
import com.anchor.recovery.core.assessment.CsbdLevel
import com.anchor.recovery.core.assessment.MoralItemKind
import com.anchor.recovery.core.assessment.MoralQuadrant

/**
 * 问卷与计分文案（`:core` 的题干 / 维度名 / 等级 / 象限）→ 展示文案资源的映射。
 *
 * `:core` 不持有中文文案（它连 `android.*` 都不能依赖），只给出稳定题号、维度归属与计分口径；
 * 文案在 `values/strings_questionnaire.xml`。枚举映射的 `when` 不写 `else`：`:core` 新增维度 /
 * 等级 / 象限时编译期就会失败，避免出现「有状态没文案」。
 *
 * 题号是 `Int`，编译器无法判定穷尽，因此题干映射用语句式 `when`（同样不写 `else`）在所有分支
 * 之外 fail-fast：题号来自 `:core` 自己的题项列表，漏配资源属于代码错误，宁可快速失败也不静默
 * 显示占位文案。
 */

@StringRes
fun csbdDimensionRes(dimension: CsbdDimension): Int = when (dimension) {
    CsbdDimension.CONTROL -> R.string.csbd_dimension_control
    CsbdDimension.COPING -> R.string.csbd_dimension_coping
    CsbdDimension.COMPULSIVE -> R.string.csbd_dimension_compulsive
    CsbdDimension.CONSEQUENCE -> R.string.csbd_dimension_consequence
    CsbdDimension.ATTEMPTS -> R.string.csbd_dimension_attempts
}

@StringRes
fun moralItemKindRes(kind: MoralItemKind): Int = when (kind) {
    MoralItemKind.BEHAVIOR -> R.string.moral_kind_behavior
    MoralItemKind.MORAL -> R.string.moral_kind_moral
}

@StringRes
fun csbdLevelLabelRes(level: CsbdLevel): Int = when (level) {
    CsbdLevel.LOW -> R.string.csbd_level_low_label
    CsbdLevel.WATCH -> R.string.csbd_level_watch_label
    CsbdLevel.HIGH -> R.string.csbd_level_high_label
    CsbdLevel.URGENT -> R.string.csbd_level_urgent_label
}

@StringRes
fun csbdLevelRangeRes(level: CsbdLevel): Int = when (level) {
    CsbdLevel.LOW -> R.string.csbd_level_low_range
    CsbdLevel.WATCH -> R.string.csbd_level_watch_range
    CsbdLevel.HIGH -> R.string.csbd_level_high_range
    CsbdLevel.URGENT -> R.string.csbd_level_urgent_range
}

@StringRes
fun csbdLevelAdviceRes(level: CsbdLevel): Int = when (level) {
    CsbdLevel.LOW -> R.string.csbd_level_low_advice
    CsbdLevel.WATCH -> R.string.csbd_level_watch_advice
    CsbdLevel.HIGH -> R.string.csbd_level_high_advice
    CsbdLevel.URGENT -> R.string.csbd_level_urgent_advice
}

@StringRes
fun moralQuadrantLabelRes(quadrant: MoralQuadrant): Int = when (quadrant) {
    MoralQuadrant.HIGH_BOTH -> R.string.moral_quadrant_high_both_label
    MoralQuadrant.HIGH_BEHAVIOR_ONLY -> R.string.moral_quadrant_high_behavior_only_label
    MoralQuadrant.HIGH_MORAL_ONLY -> R.string.moral_quadrant_high_moral_only_label
    MoralQuadrant.LOW_BOTH -> R.string.moral_quadrant_low_both_label
}

@StringRes
fun moralQuadrantInterpretationRes(quadrant: MoralQuadrant): Int = when (quadrant) {
    MoralQuadrant.HIGH_BOTH -> R.string.moral_quadrant_high_both_interpretation
    MoralQuadrant.HIGH_BEHAVIOR_ONLY -> R.string.moral_quadrant_high_behavior_only_interpretation
    MoralQuadrant.HIGH_MORAL_ONLY -> R.string.moral_quadrant_high_moral_only_interpretation
    MoralQuadrant.LOW_BOTH -> R.string.moral_quadrant_low_both_interpretation
}

/** F7 题干：题号 1..19，与 `CsbdQuestionnaire.questions` 一一对应。 */
@StringRes
fun csbdQuestionRes(id: Int): Int {
    when (id) {
        1 -> return R.string.csbd_q_1
        2 -> return R.string.csbd_q_2
        3 -> return R.string.csbd_q_3
        4 -> return R.string.csbd_q_4
        5 -> return R.string.csbd_q_5
        6 -> return R.string.csbd_q_6
        7 -> return R.string.csbd_q_7
        8 -> return R.string.csbd_q_8
        9 -> return R.string.csbd_q_9
        10 -> return R.string.csbd_q_10
        11 -> return R.string.csbd_q_11
        12 -> return R.string.csbd_q_12
        13 -> return R.string.csbd_q_13
        14 -> return R.string.csbd_q_14
        15 -> return R.string.csbd_q_15
        16 -> return R.string.csbd_q_16
        17 -> return R.string.csbd_q_17
        18 -> return R.string.csbd_q_18
        19 -> return R.string.csbd_q_19
    }
    error("Unknown CSBD question id: $id")
}

/** F8 题干：题号 1..12，与 `MoralIncongruenceScale.questions` 一一对应。 */
@StringRes
fun moralQuestionRes(id: Int): Int {
    when (id) {
        1 -> return R.string.moral_q_1
        2 -> return R.string.moral_q_2
        3 -> return R.string.moral_q_3
        4 -> return R.string.moral_q_4
        5 -> return R.string.moral_q_5
        6 -> return R.string.moral_q_6
        7 -> return R.string.moral_q_7
        8 -> return R.string.moral_q_8
        9 -> return R.string.moral_q_9
        10 -> return R.string.moral_q_10
        11 -> return R.string.moral_q_11
        12 -> return R.string.moral_q_12
    }
    error("Unknown moral question id: $id")
}
