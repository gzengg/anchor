package com.anchor.recovery.core.assessment

/**
 * F7 计分：总分分级 + 五个维度小计。
 *
 * 分级阈值是**自研参考阈值**（不是经过临床验证的 cut-off），UI 必须如实标注。
 */
enum class CsbdLevel(val label: String, val rangeText: String, val advice: String) {
    LOW(
        label = "低风险",
        rangeText = "< 20",
        advice = "当前自评分数较低。继续如实记录、保持已有的作息与支持关系即可。",
    ),
    WATCH(
        label = "需关注",
        rangeText = "20 – 39",
        advice = "有些方面已经在消耗你的时间和情绪。可以先用 App 里的渴求工具与打卡记录观察规律，" +
            "如果持续困扰，建议咨询专业人士。",
    ),
    HIGH(
        label = "高风险",
        rangeText = "40 – 55",
        advice = "自评分数偏高，说明这件事可能已经带来明显影响。建议咨询心理咨询师或精神科医生，" +
            "把你的记录一并带去讨论。",
    ),
    URGENT(
        label = "强烈建议就医",
        rangeText = "≥ 56",
        advice = "自评分数很高。请考虑尽快就诊（精神科 / 心理科 / 成瘾医学门诊），" +
            "这不是靠意志力硬扛的问题，专业评估会更有帮助。",
    ),
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

    /** 阈值口径说明，随结果一起展示。 */
    val thresholdNote: String
        get() = "分级阈值（${CsbdLevel.LOW.rangeText} / ${CsbdLevel.WATCH.rangeText} / " +
            "${CsbdLevel.HIGH.rangeText} / ${CsbdLevel.URGENT.rangeText}）为自研参考阈值，未经临床验证。"
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
