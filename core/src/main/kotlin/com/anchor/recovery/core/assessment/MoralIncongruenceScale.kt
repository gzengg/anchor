package com.anchor.recovery.core.assessment

/**
 * F8 题项：基于 Grubbs 的"道德不一致（moral incongruence）"研究思路，
 * 把"行为上的实际影响"与"价值观层面的冲突"分成两组各 6 题分别计分。
 *
 * 目的是把两类问题区分开：行为影响大不等于成瘾，价值观冲突也不等于成瘾。
 * 题项为自撰，阈值同样是自研参考值。
 *
 * 维度名与题干是用户可见文案，已外移到 `:app`（`QuestionnaireText`，资源在
 * `values/strings_questionnaire.xml`）；时间窗与免责说明同理。
 */
enum class MoralItemKind(val questionCount: Int) {
    BEHAVIOR(6),
    MORAL(6),
}

/**
 * 一道题。题号 [id]（1..12）是稳定映射键，`QuestionnaireText.moralQuestionRes(id)` 据此取题干。
 */
data class MoralQuestion(
    val id: Int,
    val kind: MoralItemKind,
)

object MoralIncongruenceScale {

    const val MIN_PER_ITEM = 0
    const val MAX_PER_ITEM = 4

    /** 单维度判定为"偏高"的自研阈值（维度满分 24）。 */
    const val HIGH_THRESHOLD = 12

    val questions: List<MoralQuestion> = listOf(
        // 行为影响
        mq(1, MoralItemKind.BEHAVIOR),
        mq(2, MoralItemKind.BEHAVIOR),
        mq(3, MoralItemKind.BEHAVIOR),
        mq(4, MoralItemKind.BEHAVIOR),
        mq(5, MoralItemKind.BEHAVIOR),
        mq(6, MoralItemKind.BEHAVIOR),
        // 价值观冲突
        mq(7, MoralItemKind.MORAL),
        mq(8, MoralItemKind.MORAL),
        mq(9, MoralItemKind.MORAL),
        mq(10, MoralItemKind.MORAL),
        mq(11, MoralItemKind.MORAL),
        mq(12, MoralItemKind.MORAL),
    )

    val questionCount: Int get() = questions.size

    val maxScorePerKind: Int get() = MoralItemKind.BEHAVIOR.questionCount * MAX_PER_ITEM

    fun ofKind(kind: MoralItemKind): List<MoralQuestion> = questions.filter { it.kind == kind }

    private fun mq(id: Int, kind: MoralItemKind) =
        MoralQuestion(id = id, kind = kind)
}
