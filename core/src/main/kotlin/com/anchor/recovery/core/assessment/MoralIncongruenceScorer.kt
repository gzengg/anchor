package com.anchor.recovery.core.assessment

/**
 * F8 四象限：行为影响（高 / 低）× 价值观冲突（高 / 低）。
 *
 * 文案刻意避免病理化：价值观冲突本身就是常见且值得讨论的困扰，不等于成瘾。
 * 象限名与解读是用户可见文案，已外移到 `:app`（`QuestionnaireText` + `strings_questionnaire.xml`）。
 */
enum class MoralQuadrant {
    HIGH_BOTH,
    HIGH_BEHAVIOR_ONLY,
    HIGH_MORAL_ONLY,
    LOW_BOTH,
    ;

    companion object {
        fun of(behaviorHigh: Boolean, moralHigh: Boolean): MoralQuadrant = when {
            behaviorHigh && moralHigh -> HIGH_BOTH
            behaviorHigh -> HIGH_BEHAVIOR_ONLY
            moralHigh -> HIGH_MORAL_ONLY
            else -> LOW_BOTH
        }
    }
}

data class MoralResult(
    val behaviorScore: Int,
    val moralScore: Int,
    val quadrant: MoralQuadrant,
    val answers: List<Int>,
) {
    val behaviorHigh: Boolean get() = behaviorScore >= MoralIncongruenceScale.HIGH_THRESHOLD

    val moralHigh: Boolean get() = moralScore >= MoralIncongruenceScale.HIGH_THRESHOLD
}

object MoralIncongruenceScorer {

    /**
     * 前 6 题是行为影响，后 6 题是价值观冲突（顺序与 [MoralIncongruenceScale.questions] 一致）。
     *
     * @throws IllegalArgumentException 作答数量与题项不一致，或存在超出 0–4 的取值。
     */
    fun score(answers: List<Int>): MoralResult {
        val expected = MoralIncongruenceScale.questionCount
        require(answers.size == expected) {
            "作答数量应为 $expected 题，实际 ${answers.size} 题"
        }
        answers.forEachIndexed { index, value ->
            require(value in MoralIncongruenceScale.MIN_PER_ITEM..MoralIncongruenceScale.MAX_PER_ITEM) {
                "第 ${index + 1} 题答案 $value 超出 " +
                    "${MoralIncongruenceScale.MIN_PER_ITEM}–${MoralIncongruenceScale.MAX_PER_ITEM} 范围"
            }
        }

        val behaviorScore = scoreOf(answers, MoralItemKind.BEHAVIOR)
        val moralScore = scoreOf(answers, MoralItemKind.MORAL)
        return MoralResult(
            behaviorScore = behaviorScore,
            moralScore = moralScore,
            quadrant = MoralQuadrant.of(
                behaviorHigh = behaviorScore >= MoralIncongruenceScale.HIGH_THRESHOLD,
                moralHigh = moralScore >= MoralIncongruenceScale.HIGH_THRESHOLD,
            ),
            answers = answers.toList(),
        )
    }

    private fun scoreOf(answers: List<Int>, kind: MoralItemKind): Int =
        MoralIncongruenceScale.ofKind(kind).sumOf { question -> answers[question.id - 1] }
}
