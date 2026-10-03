package com.anchor.recovery.core.phase

/**
 * 阶段文案的稳定 key。
 *
 * 用户可见文案（阶段名、标题、可能经历、可做的事、caution、天数区间与求助提示）全部外移到
 * `:app` 的 `values/strings_phases.xml`，由 `PhaseText.phaseTextRes` 映射成资源 id；`:core`
 * 只保留这套 key 与天数口径——它连 `android.*` 都不能依赖，也不该持有中文文案。
 *
 * 命名规则 `阶段_区块_序号`（序号从 1 起，与目录中的顺序一致，如 `ACUTE_EXPECTATION_2`
 * 对应正序第二条「期待反应」）。`PhaseText.phaseTextRes` 的 `when` 不写 `else`：这里新增
 * key 时 `:app` 编译期就会失败，避免出现「有状态没文案」。
 */
enum class PhaseTextKey {
    ACUTE_NAME,
    ACUTE_HEADLINE,
    ACUTE_EXPECTATION_1,
    ACUTE_EXPECTATION_2,
    ACUTE_EXPECTATION_3,
    ACUTE_COPING_1,
    ACUTE_COPING_2,
    ACUTE_COPING_3,
    ACUTE_COPING_4,
    ACUTE_CAUTION,

    FLUCTUATION_NAME,
    FLUCTUATION_HEADLINE,
    FLUCTUATION_EXPECTATION_1,
    FLUCTUATION_EXPECTATION_2,
    FLUCTUATION_EXPECTATION_3,
    FLUCTUATION_COPING_1,
    FLUCTUATION_COPING_2,
    FLUCTUATION_COPING_3,
    FLUCTUATION_COPING_4,

    REPAIR_NAME,
    REPAIR_HEADLINE,
    REPAIR_EXPECTATION_1,
    REPAIR_EXPECTATION_2,
    REPAIR_EXPECTATION_3,
    REPAIR_COPING_1,
    REPAIR_COPING_2,
    REPAIR_COPING_3,

    RECONNECT_NAME,
    RECONNECT_HEADLINE,
    RECONNECT_EXPECTATION_1,
    RECONNECT_EXPECTATION_2,
    RECONNECT_COPING_1,
    RECONNECT_COPING_2,
    RECONNECT_COPING_3,
    RECONNECT_CAUTION,

    CONSOLIDATION_NAME,
    CONSOLIDATION_HEADLINE,
    CONSOLIDATION_EXPECTATION_1,
    CONSOLIDATION_EXPECTATION_2,
    CONSOLIDATION_COPING_1,
    CONSOLIDATION_COPING_2,
    CONSOLIDATION_COPING_3,
}

/**
 * 一条带出处的阶段文案。
 *
 * [sourceArticleIds] 指向 `assets/content/articles.json` 里的文章 id，
 * 由 `WithdrawalPhaseResolverTest` / `WithdrawalPhaseContentTest` 校验必须真实存在。
 * [textKey] 指向 `:app` 的文案资源。
 */
data class PhaseNote(
    val textKey: PhaseTextKey,
    val sourceArticleIds: List<String>,
)

/**
 * 戒断阶段（F2）。天数边界为闭区间；[maxDay] 为 null 表示没有上限（最后一个阶段）。
 *
 * 阶段名 / 标题 / caution 是用户可见文案，均已外移；这里只留 key 与天数。
 * 天数区间文案（"第 3–7 天"）也由 `:app` 按 [minDay] / [maxDay] 渲染，`core` 不做字符串拼接。
 */
data class WithdrawalPhase(
    val id: String,
    val nameKey: PhaseTextKey,
    val minDay: Int,
    val maxDay: Int?,
    val headlineKey: PhaseTextKey,
    val expectation: List<PhaseNote>,
    val coping: List<PhaseNote>,
    val cautionKey: PhaseTextKey? = null,
) {
    fun contains(dayNumber: Int): Boolean =
        dayNumber >= minDay && (maxDay == null || dayNumber <= maxDay)

    /** 阶段内全部引用到的文章 id（去重，保持出现顺序）。 */
    val sourceArticleIds: List<String>
        get() = (expectation + coping).flatMap { it.sourceArticleIds }.distinct()

    /** 该阶段的天数跨度；无上限阶段返回 null。 */
    val daySpan: Int?
        get() = maxDay?.let { it - minDay + 1 }
}

