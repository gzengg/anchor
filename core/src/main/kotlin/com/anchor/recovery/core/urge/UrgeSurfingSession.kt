package com.anchor.recovery.core.urge

import kotlinx.datetime.Instant

/**
 * 渴求冲浪（F3）的流程状态。
 *
 * 状态机顺序固定：INTRO → BREATHE → OBSERVE → RIDE → RATE_AGAIN → DONE。
 * 计时与流程推进全部发生在这里，UI 只负责把 [UrgeSurfingState] 画出来，
 * 并用 `LaunchedEffect` 每秒调一次 [UrgeSurfingSession.tick]。
 */
enum class UrgeSurfingStage(val label: String, val hint: String) {
    INTRO("开始", "渴求像一道波浪：会自己升高，也会自己退下。接下来几分钟先不动手，只观察它。"),
    BREATHE("呼吸引导", "把注意力放在呼吸上：吸气 4 秒，停 2 秒，呼气 6 秒。走神了就回到呼吸。"),
    OBSERVE("观察渴求", "像旁观者一样描述它：在身体哪个位置？紧、热还是空？不下判断。"),
    RIDE("等待它过去", "不对抗，也不顺从。让它在这里，你只是陪着它变弱。"),
    RATE_AGAIN("再次评分", "再给渴求打一次分，看是否变化。"),
    DONE("结束", "无论分数有没有下降，你都完成了一次“不立刻行动”。"),
    ;

    /** 该阶段的下一个阶段；已是最后阶段时返回自身。 */
    val next: UrgeSurfingStage
        get() = entries.getOrElse(ordinal + 1) { this }
}

/**
 * 一轮冲浪的完整状态快照（不可变，便于 UI 用 Compose State 承载）。
 *
 * [peakIntensity] 记录过程中出现过的最高评分，[delta] 为下降值（正数代表渴求变弱）。
 */
data class UrgeSurfingState(
    val stage: UrgeSurfingStage = UrgeSurfingStage.INTRO,
    val elapsedSecInStage: Int = 0,
    /** 整轮已用秒数（跨阶段累计），用于写入 urge_episode.durationSec。 */
    val elapsedSecTotal: Int = 0,
    val initialIntensity: Int? = null,
    val peakIntensity: Int? = null,
    val endIntensity: Int? = null,
    val abandoned: Boolean = false,
) {
    val finished: Boolean get() = stage == UrgeSurfingStage.DONE

    /** 渴求下降值：初始评分 − 结束评分。任一缺失时为 null。 */
    val delta: Int?
        get() = initialIntensity?.let { initial -> endIntensity?.let { initial - it } }

    /** 中途放弃也算部分完成：保留已记录的数据，但不当作完整流程。 */
    val partiallyCompleted: Boolean get() = abandoned || (finished && endIntensity == null)
}

/** 一轮冲浪记录，用于写入 `urge_episode` 表。 */
data class UrgeSurfingResult(
    val startedAt: Instant,
    val durationSec: Int,
    val peakIntensity: Int,
    val endIntensity: Int,
    val delta: Int,
    val abandoned: Boolean,
)

/**
 * F3 的纯逻辑实现：不依赖 Android，时间由调用方（UI 的 LaunchedEffect 或测试）推进。
 *
 * @param stageDurationsSec 各阶段的计时秒数；0 表示该阶段等用户操作、不自动推进。
 */
