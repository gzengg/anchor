package com.anchor.recovery.core.assessment

/**
 * F7 自我筛查问卷的题项（:core 常量，供计分与 UI 共用）。
 *
 * 题项为**参照 WHO ICD-11 对强迫性性行为障碍（CSBD）的描述与 CSBD-19 量表的五个维度自行撰写**，
 * 不是 CSBD-19 原版题项，也不与任何已发布量表逐字对应（原版量表有版权）。
 * 因此本问卷未经过信效度验证，只能作为自评参考，不能用于诊断。
 */
enum class CsbdDimension(val label: String, val questionCount: Int) {
    CONTROL("控制失控", 4),
    COPING("情绪应对", 4),
    COMPULSIVE("强迫性", 4),
    CONSEQUENCE("负面后果", 4),
    ATTEMPTS("尝试失败", 3),
}

/**
 * 一道题。[reverseScored] 恒为 false：本问卷不使用反向计分题，让分数含义对使用者保持直白。
 */
data class CsbdQuestion(
    val id: Int,
    val dimension: CsbdDimension,
    val text: String,
    val reverseScored: Boolean = false,
)

object CsbdQuestionnaire {

    /** 每题可选分值区间 0–4（0 完全不符合 … 4 完全符合）。 */
    const val MIN_PER_ITEM = 0
    const val MAX_PER_ITEM = 4

    /** 作答时间范围提示，写在问卷 intro 页。 */
    const val TIME_WINDOW = "过去 6 个月"

    /** 卷首说明：必须随题项一起展示。 */
    const val DISCLAIMER =
        "本问卷为自评参考工具，非临床诊断；题项参照 CSBD 相关维度自行撰写，" +
            "不是 CSBD-19 原版，未经验证。如结果让你担心，建议咨询专业人士或考虑就诊。"

    val questions: List<CsbdQuestion> = listOf(
        // 控制失控
        q(1, CsbdDimension.CONTROL, "我常把大量时间花在与性 / 色情内容相关的想法或行为上，超出原本的打算。"),
        q(2, CsbdDimension.CONTROL, "我很难控制自己开始或停下这类行为。"),
        q(3, CsbdDimension.CONTROL, "明知当时有更要紧的事，我仍会先去做这件事。"),
        q(4, CsbdDimension.CONTROL, "我常在事后才发现，自己又在这件事上花了好几个小时。"),
        // 情绪应对
        q(5, CsbdDimension.COPING, "心情低落、焦虑或孤独时，我会靠这类内容让自己好受一点。"),
        q(6, CsbdDimension.COPING, "遇到压力大的事情，我会用这类内容逃避一会儿。"),
        q(7, CsbdDimension.COPING, "无聊或无事可做时，我最容易去做这件事。"),
        q(8, CsbdDimension.COPING, "做完之后情绪并没有真的变好，反而更沉。"),
        // 强迫性
        q(9, CsbdDimension.COMPULSIVE, "相关念头会反复出现在脑海里，很难让它停下来。"),
        q(10, CsbdDimension.COMPULSIVE, "我减少或停止过这类行为，但过一段时间又会重新开始。"),
        q(11, CsbdDimension.COMPULSIVE, "越是想控制，这类念头反而越强烈。"),
        q(12, CsbdDimension.COMPULSIVE, "在不太合适的场合（如工作、学习时），我也会想着这件事。"),
        // 负面后果
        q(13, CsbdDimension.CONSEQUENCE, "这件事已经影响到我的睡眠、精力或日常作息。"),
        q(14, CsbdDimension.CONSEQUENCE, "我因此耽误过工作、学习或本该完成的事。"),
        q(15, CsbdDimension.CONSEQUENCE, "我因此疏远或伤害了身边的人（伴侣、家人、朋友）。"),
        q(16, CsbdDimension.CONSEQUENCE, "我在这件事上花的时间，已经影响了我本来重视的目标。"),
        // 尝试失败
        q(17, CsbdDimension.ATTEMPTS, "我尝试过减少或停止，但没有成功。"),
        q(18, CsbdDimension.ATTEMPTS, "我曾因为这件事对自己感到失望或厌恶。"),
        q(19, CsbdDimension.ATTEMPTS, "我需要更强或更多的刺激，才能获得和以前一样的感觉。"),
    )

    val questionCount: Int get() = questions.size

    val maxTotalScore: Int get() = questionCount * MAX_PER_ITEM

    private fun q(id: Int, dimension: CsbdDimension, text: String) =
        CsbdQuestion(id = id, dimension = dimension, text = text)
}
