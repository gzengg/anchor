package com.anchor.recovery.core.streak

import com.anchor.recovery.core.clock.FakeClock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StreakCalculatorTest {

    private val zone: TimeZone = TimeZone.of("Asia/Shanghai")
    private val today: LocalDate = LocalDate.parse("2024-06-15")

    private fun clock(at: LocalDate = today, hour: Int = 12): FakeClock =
        FakeClock.at(at, hour = hour, timeZone = zone)

    private fun day(offsetFromToday: Int): LocalDate =
        LocalDate.fromEpochDays(today.toEpochDays() + offsetFromToday)

    private fun relapseAt(date: LocalDate, hour: Int = 22): Instant =
        LocalDateTime(date, kotlinx.datetime.LocalTime(hour, 0)).toInstant(zone)

    private fun days(vararg offsets: Int): List<LocalDate> = offsets.map { day(it) }

    @Test
    fun `空历史返回全零状态`() {
        val state = StreakCalculator(clock()).compute(emptyList(), emptyList())
        assertEquals(0, state.currentDays)
        assertEquals(0, state.longestDays)
        assertEquals(0, state.totalCheckInDays)
        assertFalse(state.todayCheckedIn)
        assertNull(state.lastCheckInDate)
        assertNull(state.lastRelapseDate)
        assertNull(state.daysSinceLastRelapse)
    }

    @Test
    fun `只有复吸记录时当前 streak 为零并给出距复吸天数`() {
        val state = StreakCalculator(clock()).compute(emptyList(), listOf(relapseAt(day(-2))))
        assertEquals(0, state.currentDays)
        assertEquals(0, state.longestDays)
        assertEquals(day(-2), state.lastRelapseDate)
        assertEquals(2, state.daysSinceLastRelapse)
    }

    @Test
    fun `连续三天含今天打卡`() {
        val state = StreakCalculator(clock()).compute(days(-2, -1, 0), emptyList())
        assertEquals(3, state.currentDays)
        assertEquals(3, state.longestDays)
        assertEquals(3, state.totalCheckInDays)
        assertEquals(today, state.lastCheckInDate)
        assertTrue(state.todayCheckedIn)
    }

    @Test
    fun `漏打一整天即断签`() {
        val state = StreakCalculator(clock()).compute(days(-4, -3, 0), emptyList())
        assertEquals(1, state.currentDays)
        assertEquals(2, state.longestDays)
    }

    @Test
    fun `昨天打过卡今天还没打时 streak 仍然有效`() {
        val state = StreakCalculator(clock()).compute(days(-2, -1), emptyList())
        assertEquals(2, state.currentDays)
        assertEquals(2, state.longestDays)
        assertEquals(day(-1), state.lastCheckInDate)
        assertFalse(state.todayCheckedIn)
    }

    @Test
    fun `最近一次打卡早于昨天则当前 streak 归零`() {
        val state = StreakCalculator(clock()).compute(days(-2), emptyList())
        assertEquals(0, state.currentDays)
        assertEquals(1, state.longestDays)
    }

    @Test
    fun `同日重复打卡按自然日去重`() {
        val state = StreakCalculator(clock()).compute(
            listOf(day(-1), day(-1), day(0), day(0)),
            emptyList(),
        )
        assertEquals(2, state.totalCheckInDays)
        assertEquals(2, state.currentDays)
        assertEquals(2, state.longestDays)
    }

    @Test
    fun `乱序输入不影响结果`() {
        val sorted = StreakCalculator(clock()).compute(days(-2, -1, 0), emptyList())
        val shuffled = StreakCalculator(clock()).compute(days(0, -2, -1), emptyList())
        assertEquals(sorted, shuffled)
    }

    @Test
    fun `当天复吸清零当天打卡不增加当前 streak`() {
        val state = StreakCalculator(clock()).compute(
            days(-1, 0),
            listOf(relapseAt(today, hour = 23)),
        )
        assertEquals(0, state.currentDays)
        assertEquals(2, state.longestDays, "复吸前一刻已连续两天，计入历史最长")
        assertEquals(0, state.daysSinceLastRelapse)
        assertTrue(state.todayCheckedIn)
    }

    @Test
    fun `昨天复吸今天起算为一天`() {
        val state = StreakCalculator(clock()).compute(
            days(-3, -2, -1, 0),
            listOf(relapseAt(day(-1), hour = 20)),
        )
        assertEquals(1, state.currentDays)
        assertEquals(3, state.longestDays, "复吸前一天为止的最长 streak 应保留")
        assertEquals(1, state.daysSinceLastRelapse)
    }

    @Test
    fun `复吸落在某打卡日时该日清零并从次日起算`() {
        val state = StreakCalculator(clock()).compute(
            days(-2, -1, 0),
            listOf(relapseAt(day(-2), hour = 20)),
        )
        assertEquals(2, state.currentDays)
        assertEquals(2, state.longestDays)
    }

    @Test
    fun `跨月边界按自然日连续`() {
        val monthEnd = LocalDate.parse("2024-01-31")
        val state = StreakCalculator(FakeClock.at(LocalDate.parse("2024-02-01"), timeZone = zone))
            .compute(listOf(monthEnd, LocalDate.parse("2024-02-01")), emptyList())
        assertEquals(2, state.currentDays)
        assertEquals(2, state.longestDays)
    }

    @Test
    fun `最长 streak 跨复吸重置持续追踪`() {
        // 前段 5 天（-7..-3），复吸落在 -3，后段 -2..0 共 3 天
        val state = StreakCalculator(clock()).compute(
            days(-7, -6, -5, -4, -3, -2, -1, 0),
            listOf(relapseAt(day(-3), hour = 21)),
        )
        assertEquals(3, state.currentDays)
        assertEquals(5, state.longestDays)
        assertEquals(8, state.totalCheckInDays)
    }

    @Test
    fun `23点59分与次日0点01分落在不同自然日`() {
        val clock = FakeClock.at(LocalDate.parse("2024-03-01"), hour = 23, minute = 59, timeZone = zone)
        assertEquals(LocalDate.parse("2024-03-01"), clock.today())

        clock.advanceBy(kotlin.time.Duration.parse("2m"))
        assertEquals(LocalDate.parse("2024-03-02"), clock.today())

        // 复吸时刻按本地时区换算自然日：23:59 后两分钟属于 3 月 2 日
        val relapse = clock.now()
        assertEquals(LocalDate.parse("2024-03-02"), clock.localDateOf(relapse))

        val state = StreakCalculator(clock).compute(
            listOf(LocalDate.parse("2024-03-01")),
            listOf(relapse),
        )
        assertEquals(0, state.currentDays, "复吸日即今天，当前 streak 为 0")
        assertEquals(1, state.longestDays)
    }

    @Test
    fun `日界前一刻打卡仍算前一天`() {
        val clock = FakeClock.at(LocalDate.parse("2024-03-01"), hour = 23, minute = 59, timeZone = zone)
        val state = StreakCalculator(clock).compute(
            listOf(LocalDate.parse("2024-02-29"), LocalDate.parse("2024-03-01")),
            emptyList(),
        )
        assertEquals(2, state.currentDays)
        assertTrue(state.todayCheckedIn)
    }
}
