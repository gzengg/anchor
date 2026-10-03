package com.anchor.recovery.core.notify

import com.anchor.recovery.core.clock.FakeClock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.milliseconds

class ReminderTimeCalculatorTest {

    private val utc = TimeZone.UTC

    // ---------- 基本：今天 / 明天 ----------

    @Test
    fun `当前时间早于设定时间时当天触发`() {
        val now = Instant.parse("2024-05-01T08:00:00Z")
        val next = ReminderTimeCalculator.nextReminder(now, ReminderTime(21, 0), utc)

        assertEquals(Instant.parse("2024-05-01T21:00:00Z"), next.triggerAt)
        assertEquals(13.hours, next.delay)
        assertEquals(LocalDate(2024, 5, 1), next.date)
    }

    @Test
    fun `当前时间晚于设定时间时顺延到次日`() {
        val now = Instant.parse("2024-05-01T22:00:00Z")
        val next = ReminderTimeCalculator.nextReminder(now, ReminderTime(21, 0), utc)

        assertEquals(Instant.parse("2024-05-02T21:00:00Z"), next.triggerAt)
        assertEquals(23.hours, next.delay)
        assertEquals(LocalDate(2024, 5, 2), next.date)
    }

    @Test
    fun `刚好到设定时刻视为已过 顺延到次日整 24 小时`() {
        val now = Instant.parse("2024-05-01T21:00:00Z")
        val next = ReminderTimeCalculator.nextReminder(now, ReminderTime(21, 0), utc)

        assertEquals(Instant.parse("2024-05-02T21:00:00Z"), next.triggerAt)
        assertEquals(24.hours, next.delay)
    }

    @Test
    fun `差一毫秒时仍算今天`() {
        val now = Instant.parse("2024-05-01T20:59:59.999Z")
        val next = ReminderTimeCalculator.nextReminder(now, ReminderTime(21, 0), utc)

        assertEquals(Instant.parse("2024-05-01T21:00:00Z"), next.triggerAt)
        assertEquals(1.milliseconds, next.delay)
    }

    @Test
    fun `午夜设定 00 00 在当天 23 59 之后顺延到次日的零点`() {
        val now = Instant.parse("2024-05-01T23:59:30Z")
        val next = ReminderTimeCalculator.nextReminder(now, ReminderTime(0, 1), utc)

        assertEquals(Instant.parse("2024-05-02T00:01:00Z"), next.triggerAt)
        assertEquals(90.seconds, next.delay)
    }

    // ---------- 跨月 / 跨年 ----------

    @Test
    fun `跨月边界顺延到次月`() {
        val now = Instant.parse("2024-05-31T23:00:00Z")
        val next = ReminderTimeCalculator.nextReminder(now, ReminderTime(21, 0), utc)

        assertEquals(Instant.parse("2024-06-01T21:00:00Z"), next.triggerAt)
        assertEquals(LocalDate(2024, 6, 1), next.date)
    }

    @Test
    fun `跨年边界顺延到次年`() {
        val now = Instant.parse("2024-12-31T23:30:00Z")
        val next = ReminderTimeCalculator.nextReminder(now, ReminderTime(21, 0), utc)

        assertEquals(Instant.parse("2025-01-01T21:00:00Z"), next.triggerAt)
        assertEquals(LocalDate(2025, 1, 1), next.date)
    }

    // ---------- 时区 ----------

    @Test
    fun `按用户本地时区判定日界而非 UTC`() {
        val tokyo = TimeZone.of("Asia/Tokyo")
        // 2024-05-01T13:00Z == 东京 2024-05-01 22:00，已过 21:00 -> 次日东京 21:00。
        val now = Instant.parse("2024-05-01T13:00:00Z")
        val next = ReminderTimeCalculator.nextReminder(now, ReminderTime(21, 0), tokyo)

        assertEquals(Instant.parse("2024-05-02T12:00:00Z"), next.triggerAt)
        assertEquals(LocalDate(2024, 5, 2), next.date)
        assertEquals(23.hours, next.delay)
    }

    @Test
    fun `同一时刻在不同时区得到不同的触发日`() {
        val now = Instant.parse("2024-05-01T23:30:00Z")
        val shanghai = ReminderTimeCalculator.nextReminder(now, ReminderTime(7, 0), TimeZone.of("Asia/Shanghai"))
        val losAngeles = ReminderTimeCalculator.nextReminder(now, ReminderTime(7, 0), TimeZone.of("America/Los_Angeles"))

        // 上海已是 5-02 07:30 -> 次日；洛杉矶还是 5-01 16:30 -> 当天。
        assertEquals(LocalDate(2024, 5, 3), shanghai.date)
        assertEquals(LocalDate(2024, 5, 2), losAngeles.date)
    }

