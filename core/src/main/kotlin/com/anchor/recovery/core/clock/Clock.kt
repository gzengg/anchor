package com.anchor.recovery.core.clock

import kotlinx.datetime.Clock as KotlinxClock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * 全局时钟。**所有日期/时间逻辑必须经此获取当前时间**，测试注入 [FakeClock]。
 *
 * 自然日（local date）一律在 [timeZone] 下换算：打卡/复吸/断签都按用户本地日界判定。
 */
interface Clock {
    val timeZone: TimeZone

    fun now(): Instant

    fun nowMillis(): Long = now().toEpochMilliseconds()

    /** 当前自然日。 */
    fun today(): LocalDate = localDateOf(now())

    /** 某一时刻所在的自然日。 */
    fun localDateOf(instant: Instant): LocalDate = instant.toLocalDateTime(timeZone).date
}

class SystemClock(
    override val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : Clock {
    override fun now(): Instant = KotlinxClock.System.now()
}
