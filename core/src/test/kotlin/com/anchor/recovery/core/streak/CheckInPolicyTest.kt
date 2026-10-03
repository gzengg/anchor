package com.anchor.recovery.core.streak

import com.anchor.recovery.core.clock.FakeClock
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CheckInPolicyTest {

    private val today: LocalDate = LocalDate.parse("2024-06-15")
    private val clock = FakeClock.at(today)

    private fun day(offset: Int): LocalDate = LocalDate.fromEpochDays(today.toEpochDays() + offset)

    @Test
    fun `今天未打卡时允许打卡`() {
        val decision = CheckInPolicy(clock).evaluate(listOf(day(-1)))
        assertIs<CheckInDecision.Allowed>(decision)
        assertEquals(today, decision.date)
    }

    @Test
    fun `今天已打卡时返回已打卡`() {
        val decision = CheckInPolicy(clock).evaluate(listOf(day(-1), today))
        assertIs<CheckInDecision.AlreadyCheckedIn>(decision)
        assertEquals(today, decision.date)
    }

    @Test
    fun `未来日期被拒绝`() {
        val decision = CheckInPolicy(clock).evaluate(emptyList(), day(1))
        assertIs<CheckInDecision.Rejected>(decision)
        assertEquals(CheckInRejection.FUTURE_DATE, decision.reason)
    }

    @Test
    fun `过去日期不支持补打卡`() {
        val decision = CheckInPolicy(clock).evaluate(emptyList(), day(-1))
        assertIs<CheckInDecision.Rejected>(decision)
        assertEquals(CheckInRejection.NOT_TODAY, decision.reason)
    }

    @Test
    fun `未指定日期时默认使用今天`() {
        val decision = CheckInPolicy(clock).evaluate(emptyList())
        assertIs<CheckInDecision.Allowed>(decision)
        assertEquals(today, decision.date)
    }
}