    @Test
    fun `夏令时缺口不抛异常且落在缺口之后`() {
        val newYork = TimeZone.of("America/New_York")
        // 2024-03-10 本地 02:30 不存在（02:00 直接跳到 03:00）。
        val now = Instant.parse("2024-03-10T05:00:00Z")
        val next = ReminderTimeCalculator.nextReminder(now, ReminderTime(2, 30), newYork)

        assertEquals(LocalDate(2024, 3, 10), next.date)
        assertTrue(next.triggerAt >= Instant.parse("2024-03-10T07:00:00Z"), "缺口解析不得早于跳变点")
        assertTrue(next.triggerAt < Instant.parse("2024-03-10T08:00:00Z"), "缺口解析不得漂到一小时之外")
        assertTrue(next.delay > kotlin.time.Duration.ZERO)
    }

    // ---------- Clock 注入 ----------

    @Test
    fun `经 Clock 计算与直接传入时刻结果一致`() {
        val clock = FakeClock.at(LocalDate(2024, 5, 1), hour = 8, timeZone = utc)
        val viaClock = ReminderTimeCalculator.nextReminder(clock, ReminderTime(21, 0))
        val direct = ReminderTimeCalculator.nextReminder(clock.now(), ReminderTime(21, 0), utc)

        assertEquals(direct, viaClock)
        assertEquals(13.hours, ReminderTimeCalculator.delayUntilNext(clock, ReminderTime(21, 0)))
    }

    @Test
    fun `周期任务首次延迟不超过 24 小时`() {
        val clock = FakeClock.at(LocalDate(2024, 5, 1), hour = 8, timeZone = utc)
        val delay = ReminderTimeCalculator.initialDelayForPeriodic(clock, ReminderTime(21, 0))

        assertEquals(13.hours, delay)
        assertTrue(delay <= 24.hours)
    }

    @Test
    fun `延迟加当前时刻等于触发时刻`() {
        val now = Instant.parse("2024-05-01T05:07:11Z")
        val next = ReminderTimeCalculator.nextReminder(now, ReminderTime(23, 45), utc)

        assertEquals(next.triggerAt, now + next.delay)
    }

    // ---------- 设定时间解析与校验 ----------

    @Test
    fun `默认时间为晚上九点`() {
        assertEquals(21, ReminderTime.DEFAULT.hour)
        assertEquals(0, ReminderTime.DEFAULT.minute)
        assertEquals("21:00", ReminderTime.DEFAULT.toString())
    }

    @Test
    fun `非法小时与分钟被拒绝`() {
        assertFailsWith<IllegalArgumentException> { ReminderTime(24, 0) }
        assertFailsWith<IllegalArgumentException> { ReminderTime(-1, 0) }
        assertFailsWith<IllegalArgumentException> { ReminderTime(0, 60) }
        assertFailsWith<IllegalArgumentException> { ReminderTime(0, -1) }
    }

    @Test
    fun `合法范围边界值可用`() {
        assertEquals("00:00", ReminderTime(0, 0).toString())
        assertEquals("23:59", ReminderTime(23, 59).toString())
        assertEquals("07:05", ReminderTime(7, 5).toString())
    }

    @Test
    fun `解析合法字符串`() {
        assertEquals(ReminderTime(7, 5), ReminderTime.parseOrNull("07:05"))
        assertEquals(ReminderTime(23, 59), ReminderTime.parseOrNull("23:59"))
        assertEquals(ReminderTime(21, 0), ReminderTime.parseOrNull(" 21:00 "))
    }

    @Test
    fun `解析脏值返回 null 而不是崩溃`() {
        assertNull(ReminderTime.parseOrNull(""))
        assertNull(ReminderTime.parseOrNull("abc"))
        assertNull(ReminderTime.parseOrNull("25:00"))
        assertNull(ReminderTime.parseOrNull("07:60"))
        assertNull(ReminderTime.parseOrNull("7"))
        assertNull(ReminderTime.parseOrNull("07:00:00"))
        assertNull(ReminderTime.parseOrNull("--:--"))
    }

    @Test
    fun `脏值可回落到默认时间`() {
        val parsed = ReminderTime.parseOrNull("不是时间") ?: ReminderTime.DEFAULT

        assertEquals(ReminderTime.DEFAULT, parsed)
    }

    @Test
    fun `按分钟滚动时跨小时与跨天都回绕`() {
        assertEquals(ReminderTime(21, 5), ReminderTime(21, 0).shiftMinutes(5))
        assertEquals(ReminderTime(22, 0), ReminderTime(21, 0).shiftMinutes(60))
        assertEquals(ReminderTime(0, 0), ReminderTime(23, 55).shiftMinutes(5))
        assertEquals(ReminderTime(23, 55), ReminderTime(0, 0).shiftMinutes(-5))
        assertEquals(ReminderTime(20, 55), ReminderTime(21, 0).shiftMinutes(-5))
        assertEquals(ReminderTime(21, 0), ReminderTime(21, 0).shiftMinutes(0))
        assertEquals(ReminderTime(21, 0), ReminderTime(21, 0).shiftMinutes(24 * 60))
    }
}
