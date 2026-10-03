package com.anchor.recovery.core.assessment

/**
 * F8 题项：基于 Grubbs 的"道德不一致（moral incongruence）"研究思路，
 * 把"行为上的实际影响"与"价值观层面的冲突"分成两组各 6 题分别计分。
 *
 * 目的是把两类问题区分开：行为影响大不等于成瘾，价值观冲突也不等于成瘾。
 * 题项为自撰，阈值同样是自研参考值。
 */
enum class MoralItemKind(val label: String, val questionCount: Int) {
    BEHAVIOR("行为影响", 6),
    MORAL("价值观冲突", 6),
}

data class MoralQuestion(
    val id: Int,
    val kind: MoralItemKind,
    val text: String,
)

object MoralIncongruenceScale {

    const val MIN_PER_ITEM = 0
    const val MAX_PER_ITEM = 4

    /** 单维度判定为"偏高"的自研阈值（维度满分 24）。 */
    const val HIGH_THRESHOLD = 12

    const val TIME_WINDOW = "过去 1 个月"

    const val DISCLAIMER =
        "本问卷为自评参考工具，非临床诊断；题项参照道德不一致研究方向自行撰写，未经验证。" +
            "它不判断你是否成瘾，只帮助你看清“行为影响”与“价值观冲突”各自的分量。"

    val questions: List<MoralQuestion> = listOf(
        // 行为影响
        mq(1, MoralItemKind.BEHAVIOR, "最近一个月，我在这件事上花的时间比我期望的多。"),
        mq(2, MoralItemKind.BEHAVIOR, "这件事的频率已经影响到我的日常安排。"),
        mq(3, MoralItemKind.BEHAVIOR, "我在并不想做的时候仍然去做了。"),
        mq(4, MoralItemKind.BEHAVIOR, "我因为这件事推迟了原本要完成的事情。"),
        mq(5, MoralItemKind.BEHAVIOR, "我很难在一段时间内完全不接触这类内容。"),
        mq(6, MoralItemKind.BEHAVIOR, "这件事在我生活里的比重，比我愿意承认的更大。"),
        // 价值观冲突
        mq(7, MoralItemKind.MORAL, "做完之后，我会觉得自己违背了重要的价值观。"),
        mq(8, MoralItemKind.MORAL, "我觉得这件事与我的信仰或原则不相容。"),
        mq(9, MoralItemKind.MORAL, "我担心如果别人知道，会对我改变看法。"),
        mq(10, MoralItemKind.MORAL, "我会因为这件事而觉得自己不是一个好人。"),
        mq(11, MoralItemKind.MORAL, "我曾向伴侣或家人隐瞒这件事的程度或频率。"),
        mq(12, MoralItemKind.MORAL, "我在这件事上的行为，与我想成为的人并不一致。"),
    )

    val questionCount: Int get() = questions.size

    val maxScorePerKind: Int get() = MoralItemKind.BEHAVIOR.questionCount * MAX_PER_ITEM

    fun ofKind(kind: MoralItemKind): List<MoralQuestion> = questions.filter { it.kind == kind }

    private fun mq(id: Int, kind: MoralItemKind, text: String) =
        MoralQuestion(id = id, kind = kind, text = text)
}
