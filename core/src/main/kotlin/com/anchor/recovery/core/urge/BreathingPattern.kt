package com.anchor.recovery.core.urge

/** 4-2-6 呼吸节拍的一次呼吸提示。 */
data class BreathingCue(val phase: Int, val remainingSec: Int) {

    companion object {
        const val PHASE_IN = 0
        const val PHASE_HOLD = 1
        const val PHASE_OUT = 2
    }
}

/**
 * 呼吸引导的节拍：吸气 4 秒 → 停 2 秒 → 呼气 6 秒，循环。
 *
 * F3 与 F4 共用同一份节拍，UI 只把当前阶段的已用秒数传进来。
 */
object BreathingPattern {

    const val IN_SEC = 4
    const val HOLD_SEC = 2
    const val OUT_SEC = 6
    const val CYCLE_SEC = IN_SEC + HOLD_SEC + OUT_SEC

    fun cue(elapsedSec: Int): BreathingCue {
        val position = if (elapsedSec < 0) 0 else elapsedSec % CYCLE_SEC
        return when {
            position < IN_SEC -> BreathingCue(BreathingCue.PHASE_IN, IN_SEC - position)
            position < IN_SEC + HOLD_SEC ->
                BreathingCue(BreathingCue.PHASE_HOLD, IN_SEC + HOLD_SEC - position)

            else -> BreathingCue(BreathingCue.PHASE_OUT, CYCLE_SEC - position)
        }
    }
}
