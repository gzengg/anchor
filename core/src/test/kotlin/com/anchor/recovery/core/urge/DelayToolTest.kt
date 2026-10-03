package com.anchor.recovery.core.urge

import com.anchor.recovery.core.clock.FakeClock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/** F4 延时工具的倒计时逻辑（时间全部来自注入的 Clock）。 */
class DelayToolTest {

    private val clock = FakeClock(
        current = Instant.parse("2026-04-01T20:00:00Z"),
        timeZone = TimeZone.of("Asia/Shanghai"),
    )
    private val tool = DelayTool(clock)

    @Test
    fun `默认时长为十分钟且起始剩余时间为满额`() {
        val session = tool.start()
        assertEquals(600, session.durationSec)
        assertEquals(600, tool.remainingSec(session))
        assertEquals("10:00", tool.remainingLabel(session))
        assertFalse(tool.isComplete(session))
        assertEquals(0f, tool.progress(session))
        assertEquals(Instant.parse("2026-04-01T20:10:00Z"), session.endsAt)
    }

    @Test
    fun `剩余时间随注入时钟减少`() {
        val session = tool.start()

        clock.advanceBy(60.seconds)
        assertEquals(540, tool.remainingSec(session))
        assertEquals("09:00", tool.remainingLabel(session))
        assertEquals(0.1f, tool.progress(session))

        clock.advanceBy(540.seconds)
        assertEquals(0, tool.remainingSec(session))
        assertEquals("00:00", tool.remainingLabel(session))
        assertTrue(tool.isComplete(session))
        assertEquals(1f, tool.progress(session))
    }

    @Test
    fun `超过预计时长也不会出现负数`() {
        val session = tool.start(durationSec = 30)
        clock.advanceBy(120.seconds)
        assertEquals(0, tool.remainingSec(session))
        assertTrue(tool.elapsedSec(session) >= 0)
        assertEquals(1f, tool.progress(session))
        assertTrue(tool.isComplete(session))
    }

    @Test
    fun `自定义时长与零时长边界`() {
        val short = tool.start(durationSec = 90)
        assertEquals(90, short.durationSec)
        assertEquals("01:30", tool.remainingLabel(short))
        assertFalse(tool.isComplete(short))

        val instant = tool.start(durationSec = 0)
        assertTrue(tool.isComplete(instant))
        assertEquals(1f, tool.progress(instant))
        assertEquals(0, tool.remainingSec(instant))
    }

    @Test
    fun `剩余时间文案支持超过一小时`() {
        val long = tool.start(durationSec = 3661)
        assertEquals("61:01", tool.remainingLabel(long))
    }

    @Test
    fun `提示语轮播按时间循环且在提示语为空时安全`() {
        assertEquals(0, tool.promptIndex(elapsedSec = 0, promptCount = 3))
        assertEquals(1, tool.promptIndex(elapsedSec = 45, promptCount = 3))
        assertEquals(2, tool.promptIndex(elapsedSec = 90, promptCount = 3))
        assertEquals(0, tool.promptIndex(elapsedSec = 135, promptCount = 3))
        assertEquals(0, tool.promptIndex(elapsedSec = 500, promptCount = 0))
    }
}
