package com.anchor.recovery.core.clock

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Duration

/**
 * 可写时钟测试替身：放在 main 源集，方便 :core 与 :app 的 JVM 测试共用同一份实现。
 *
 * 默认时区固定为 Asia/Shanghai，避免测试结果随构建机本地时区漂移。
 */
class FakeClock(
    private var current: Instant,
    override val timeZone: TimeZone = TimeZone.of("Asia/Shanghai"),
) : Clock {

    override fun now(): Instant = current

    fun setNow(instant: Instant) {
        current = instant
    }

    fun advanceBy(duration: Duration) {
        current += duration
    }

    /** 把（本地时区的）当前时刻直接设为某天某时某分。 */
    fun setLocalDateTime(date: LocalDate, hour: Int = 12, minute: Int = 0, second: Int = 0) {
        current = LocalDateTime(date, LocalTime(hour, minute, second)).toInstant(timeZone)
    }

    companion object {
        /** 以「某天中午 12:00」为基准创建时钟，避免多数用例落在日界边缘。 */
        fun at(
            date: LocalDate,
            hour: Int = 12,
            minute: Int = 0,
            timeZone: TimeZone = TimeZone.of("Asia/Shanghai"),
        ): FakeClock = FakeClock(
            LocalDateTime(date, LocalTime(hour, minute)).toInstant(timeZone),
            timeZone,
        )
    }
}
