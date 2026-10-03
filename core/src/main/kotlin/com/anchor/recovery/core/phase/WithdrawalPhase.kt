package com.anchor.recovery.core.phase

/**
 * 一条带出处的阶段文案。
 *
 * [sourceArticleIds] 指向 `assets/content/articles.json` 里的文章 id，
 * 由 `WithdrawalPhaseResolverTest` / `WithdrawalPhaseContentTest` 校验必须真实存在。
 */
data class PhaseNote(
    val text: String,
    val sourceArticleIds: List<String>,
)

/**
 * 戒断阶段（F2）。天数边界为闭区间；[maxDay] 为 null 表示没有上限（最后一个阶段）。
 */
data class WithdrawalPhase(
    val id: String,
    val name: String,
    val minDay: Int,
    val maxDay: Int?,
    val headline: String,
    val expectation: List<PhaseNote>,
    val coping: List<PhaseNote>,
    val caution: String? = null,
) {
    fun contains(dayNumber: Int): Boolean =
        dayNumber >= minDay && (maxDay == null || dayNumber <= maxDay)

    val dayLabel: String
        get() = if (maxDay == null) "第 $minDay 天起" else "第 $minDay–$maxDay 天"

    /** 阶段内全部引用到的文章 id（去重，保持出现顺序）。 */
    val sourceArticleIds: List<String>
        get() = (expectation + coping).flatMap { it.sourceArticleIds }.distinct()

    /** 该阶段的天数跨度；无上限阶段返回 null。 */
    val daySpan: Int?
        get() = maxDay?.let { it - minDay + 1 }
}

/**
 * 五个阶段的文案目录。
 *
 * 时间线（症状与主观经验）提炼自 `anchor-reference-docs/05-cases/`（社区自我报告与定性研究），
 * 应对技术另引 `04-methods/` 里的干预研究。社区来源（cases-006/007/008：YBOP、NoFap）
 * 自我报告性质、无对照验证，故文案中一律标明"自我报告/社区经验"，不做医学结论。
 */
object WithdrawalPhaseCatalog {

    const val HELP_SEEKING_NOTICE = "本 App 只提供自助记录与科普，不能替代诊疗。若痛苦持续加重，请咨询专业医生。"

