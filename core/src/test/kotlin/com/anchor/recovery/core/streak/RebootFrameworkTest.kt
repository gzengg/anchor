package com.anchor.recovery.core.streak

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RebootFrameworkTest {

    @Test
    fun `里程碑固定为一七三十六十天`() {
        assertEquals(listOf(1, 7, 30, 60, 90), RebootFramework.milestones.map { it.days })
        assertEquals(90, RebootFramework.GOAL_DAYS)
        assertEquals(
            RebootFramework.milestones.size,
            RebootFramework.milestones.map { it.days }.toSet().size,
            "里程碑天数不得重复",
        )
        assertTrue(RebootFramework.milestones.all { it.days > 0 }, "里程碑天数必须为正")
    }

    @Test
    fun `已达成里程碑按天数累计`() {
        assertEquals(emptyList(), RebootFramework.reached(0))
        assertEquals(listOf(1), RebootFramework.reached(1).map { it.days })
        assertEquals(listOf(1, 7, 30), RebootFramework.reached(59).map { it.days })
        assertEquals(listOf(1, 7, 30, 60, 90), RebootFramework.reached(90).map { it.days })
        assertEquals(listOf(1, 7, 30, 60, 90), RebootFramework.reached(365).map { it.days })
    }

    @Test
    fun `下一个里程碑在达成后切换`() {
        assertEquals(1, RebootFramework.next(0)?.days)
        assertEquals(7, RebootFramework.next(1)?.days)
        assertEquals(30, RebootFramework.next(7)?.days)
        assertEquals(60, RebootFramework.next(30)?.days)
        assertEquals(90, RebootFramework.next(60)?.days)
        assertNull(RebootFramework.next(90))
        assertNull(RebootFramework.next(120))
    }

    @Test
    fun `九十天进度被钳制在零到一`() {
        assertEquals(0f, RebootFramework.progress(0))
        assertEquals(0.5f, RebootFramework.progress(45))
        assertEquals(1f, RebootFramework.progress(90))
        assertEquals(1f, RebootFramework.progress(200))
        assertEquals(0f, RebootFramework.progress(-3))
    }
}
