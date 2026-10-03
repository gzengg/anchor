package com.anchor.recovery.core.urge

import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** F3 状态机：完整流程 / 中途放弃 / 评分钳制。 */
class UrgeSurfingSessionTest {

    private val session = UrgeSurfingSession()
    private val startedAt = Instant.parse("2026-03-05T22:30:00Z")

    @Test
    fun `完整流程按 INTRO-BREATHE-OBSERVE-RIDE-RATE_AGAIN-DONE 顺序推进`() {
        var state = session.initial()
        assertEquals(UrgeSurfingStage.INTRO, state.stage)

        state = session.start(state, intensity = 8)
        assertEquals(UrgeSurfingStage.BREATHE, state.stage)
        assertEquals(8, state.initialIntensity)
        assertEquals(8, state.peakIntensity)

        // 呼吸 60 秒后自动进入观察
        state = session.tick(state, seconds = 59)
        assertEquals(UrgeSurfingStage.BREATHE, state.stage)
        assertEquals(1, session.remainingSecInStage(state))

        state = session.tick(state, seconds = 1)
        assertEquals(UrgeSurfingStage.OBSERVE, state.stage)
        assertEquals(0, state.elapsedSecInStage)

        // 观察 30 秒 + 等待 120 秒
        state = session.tick(state, seconds = 30)
        assertEquals(UrgeSurfingStage.RIDE, state.stage)

        state = session.tick(state, seconds = 119)
        assertEquals(UrgeSurfingStage.RIDE, state.stage)

        state = session.tick(state, seconds = 1)
        assertEquals(UrgeSurfingStage.RATE_AGAIN, state.stage)

        // RATE_AGAIN 等用户输入，不再自动推进
        state = session.tick(state, seconds = 600)
        assertEquals(UrgeSurfingStage.RATE_AGAIN, state.stage)
        assertEquals(0, state.elapsedSecInStage)

        state = session.finish(state, endIntensity = 3)
        assertEquals(UrgeSurfingStage.DONE, state.stage)
        assertTrue(state.finished)
        assertFalse(state.abandoned)
        assertFalse(state.partiallyCompleted)
        assertEquals(3, state.endIntensity)
        assertEquals(5, state.delta)
        assertTrue(session.remainingSecInStage(state) == 0)
    }

    @Test
    fun `中途放弃记为部分完成并保留已记录评分`() {
        var state = session.start(session.initial(), intensity = 7)
        state = session.rate(state, intensity = 9)
        state = session.tick(state, seconds = 10)
        assertEquals(UrgeSurfingStage.BREATHE, state.stage)

        state = session.abandon(state)
        assertTrue(state.abandoned)
        assertTrue(state.partiallyCompleted)
        assertEquals(UrgeSurfingStage.DONE, state.stage)
        assertEquals(7, state.initialIntensity)
        assertEquals(9, state.peakIntensity)
        assertNull(state.delta)

        // 放弃后任何推进都不再生效
        assertEquals(state, session.tick(state, seconds = 60))
        assertEquals(state, session.rate(state, intensity = 1))
        assertEquals(state, session.skipStage(state))
    }

    @Test
    fun `评分钳制在 1 到 10`() {
        var state = session.start(session.initial(), intensity = 0)
        assertEquals(1, state.initialIntensity)

        state = session.rate(state, intensity = 99)
        assertEquals(10, state.peakIntensity)

        state = session.finish(state, endIntensity = -3)
        assertEquals(1, state.endIntensity)
        assertEquals(0, state.delta)

        // start 时越界的高分同样被夹到 10
        val high = session.start(session.initial(), intensity = 42)
        assertEquals(10, high.initialIntensity)
    }

    @Test
    fun `跨阶段的一次 tick 不会丢秒数`() {
        // 呼吸剩 10 秒时推进 15 秒 → 进入观察阶段的第 5 秒
        var state = session.start(session.initial(), intensity = 5)
        state = session.tick(state, seconds = 50)
        assertEquals(UrgeSurfingStage.BREATHE, state.stage)

        state = session.tick(state, seconds = 15)
        assertEquals(UrgeSurfingStage.OBSERVE, state.stage)
        assertEquals(5, state.elapsedSecInStage)

        // 一次跳过观察 + 等待两个阶段
        state = session.tick(state, seconds = 200)
        assertEquals(UrgeSurfingStage.RATE_AGAIN, state.stage)
    }

    @Test
    fun `导出结果在未评分时用峰值兜底`() {
        var state = session.start(session.initial(), intensity = 6)
        state = session.rate(state, intensity = 8)
        state = session.tick(state, seconds = 90)
        assertEquals(90, state.elapsedSecTotal)
        val abandoned = session.abandon(state)
        val result = session.result(abandoned, startedAt = startedAt)

        assertEquals(startedAt, result.startedAt)
        assertEquals(8, result.peakIntensity)
        assertEquals(8, result.endIntensity)
        assertEquals(0, result.delta)
        assertEquals(90, result.durationSec)
        assertTrue(result.abandoned)
    }

    @Test
    fun `skipStage 可以手动跳到下一阶段`() {
        var state = session.start(session.initial(), intensity = 4)
        state = session.skipStage(state)
        assertEquals(UrgeSurfingStage.OBSERVE, state.stage)
        state = session.skipStage(state)
        assertEquals(UrgeSurfingStage.RIDE, state.stage)
        state = session.skipStage(state)
        assertEquals(UrgeSurfingStage.RATE_AGAIN, state.stage)
        state = session.skipStage(state)
        assertEquals(UrgeSurfingStage.DONE, state.stage)
        // DONE 之后停在原地
        assertEquals(UrgeSurfingStage.DONE, session.skipStage(state).stage)
    }

    @Test
    fun `阶段进度与剩余时间在边界上正确`() {
        val state = session.start(session.initial(), intensity = 5)
        assertEquals(60, session.remainingSecInStage(state))
        assertEquals(0f, session.stageProgress(state))

        val half = session.tick(state, seconds = 30)
        assertEquals(0.5f, session.stageProgress(half))
        assertEquals(30, session.remainingSecInStage(half))
    }
}
