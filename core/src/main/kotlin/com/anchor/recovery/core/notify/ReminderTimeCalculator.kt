package com.anchor.recovery.core.notify

import com.anchor.recovery.core.clock.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/**
 * 每日提醒时间（本地时区的 HH:mm）。脏值在边界处挡住，不从 DataStore 直接带进计算。
 */
data class ReminderTime(val hour: Int, val minute: Int) {
    init {
        require(hour in 0..23) { "小时必须在 0..23，实际 $hour" }
        require(minute in 0..59) { "分钟必须在 0..59，实际 $minute" }
    }

    fun toLocalTime(): LocalTime = LocalTime(hour, minute)

    /** 以分钟为步长滚动（设置页的 +/- 按钮），跨小时与跨天都回绕。 */
    fun shiftMinutes(delta: Int): ReminderTime {
        val total = ((hour * 60 + minute + delta) % MINUTES_PER_DAY + MINUTES_PER_DAY) % MINUTES_PER_DAY
        return ReminderTime(total / 60, total % 60)
    }

    /** 固定 two-digits 格式，不依赖 Locale。 */
    override fun toString(): String =
        "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

    companion object {
        const val MINUTES_PER_DAY = 24 * 60

        /** 默认晚上 21:00 —— 睡前记录，不打断白天。 */
        val DEFAULT = ReminderTime(21, 0)

        /** 解析 HH:mm；非法输入返回 null，由调用方回落到 [DEFAULT]。 */
        fun parseOrNull(raw: String): ReminderTime? {
            val parts = raw.trim().split(":")
            if (parts.size != 2) return null
            val hour = parts[0].toIntOrNull() ?: return null
            val minute = parts[1].toIntOrNull() ?: return null
            if (hour !in 0..23 || minute !in 0..59) return null
            return ReminderTime(hour, minute)
        }
    }
}

/** 下一次提醒的绝对时刻、相对当前时间的延迟，以及它落在哪个自然日。 */
data class NextReminder(
    val triggerAt: Instant,
    val delay: Duration,
    val date: LocalDate,
    val time: ReminderTime,
)

/**
 * 依据用户设定时间与「现在」计算下一次提醒触发延迟，供 WorkManager 的 `setInitialDelay` 使用。
 *
 * 规则：当天设定时刻还没到就用当天；已经到点或刚好到点则顺延到次日。
 * 夏令时缺口（本地时间不存在）交由 kotlinx-datetime 解析为缺口之后的时刻，不会抛异常。
 */
object ReminderTimeCalculator {

    fun nextReminder(now: Instant, time: ReminderTime, timeZone: TimeZone): NextReminder {
        val today = now.toLocalDateTime(timeZone).date
        val todayTrigger = today.atTime(time.toLocalTime()).toInstant(timeZone)
        if (todayTrigger > now) {
            return NextReminder(todayTrigger, todayTrigger - now, today, time)
        }
        val tomorrow = today.plus(1, DateTimeUnit.DAY)
        val tomorrowTrigger = tomorrow.atTime(time.toLocalTime()).toInstant(timeZone)
        return NextReminder(tomorrowTrigger, tomorrowTrigger - now, tomorrow, time)
    }

    fun nextReminder(clock: Clock, time: ReminderTime): NextReminder =
        nextReminder(clock.now(), time, clock.timeZone)

    /** 只取延迟时长（WorkManager 直接喂这个值）。 */
    fun delayUntilNext(clock: Clock, time: ReminderTime): Duration =
        nextReminder(clock, time).delay

    /**
     * 24 小时周期任务的首次延迟：超过 24 小时会被钳到 24 小时，避免周期任务第一次触发被无限推迟。
     */
    fun initialDelayForPeriodic(clock: Clock, time: ReminderTime): Duration {
        val delay = delayUntilNext(clock, time)
        val oneDay = 24.hours
        return if (delay > oneDay) oneDay else delay
    }
}
