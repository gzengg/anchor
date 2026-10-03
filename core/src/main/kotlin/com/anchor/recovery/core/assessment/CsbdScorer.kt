package com.anchor.recovery.core.assessment

/**
 * F7 计分：总分分级 + 五个维度小计。
 *
 * 分级阈值是**自研参考阈值**（不是经过临床验证的 cut-off），UI 必须如实标注。
 * 等级名 / 分区间 / 建议都是用户可见文案，已外移到 `:app`（`QuestionnaireText` + `strings_questionnaire.xml`）。
 */
enum class CsbdLevel {
    LOW,
    WATCH,
    HIGH,
    URGENT,
    ;

    companion object {
        /** 自研阈值：<20 / 20–39 / 40–55 / ≥56。 */
        fun of(totalScore: Int): CsbdLevel = when {
            totalScore < 20 -> LOW
            totalScore < 40 -> WATCH
            totalScore < 56 -> HIGH
            else -> URGENT
        }
    }
}

data class CsbdResult(
    val totalScore: Int,
    val level: CsbdLevel,
    val dimensionScores: Map<CsbdDimension, Int>,
    val answers: List<Int>,
) {
    /** 得分最高的维度（并列时取枚举顺序靠前者），用于结果页给出针对性表述。 */
    val topDimension: CsbdDimension
        get() = CsbdDimension.entries.maxBy { dimensionScores[it] ?: 0 }

    val topDimensionScore: Int get() = dimensionScores[topDimension] ?: 0
}

object CsbdScorer {

    /**
     * @throws IllegalArgumentException 作答数量与题项不一致，或存在超出 0–4 的取值。
     */
    fun score(answers: List<Int>): CsbdResult {
        val expected = CsbdQuestionnaire.questionCount
        require(answers.size == expected) {
            "作答数量应为 $expected 题，实际 ${answers.size} 题"
        }
        answers.forEachIndexed { index, value ->
            require(value in CsbdQuestionnaire.MIN_PER_ITEM..CsbdQuestionnaire.MAX_PER_ITEM) {
                "第 ${index + 1} 题答案 $value 超出 " +
                    "${CsbdQuestionnaire.MIN_PER_ITEM}–${CsbdQuestionnaire.MAX_PER_ITEM} 范围"
            }
        }

        val byDimension = CsbdDimension.entries.associateWith { dimension ->
            CsbdQuestionnaire.questions
                .filter { it.dimension == dimension }
                .sumOf { question ->
                    val raw = answers[question.id - 1]
                    if (question.reverseScored) {
                        CsbdQuestionnaire.MAX_PER_ITEM - raw
                    } else {
                        raw
                    }
                }
        }
        // 维度小计之和必然等于总分，测试据此交叉校验。
        val total = byDimension.values.sum()
        return CsbdResult(
            totalScore = total,
            level = CsbdLevel.of(total),
            dimensionScores = byDimension,
            answers = answers.toList(),
        )
    }
}
