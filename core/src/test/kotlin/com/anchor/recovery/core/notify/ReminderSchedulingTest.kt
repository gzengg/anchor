package com.anchor.recovery.core.notify

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReminderSchedulingTest {

    @Test
    fun withoutRequestAlwaysInexact() {
        assertEquals(
            ReminderStrategy.INEXACT,
            ReminderScheduling.strategyFor(exactRequested = false, exactAllowed = false),
        )
        assertEquals(
            ReminderStrategy.INEXACT,
            ReminderScheduling.strategyFor(exactRequested = false, exactAllowed = true),
        )
    }

    @Test
    fun exactOnlyWhenRequestedAndAllowed() {
        assertEquals(
            ReminderStrategy.EXACT,
            ReminderScheduling.strategyFor(exactRequested = true, exactAllowed = true),
        )
        assertEquals(
            ReminderStrategy.INEXACT,
            ReminderScheduling.strategyFor(exactRequested = true, exactAllowed = false),
        )
    }

    @Test
    fun fallBackOnlyWhenRequestedButNotAllowed() {
        assertTrue(ReminderScheduling.shouldFallBack(exactRequested = true, exactAllowed = false))
        assertFalse(ReminderScheduling.shouldFallBack(exactRequested = true, exactAllowed = true))
        // 用户本来就没开精确提醒：权限没授权也不是「回落」，不该弹提示。
        assertFalse(ReminderScheduling.shouldFallBack(exactRequested = false, exactAllowed = false))
    }
}
