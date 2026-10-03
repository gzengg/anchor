package com.anchor.recovery.core.assessment

/**
 * F7 自我筛查问卷的题项（:core 常量，供计分与 UI 共用）。
 *
 * 题项为**参照 WHO ICD-11 对强迫性性行为障碍（CSBD）的描述与 CSBD-19 量表的五个维度自行撰写**，
 * 不是 CSBD-19 原版题项，也不与任何已发布量表逐字对应（原版量表有版权）。
 * 因此本问卷未经过信效度验证，只能作为自评参考，不能用于诊断。
 *
 * 维度名与题干是用户可见文案，已外移到 `:app`（`QuestionnaireText`，资源在
 * `values/strings_questionnaire.xml`）；本模块只保留维度归属、题号与计分口径。
 */
enum class CsbdDimension(val questionCount: Int) {
    CONTROL(4),
    COPING(4),
    COMPULSIVE(4),
    CONSEQUENCE(4),
    ATTEMPTS(3),
}

/**
 * 一道题。[reverseScored] 恒为 false：本问卷不使用反向计分题，让分数含义对使用者保持直白。
 *
 * 题干不在这里：题号 [id]（1..19）是稳定映射键，`QuestionnaireText.csbdQuestionRes(id)` 据此取文案。
 */
data class CsbdQuestion(
    val id: Int,
    val dimension: CsbdDimension,
    val reverseScored: Boolean = false,
)

object CsbdQuestionnaire {

    /** 每题可选分值区间 0–4（0 完全不符合 … 4 完全符合）。 */
    const val MIN_PER_ITEM = 0
    const val MAX_PER_ITEM = 4

    val questions: List<CsbdQuestion> = listOf(
        // 控制失控
        q(1, CsbdDimension.CONTROL),
        q(2, CsbdDimension.CONTROL),
        q(3, CsbdDimension.CONTROL),
        q(4, CsbdDimension.CONTROL),
        // 情绪应对
        q(5, CsbdDimension.COPING),
        q(6, CsbdDimension.COPING),
        q(7, CsbdDimension.COPING),
        q(8, CsbdDimension.COPING),
        // 强迫性
        q(9, CsbdDimension.COMPULSIVE),
        q(10, CsbdDimension.COMPULSIVE),
        q(11, CsbdDimension.COMPULSIVE),
        q(12, CsbdDimension.COMPULSIVE),
        // 负面后果
        q(13, CsbdDimension.CONSEQUENCE),
        q(14, CsbdDimension.CONSEQUENCE),
        q(15, CsbdDimension.CONSEQUENCE),
        q(16, CsbdDimension.CONSEQUENCE),
        // 尝试失败
        q(17, CsbdDimension.ATTEMPTS),
        q(18, CsbdDimension.ATTEMPTS),
        q(19, CsbdDimension.ATTEMPTS),
    )

    val questionCount: Int get() = questions.size

    val maxTotalScore: Int get() = questionCount * MAX_PER_ITEM

    private fun q(id: Int, dimension: CsbdDimension) =
        CsbdQuestion(id = id, dimension = dimension)
}