    val phases: List<WithdrawalPhase> = listOf(
        WithdrawalPhase(
            id = "ACUTE",
            name = "急性期",
            minDay = 0,
            maxDay = 7,
            headline = "最强烈的反应通常出现在开头几天：这不是“意志力不够”，而是身体与注意力系统正在重新适应。",
            expectation = listOf(
                PhaseNote(
                    text = "常见的自我报告：入睡困难、焦虑烦躁、注意力涣散（脑雾）、情绪波动、头痛或肌肉紧绷。",
                    sourceArticleIds = listOf("cases-006"),
                ),
                PhaseNote(
                    text = "前 5 天性欲可能不降反升，随后常突然转为性欲低下的“平坦期”；这种起伏本身是过程的一部分。",
                    sourceArticleIds = listOf("cases-007"),
                ),
                PhaseNote(
                    text = "突然浮现的画面（闪回）与性梦在这一阶段很常见，出现闪回并不代表失败。",
                    sourceArticleIds = listOf("cases-006", "cases-007"),
                ),
            ),
            coping = listOf(
                PhaseNote(
                    text = "把戒断反应预期成过程的一部分：事先知道它会来，能明显降低“我是不是坏了”的二次恐慌。",
                    sourceArticleIds = listOf("cases-006"),
                ),
                PhaseNote(
                    text = "社区经验反复提到酒精与“先看一眼、再越陷越深”的路径是复发高危因素，提前避开。",
                    sourceArticleIds = listOf("cases-006"),
                ),
                PhaseNote(
                    text = "为深夜与独处时段准备一个替代活动（个案用的是出门开车、散步这类需要轻度专注的事）。",
                    sourceArticleIds = listOf("cases-007"),
                ),
                PhaseNote(
                    text = "渴求来时先“观察而不行动”，让冲动像浪一样自己过去，而不是立刻对抗或顺从。",
                    sourceArticleIds = listOf("methods-006"),
                ),
            ),
            caution = "若出现持续失眠、明显抑郁或伤害自己的念头，请尽快联系专业医生或当地心理援助热线。",
        ),
        WithdrawalPhase(
            id = "FLUCTUATION",
            name = "波动期",
            minDay = 8,
            maxDay = 29,
            headline = "第 2–4 周常被社区描述为“最艰难”的一段：好转不是直线，症状会反复。",
            expectation = listOf(
                PhaseNote(
                    text = "多人报告戒断最强烈期集中在第 1–3 周，症状断断续续可持续数月（社区将其类比为急性期后戒断综合征 PAWS）。",
                    sourceArticleIds = listOf("cases-006"),
                ),
                PhaseNote(
                    text = "平坦期：性欲与情绪都低而空，甚至对原本感兴趣的人和事也提不起劲，自述“不是抑郁，因为对未来还是乐观的”。",
                    sourceArticleIds = listOf("cases-007"),
                ),
                PhaseNote(
                    text = "最容易出现的念头是“进展太慢、到底有没有用”，这种不耐烦本身就是这一阶段的典型反应。",
                    sourceArticleIds = listOf("cases-007"),
                ),
            ),
            coping = listOf(
                PhaseNote(
                    text = "把长期目标切成小目标（例如先稳稳走到 30 天），用可达成的小节点替代“一口气戒到底”。",
                    sourceArticleIds = listOf("cases-008"),
                ),
                PhaseNote(
                    text = "用累计使用时长/次数去理解恢复所需时间：个案靠算清“7000 次”来接受恢复本来就要几个月。",
                    sourceArticleIds = listOf("cases-007"),
                ),
                PhaseNote(
                    text = "列清自己的高危情境（时间、地点、情绪、设备）并事先写好对策，避免临场硬扛。",
                    sourceArticleIds = listOf("methods-012"),
                ),
                PhaseNote(
                    text = "正念练习（观察渴求、不评判、不立刻行动）有随机对照试验支持用于降低渴求与复发。",
                    sourceArticleIds = listOf("methods-004"),
                ),
            ),
        ),
        WithdrawalPhase(
            id = "REPAIR",
            name = "修复期",
            minDay = 30,
            maxDay = 59,
            headline = "强度通常在下降，但波动仍在：这一阶段的关键是别把“还有渴求”当成失败。",
            expectation = listOf(
                PhaseNote(
                    text = "闪回与偶发渴求仍会出现，常由压力、孤独、酒精等触发，但强度与频率一般较前几周下降。",
                    sourceArticleIds = listOf("cases-006"),
                ),
                PhaseNote(
                    text = "开始出现主观获益的自我报告：情绪更稳、精力更集中、对自己更有掌控感；这是社区自我报告，不构成医学结论。",
                    sourceArticleIds = listOf("cases-001"),
                ),
                PhaseNote(
                    text = "典型体验是“大体平稳，但能感觉到有变化正在发生”。",
                    sourceArticleIds = listOf("cases-006"),
                ),
            ),
            coping = listOf(
                PhaseNote(
                    text = "把每次成功抵抗闪回算作一次训练：抵抗闪回不是副作用，而是康复过程本身。",
                    sourceArticleIds = listOf("cases-007"),
                ),
                PhaseNote(
                    text = "继续写记录并回看：对戒色日志的定性分析显示，书写与同伴反馈是受访者最主要的应对资源。",
                    sourceArticleIds = listOf("cases-001"),
                ),
                PhaseNote(
                    text = "环境控制 + 问责伙伴：把高风险设备、时段、应用设为默认阻断，并留一个可以如实汇报的人。",
                    sourceArticleIds = listOf("methods-012"),
                ),
            ),
        ),
        WithdrawalPhase(
            id = "RECONNECT",
            name = "重连期",
            minDay = 60,
            maxDay = 89,
            headline = "多数人此时已趋平稳，但要留意个案记录到的“第二波”不适。",
            expectation = listOf(
                PhaseNote(
                    text = "个案逐周记录显示第 61–76 天可能出现第二波较强不适（抑郁、焦虑、极度孤独、失眠、无性欲），第 80 天后基本平稳。",
                    sourceArticleIds = listOf("cases-006"),
                ),
                PhaseNote(
                    text = "社区把 90 天当作重启参照线（约 ±30 天），个体差异很大，不要把它当成硬性截止日。",
                    sourceArticleIds = listOf("cases-008", "cases-007"),
                ),
            ),
            coping = listOf(
                PhaseNote(
                    text = "第二波来了先回看自己已经完成的记录：走过 60 天本身就是能力证据，别用“又回到原点”评价自己。",
                    sourceArticleIds = listOf("cases-006"),
                ),
                PhaseNote(
                    text = "把目标从“攒天数”换成“成为什么样的人”：多数受访者更在意身份与生活节奏的改变。",
                    sourceArticleIds = listOf("cases-001", "cases-004"),
                ),
                PhaseNote(
                    text = "若痛苦主要来自“我不该是这样的人”的价值观冲突，重点要处理的是自我评价，而不是把它全部归因于成瘾。",
                    sourceArticleIds = listOf("cases-004", "cases-012"),
                ),
            ),
            caution = "第二波的强度与时长个体差异极大；若持续两周以上并影响工作生活，建议寻求专业帮助。",
        ),
        WithdrawalPhase(
            id = "CONSOLIDATION",
            name = "巩固期",
            minDay = 90,
            maxDay = null,
            headline = "90 天不是终点线，而是把新习惯固定下来的起点。",
            expectation = listOf(
                PhaseNote(
                    text = "90 天来自 NoFap 社区的“90 天重启挑战”，属于社区方案而非临床标准，采信时应保守。",
                    sourceArticleIds = listOf("methods-011", "cases-008"),
                ),
                PhaseNote(
                    text = "即使长期稳定，强压力事件仍可能唤起旧反应；偶发失误不等于前功尽弃。",
                    sourceArticleIds = listOf("cases-006"),
                ),
            ),
            coping = listOf(
                PhaseNote(
                    text = "把替代性奖励写进日常结构（规律作息、运动、社交），而不是只靠“不再做某件事”。",
                    sourceArticleIds = listOf("methods-012", "cases-001"),
                ),
                PhaseNote(
                    text = "用长期视角看波动：叙事分析提示，“戒断—复发”的二元框架本身会放大痛苦，允许起伏比追求零失误更可持续。",
                    sourceArticleIds = listOf("cases-002", "cases-008"),
                ),
                PhaseNote(
                    text = "把同伴支持与如实汇报保持下去，别在“已经好了”的时候把它撤掉。",
                    sourceArticleIds = listOf("cases-001"),
                ),
            ),
        ),
    )
}

