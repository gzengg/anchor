package com.anchor.recovery.core.assessment

/**
 * F8 四象限：行为影响（高 / 低）× 价值观冲突（高 / 低）。
 *
 * 文案刻意避免病理化：价值观冲突本身就是常见且值得讨论的困扰，不等于成瘾。
 */
enum class MoralQuadrant(val label: String, val interpretation: String) {
    HIGH_BOTH(
        label = "行为影响偏高 + 价值观冲突偏高",
        interpretation = "两边都不轻：行为已经带来实际影响，同时伴随明显的自我冲突。" +
            "这一类最值得尽早找人聊一聊（心理咨询师或精神科医生都可以）。",
    ),
    HIGH_BEHAVIOR_ONLY(
        label = "行为影响偏高 + 价值观冲突偏低",
        interpretation = "困扰更集中在实际影响上：你并不为价值观层面自责，但时间和生活安排确实被占用了。" +
            "可以从记录规律、设定边界开始。",
    ),
    HIGH_MORAL_ONLY(
        label = "行为影响偏低 + 价值观冲突偏高",
        interpretation = "痛苦更多来自自我评价：行为本身的影响有限，但你对它的道德评价很重。" +
            "价值观冲突不等于成瘾，也不等于你的人格有问题；这类冲突本身就可以作为议题去谈。",
    ),
    LOW_BOTH(
        label = "行为影响偏低 + 价值观冲突偏低",
        interpretation = "两项都不高：目前记录到的行为影响与价值观冲突都不明显。" +
            "如果仍有说不清的困扰，依然可以和专业人士讨论。",
    ),
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

    /** 阈值口径说明，随结果一起展示。 */
    val thresholdNote: String
        get() = "判定口径：每个维度满分 ${MoralIncongruenceScale.maxScorePerKind} 分，" +
            "≥ ${MoralIncongruenceScale.HIGH_THRESHOLD} 分记为偏高——这是自研参考阈值，未经临床验证。"
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
