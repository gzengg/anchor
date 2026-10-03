package com.anchor.recovery.core.urge

import kotlin.test.Test
import kotlin.test.assertEquals

/** F3/F4 共用的 4-2-6 呼吸节拍。 */
class BreathingPatternTest {

    @Test
    fun `一个周期内依次是吸气 4 秒、停 2 秒、呼气 6 秒`() {
        assertEquals(BreathingCue.PHASE_IN, BreathingPattern.cue(0).phase)
        assertEquals(4, BreathingPattern.cue(0).remainingSec)
        assertEquals(1, BreathingPattern.cue(3).remainingSec)
        assertEquals(BreathingCue.PHASE_HOLD, BreathingPattern.cue(4).phase)
        assertEquals(2, BreathingPattern.cue(4).remainingSec)
        assertEquals(1, BreathingPattern.cue(5).remainingSec)
        assertEquals(BreathingCue.PHASE_OUT, BreathingPattern.cue(6).phase)
        assertEquals(6, BreathingPattern.cue(6).remainingSec)
        assertEquals(1, BreathingPattern.cue(11).remainingSec)
    }

    @Test
    fun `十二秒后回到吸气且负数与超大值都有兜底`() {
        assertEquals(BreathingCue.PHASE_IN, BreathingPattern.cue(12).phase)
        assertEquals(4, BreathingPattern.cue(12).remainingSec)
        assertEquals(BreathingCue.PHASE_IN, BreathingPattern.cue(0).phase)
        assertEquals(BreathingCue.PHASE_IN, BreathingPattern.cue(-5).phase)
        assertEquals(BreathingPattern.cue(2), BreathingPattern.cue(1202))
    }
}