class UrgeSurfingSession(
    private val stageDurationsSec: Map<UrgeSurfingStage, Int> = DEFAULT_DURATIONS,
) {

    fun initial(): UrgeSurfingState = UrgeSurfingState()

    /** 从 INTRO 进入流程，记录第一次评分。 */
    fun start(state: UrgeSurfingState, intensity: Int): UrgeSurfingState =
        state.copy(
            stage = UrgeSurfingStage.BREATHE,
            elapsedSecInStage = 0,
            initialIntensity = clamp(intensity),
            peakIntensity = clamp(intensity),
        )

    /**
     * 推进 [seconds] 秒；当前阶段计时走完就自动进入下一阶段。
     * RATE_AGAIN 阶段不自动推进（等用户再次评分）。
     */
    fun tick(state: UrgeSurfingState, seconds: Int = 1): UrgeSurfingState {
        if (seconds <= 0 || state.finished || state.abandoned) return state
        val duration = durationOf(state.stage)
        if (duration <= 0) return state

        var remaining = state.elapsedSecInStage + seconds
        val elapsedTotal = state.elapsedSecTotal + seconds
        var stage = state.stage
        while (true) {
            val limit = durationOf(stage)
            if (limit <= 0 || remaining < limit) break
            remaining -= limit
            val next = stage.next
            if (next == stage || durationOf(next) <= 0) {
                // 进入等待用户输入的阶段（RATE_AGAIN）：剩余秒数清零。
                stage = next
                remaining = 0
                break
            }
            stage = next
        }
        return state.copy(stage = stage, elapsedSecInStage = remaining, elapsedSecTotal = elapsedTotal)
    }

    /** 跳过当前阶段的剩余计时（用户点“下一步”）。 */
    fun skipStage(state: UrgeSurfingState): UrgeSurfingState {
        if (state.finished || state.abandoned) return state
        return state.copy(stage = state.stage.next, elapsedSecInStage = 0)
    }

    /** 过程中再次打分（OBSERVE / RIDE 阶段），只更新峰值。 */
    fun rate(state: UrgeSurfingState, intensity: Int): UrgeSurfingState {
        if (state.finished || state.abandoned) return state
        val value = clamp(intensity)
        return state.copy(
            peakIntensity = maxOf(state.peakIntensity ?: value, value),
        )
    }

    /** 完成流程：记录结束评分，进入 DONE。 */
    fun finish(state: UrgeSurfingState, endIntensity: Int): UrgeSurfingState {
        if (state.abandoned) return state
        val value = clamp(endIntensity)
        return state.copy(
            stage = UrgeSurfingStage.DONE,
            elapsedSecInStage = 0,
            endIntensity = value,
            peakIntensity = maxOf(state.peakIntensity ?: value, value),
        )
    }

    /** 中途放弃：标记为部分完成，已记录的评分保留。 */
    fun abandon(state: UrgeSurfingState): UrgeSurfingState {
        if (state.finished) return state
        return state.copy(
            stage = UrgeSurfingStage.DONE,
            elapsedSecInStage = 0,
            abandoned = true,
        )
    }

    /** 当前阶段还剩几秒；等待用户输入的阶段返回 0。 */
    fun remainingSecInStage(state: UrgeSurfingState): Int {
        val duration = durationOf(state.stage)
        return (duration - state.elapsedSecInStage).coerceAtLeast(0)
    }

    fun stageProgress(state: UrgeSurfingState): Float {
        val duration = durationOf(state.stage)
        if (duration <= 0) return 1f
        return (state.elapsedSecInStage.toFloat() / duration).coerceIn(0f, 1f)
    }

    /** 导出可落库的结果；评分缺失时用峰值兜底，保证“部分完成”也能记录。 */
    fun result(state: UrgeSurfingState, startedAt: Instant): UrgeSurfingResult {
        val peak = state.peakIntensity ?: state.initialIntensity ?: MIN_INTENSITY
        val end = state.endIntensity ?: peak
        return UrgeSurfingResult(
            startedAt = startedAt,
            durationSec = state.elapsedSecTotal,
            peakIntensity = peak,
            endIntensity = end,
            delta = peak - end,
            abandoned = state.abandoned,
        )
    }

    private fun durationOf(stage: UrgeSurfingStage): Int = stageDurationsSec[stage] ?: 0

    companion object {
        const val MIN_INTENSITY = 1
        const val MAX_INTENSITY = 10

        /** 评分一律钳制在 1–10，越界输入不抛异常。 */
        fun clamp(intensity: Int): Int = intensity.coerceIn(MIN_INTENSITY, MAX_INTENSITY)

        /** 默认节奏：呼吸 60s → 观察 30s → 等待 120s → 用户评分。 */
        val DEFAULT_DURATIONS: Map<UrgeSurfingStage, Int> = mapOf(
            UrgeSurfingStage.INTRO to 0,
            UrgeSurfingStage.BREATHE to 60,
            UrgeSurfingStage.OBSERVE to 30,
            UrgeSurfingStage.RIDE to 120,
            UrgeSurfingStage.RATE_AGAIN to 0,
            UrgeSurfingStage.DONE to 0,
        )
    }
}
