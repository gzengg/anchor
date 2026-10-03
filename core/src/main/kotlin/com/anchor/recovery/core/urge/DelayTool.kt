package com.anchor.recovery.core.urge

import com.anchor.recovery.core.clock.Clock
import kotlinx.datetime.Instant

/** 一次延时练习：从 [startedAt] 起计时 [durationSec] 秒。 */
data class DelaySession(
    val startedAt: Instant,
    val durationSec: Int = DelayTool.DEFAULT_DURATION_SEC,
) {
    val endsAt: Instant
        get() = Instant.fromEpochSeconds(startedAt.epochSeconds + durationSec)
}

/**
 * F4 十分钟延时工具的纯逻辑：只负责“还剩多久 / 是否到点”，当前时间一律来自 [Clock]。
 *
 * 到点之后由 UI 询问“渴求是否过去”，并写入 `urge_episode`（tool = DELAY_TOOL）。
 */
class DelayTool(
    private val clock: Clock = com.anchor.recovery.core.clock.SystemClock(),
    val defaultDurationSec: Int = DEFAULT_DURATION_SEC,
) {

    fun start(durationSec: Int = defaultDurationSec): DelaySession =
        DelaySession(startedAt = clock.now(), durationSec = durationSec.coerceAtLeast(0))

    fun elapsedSec(session: DelaySession, now: Instant = clock.now()): Int =
        (now.epochSeconds - session.startedAt.epochSeconds).coerceAtLeast(0).toInt()

    fun remainingSec(session: DelaySession, now: Instant = clock.now()): Int =
        (session.durationSec - elapsedSec(session, now)).coerceAtLeast(0)

    fun isComplete(session: DelaySession, now: Instant = clock.now()): Boolean =
        elapsedSec(session, now) >= session.durationSec

    /** 进度 0f–1f，供进度条使用。 */
    fun progress(session: DelaySession, now: Instant = clock.now()): Float {
        if (session.durationSec <= 0) return 1f
        return (elapsedSec(session, now).toFloat() / session.durationSec).coerceIn(0f, 1f)
    }

    /** 剩余时间的 mm:ss 文案（超过 1 小时也能正常显示）。 */
    fun remainingLabel(session: DelaySession, now: Instant = clock.now()): String =
        formatDuration(remainingSec(session, now))

    /** 提示语轮播：每 [rotateEverySec] 秒换一条，循环播放。 */
    fun promptIndex(elapsedSec: Int, promptCount: Int, rotateEverySec: Int = PROMPT_ROTATE_SEC): Int {
        if (promptCount <= 0) return 0
        if (rotateEverySec <= 0) return 0
        return (elapsedSec / rotateEverySec) % promptCount
    }

    companion object {
        const val DEFAULT_DURATION_SEC = 10 * 60
        const val PROMPT_ROTATE_SEC = 45

        fun formatDuration(totalSec: Int): String {
            val safe = totalSec.coerceAtLeast(0)
            val minutes = safe / 60
            val seconds = safe % 60
            return "%02d:%02d".format(minutes, seconds)
        }
    }
}