/**
 * 由戒断天数解析当前阶段（F2 全部判断逻辑都在这里，UI 只负责渲染）。
 *
 * 边界按自然日计：第 0–7 天为急性期，第 8 天起进入波动期，以此类推。
 */
class WithdrawalPhaseResolver(
    val phases: List<WithdrawalPhase> = WithdrawalPhaseCatalog.phases,
) {
    init {
        require(phases.isNotEmpty()) { "阶段目录不能为空" }
        phases.forEachIndexed { index, phase ->
            if (index == 0) {
                require(phase.minDay == 0) { "首个阶段必须从第 0 天开始，实际 ${phase.minDay}" }
            } else {
                val previous = phases[index - 1]
                val expected = requireNotNull(previous.maxDay) { "非末尾阶段必须有上界" } + 1
                require(phase.minDay == expected) {
                    "阶段不连续：${previous.id} 结束于 ${previous.maxDay}，${phase.id} 从 ${phase.minDay} 开始"
                }
            }
            require(phase.maxDay == null || phase.maxDay >= phase.minDay) { "阶段 ${phase.id} 的天数区间非法" }
        }
        require(phases.last().maxDay == null) { "最后一个阶段必须没有上界" }
    }

    /** 负数天数（数据异常时的兜底）落到第一个阶段。 */
    fun resolve(dayNumber: Int): WithdrawalPhase {
        val normalized = if (dayNumber < 0) 0 else dayNumber
        return phases.lastOrNull { it.contains(normalized) } ?: phases.first()
    }

    fun indexOf(dayNumber: Int): Int = phases.indexOf(resolve(dayNumber))

    /** 当前阶段内的进度 0f–1f；无上界阶段恒为 0f。 */
    fun progressInPhase(dayNumber: Int): Float {
        val phase = resolve(dayNumber)
        val span = phase.daySpan ?: return 0f
        val elapsed = dayNumber - phase.minDay
        return (elapsed.toFloat() / span).coerceIn(0f, 1f)
    }

    /** 进入下一阶段的第一天；已在最后阶段时返回 null。 */
    fun nextTransitionDay(dayNumber: Int): Int? =
        resolve(dayNumber).maxDay?.plus(1)

    /** 距离下一阶段开始还有几天；已在最后阶段时返回 null。 */
    fun daysUntilNextPhase(dayNumber: Int): Int? =
        nextTransitionDay(dayNumber)?.let { (it - dayNumber).coerceAtLeast(0) }
}