/**
 * 五个阶段的目录（结构与天数口径见 `WithdrawalPhaseResolver`）。
 *
 * 时间线（症状与主观经验）提炼自 `anchor-reference-docs/05-cases/`（社区自我报告与定性研究），
 * 应对技术另引 `04-methods/` 里的干预研究。社区来源（cases-006/007/008：YBOP、NoFap）
 * 自我报告性质、无对照验证，故文案中一律标明"自我报告/社区经验"，不做医学结论。
 * 文案本体在 `:app` 的 `values/strings_phases.xml`，合规断言在 `PhaseTextTest`。
 */
object WithdrawalPhaseCatalog {

    val phases: List<WithdrawalPhase> = listOf(
        WithdrawalPhase(
            id = "ACUTE",
            nameKey = PhaseTextKey.ACUTE_NAME,
            minDay = 0,
            maxDay = 7,
            headlineKey = PhaseTextKey.ACUTE_HEADLINE,
            expectation = listOf(
                PhaseNote(
                    textKey = PhaseTextKey.ACUTE_EXPECTATION_1,
                    sourceArticleIds = listOf("cases-006"),
                ),
                PhaseNote(
                    textKey = PhaseTextKey.ACUTE_EXPECTATION_2,
                    sourceArticleIds = listOf("cases-007"),
                ),
                PhaseNote(
                    textKey = PhaseTextKey.ACUTE_EXPECTATION_3,
                    sourceArticleIds = listOf("cases-006", "cases-007"),
                ),
            ),
            coping = listOf(
                PhaseNote(
                    textKey = PhaseTextKey.ACUTE_COPING_1,
                    sourceArticleIds = listOf("cases-006"),
                ),
                PhaseNote(
                    textKey = PhaseTextKey.ACUTE_COPING_2,
                    sourceArticleIds = listOf("cases-006"),
                ),
                PhaseNote(
                    textKey = PhaseTextKey.ACUTE_COPING_3,
                    sourceArticleIds = listOf("cases-007"),
                ),
                PhaseNote(
                    textKey = PhaseTextKey.ACUTE_COPING_4,
                    sourceArticleIds = listOf("methods-006"),
                ),
            ),
            cautionKey = PhaseTextKey.ACUTE_CAUTION,
        ),
        WithdrawalPhase(
            id = "FLUCTUATION",
            nameKey = PhaseTextKey.FLUCTUATION_NAME,
            minDay = 8,
            maxDay = 29,
            headlineKey = PhaseTextKey.FLUCTUATION_HEADLINE,
            expectation = listOf(
                PhaseNote(
                    textKey = PhaseTextKey.FLUCTUATION_EXPECTATION_1,
                    sourceArticleIds = listOf("cases-006"),
                ),
                PhaseNote(
                    textKey = PhaseTextKey.FLUCTUATION_EXPECTATION_2,
                    sourceArticleIds = listOf("cases-007"),
                ),
                PhaseNote(
                    textKey = PhaseTextKey.FLUCTUATION_EXPECTATION_3,
                    sourceArticleIds = listOf("cases-007"),
                ),
            ),
            coping = listOf(
                PhaseNote(
                    textKey = PhaseTextKey.FLUCTUATION_COPING_1,
                    sourceArticleIds = listOf("cases-008"),
                ),
                PhaseNote(
                    textKey = PhaseTextKey.FLUCTUATION_COPING_2,
                    sourceArticleIds = listOf("cases-007"),
                ),
                PhaseNote(
                    textKey = PhaseTextKey.FLUCTUATION_COPING_3,
                    sourceArticleIds = listOf("methods-012"),
                ),
                PhaseNote(
                    textKey = PhaseTextKey.FLUCTUATION_COPING_4,
                    sourceArticleIds = listOf("methods-004"),
                ),
            ),
        ),
        WithdrawalPhase(
            id = "REPAIR",
            nameKey = PhaseTextKey.REPAIR_NAME,
            minDay = 30,
            maxDay = 59,
            headlineKey = PhaseTextKey.REPAIR_HEADLINE,
            expectation = listOf(
                PhaseNote(
                    textKey = PhaseTextKey.REPAIR_EXPECTATION_1,
                    sourceArticleIds = listOf("cases-006"),
                ),
                PhaseNote(
                    textKey = PhaseTextKey.REPAIR_EXPECTATION_2,
                    sourceArticleIds = listOf("cases-001"),
                ),
                PhaseNote(
                    textKey = PhaseTextKey.REPAIR_EXPECTATION_3,
                    sourceArticleIds = listOf("cases-006"),
                ),
            ),
            coping = listOf(
                PhaseNote(
                    textKey = PhaseTextKey.REPAIR_COPING_1,
                    sourceArticleIds = listOf("cases-007"),
                ),
                PhaseNote(
                    textKey = PhaseTextKey.REPAIR_COPING_2,
                    sourceArticleIds = listOf("cases-001"),
                ),
                PhaseNote(
                    textKey = PhaseTextKey.REPAIR_COPING_3,
                    sourceArticleIds = listOf("methods-012"),
                ),
            ),
        ),
        WithdrawalPhase(
            id = "RECONNECT",
            nameKey = PhaseTextKey.RECONNECT_NAME,
            minDay = 60,
            maxDay = 89,
            headlineKey = PhaseTextKey.RECONNECT_HEADLINE,
            expectation = listOf(
                PhaseNote(
                    textKey = PhaseTextKey.RECONNECT_EXPECTATION_1,
                    sourceArticleIds = listOf("cases-006"),
                ),
                PhaseNote(
                    textKey = PhaseTextKey.RECONNECT_EXPECTATION_2,
                    sourceArticleIds = listOf("cases-008", "cases-007"),
                ),
            ),
            coping = listOf(
                PhaseNote(
                    textKey = PhaseTextKey.RECONNECT_COPING_1,
                    sourceArticleIds = listOf("cases-006"),
                ),
                PhaseNote(
                    textKey = PhaseTextKey.RECONNECT_COPING_2,
                    sourceArticleIds = listOf("cases-001", "cases-004"),
                ),
                PhaseNote(
                    textKey = PhaseTextKey.RECONNECT_COPING_3,
                    sourceArticleIds = listOf("cases-004", "cases-012"),
                ),
            ),
            cautionKey = PhaseTextKey.RECONNECT_CAUTION,
        ),
        WithdrawalPhase(
            id = "CONSOLIDATION",
            nameKey = PhaseTextKey.CONSOLIDATION_NAME,
            minDay = 90,
            maxDay = null,
            headlineKey = PhaseTextKey.CONSOLIDATION_HEADLINE,
            expectation = listOf(
                PhaseNote(
                    textKey = PhaseTextKey.CONSOLIDATION_EXPECTATION_1,
                    sourceArticleIds = listOf("methods-011", "cases-008"),
                ),
                PhaseNote(
                    textKey = PhaseTextKey.CONSOLIDATION_EXPECTATION_2,
                    sourceArticleIds = listOf("cases-006"),
                ),
            ),
            coping = listOf(
                PhaseNote(
                    textKey = PhaseTextKey.CONSOLIDATION_COPING_1,
                    sourceArticleIds = listOf("methods-012", "cases-001"),
                ),
                PhaseNote(
                    textKey = PhaseTextKey.CONSOLIDATION_COPING_2,
                    sourceArticleIds = listOf("cases-002", "cases-008"),
                ),
                PhaseNote(
                    textKey = PhaseTextKey.CONSOLIDATION_COPING_3,
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
