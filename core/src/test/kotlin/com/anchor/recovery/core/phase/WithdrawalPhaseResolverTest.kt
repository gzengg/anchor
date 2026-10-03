package com.anchor.recovery.core.phase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WithdrawalPhaseResolverTest {

    private val resolver = WithdrawalPhaseResolver()

    @Test
    fun `边界天数落在正确的阶段`() {
        val expected = mapOf(
            0 to "ACUTE",
            1 to "ACUTE",
            7 to "ACUTE",
            8 to "FLUCTUATION",
            29 to "FLUCTUATION",
            30 to "REPAIR",
            59 to "REPAIR",
            60 to "RECONNECT",
            89 to "RECONNECT",
            90 to "CONSOLIDATION",
            365 to "CONSOLIDATION",
            Int.MAX_VALUE to "CONSOLIDATION",
        )

        expected.forEach { (day, id) ->
            assertEquals(id, resolver.resolve(day).id, "第 $day 天应属于 $id")
        }
    }

    @Test
    fun `每个阶段的每一天都自洽`() {
        (0..200).forEach { day ->
            val phase = resolver.resolve(day)
            assertTrue(phase.contains(day), "第 $day 天应满足 ${phase.id} 的区间")
            assertEquals(phase.id, resolver.resolve(day + 0).id)
        }
    }

    @Test
    fun `负数天数兜底到急性期`() {
        assertEquals("ACUTE", resolver.resolve(-1).id)
        assertEquals("ACUTE", resolver.resolve(Int.MIN_VALUE).id)
        assertEquals(0, resolver.indexOf(-100))
    }

    @Test
    fun `阶段进度与下一阶段天数`() {
        assertEquals(8, resolver.nextTransitionDay(0))
        assertEquals(8, resolver.nextTransitionDay(7))
        assertEquals(6, resolver.daysUntilNextPhase(2))
        assertEquals(1, resolver.daysUntilNextPhase(89))
        assertNull(resolver.nextTransitionDay(90))
        assertNull(resolver.daysUntilNextPhase(365))

        assertEquals(0f, resolver.progressInPhase(0))
        assertEquals(0.5f, resolver.progressInPhase(4))
        assertEquals(0f, resolver.progressInPhase(90))
    }

    @Test
    fun `不连续的阶段目录在构造时就失败`() {
        val broken = listOf(
            WithdrawalPhaseCatalog.phases[0],
            WithdrawalPhaseCatalog.phases[2],
        )

        assertFailsWith<IllegalArgumentException> { WithdrawalPhaseResolver(broken) }
    }

    @Test
    fun `末尾阶段必须有上界否则构造失败`() {
        val openEndedFirst = listOf(
            WithdrawalPhase(
                id = "ONLY",
                nameKey = PhaseTextKey.REPAIR_NAME,
                minDay = 0,
                maxDay = null,
                headlineKey = PhaseTextKey.REPAIR_HEADLINE,
                expectation = listOf(PhaseNote(PhaseTextKey.REPAIR_COPING_1, listOf("cases-001"))),
                coping = listOf(PhaseNote(PhaseTextKey.REPAIR_COPING_2, listOf("cases-001"))),
            ),
        )

        // 单个开放区间是合法的
        assertEquals("ONLY", WithdrawalPhaseResolver(openEndedFirst).resolve(10_000).id)

        // 但后面还有阶段时，前一个阶段不能没有上界
        val overlapping = listOf(
            WithdrawalPhase(
                "A",
                PhaseTextKey.REPAIR_NAME,
                0,
                null,
                PhaseTextKey.REPAIR_HEADLINE,
                listOf(PhaseNote(PhaseTextKey.REPAIR_COPING_1, listOf("cases-001"))),
                emptyList(),
            ),
            WithdrawalPhase(
                "B",
                PhaseTextKey.CONSOLIDATION_NAME,
                10,
                null,
                PhaseTextKey.CONSOLIDATION_HEADLINE,
                listOf(PhaseNote(PhaseTextKey.CONSOLIDATION_COPING_1, listOf("cases-001"))),
                emptyList(),
            ),
        )
        assertFailsWith<IllegalArgumentException> { WithdrawalPhaseResolver(overlapping) }
    }

    @Test
    fun `首个阶段必须从第0天开始`() {
        val shifted = listOf(
            WithdrawalPhase(
                "A",
                PhaseTextKey.REPAIR_NAME,
                1,
                null,
                PhaseTextKey.REPAIR_HEADLINE,
                emptyList(),
                emptyList(),
            ),
        )

        assertFailsWith<IllegalArgumentException> { WithdrawalPhaseResolver(shifted) }
    }

    @Test
    fun `每个阶段都有预期反应与应对建议且逐条标注来源`() {
        assertEquals(5, resolver.phases.size)
        resolver.phases.forEach { phase ->
            assertTrue(phase.expectation.isNotEmpty(), "${phase.id} 缺预期反应")
            assertTrue(phase.coping.isNotEmpty(), "${phase.id} 缺应对建议")
            (phase.expectation + phase.coping).forEach { note ->
                assertTrue(
                    note.sourceArticleIds.isNotEmpty(),
                    "${phase.id} 的文案没标来源：${note.textKey}",
                )
                note.sourceArticleIds.forEach { id ->
                    assertTrue(
                        Regex("^(cases|methods)-\\d{3}$").matches(id),
                        "${phase.id} 的来源 id 格式非法：$id",
                    )
                }
            }
        }
    }

    @Test
    fun `阶段文案 key 在目录里不重复`() {
        val keys = resolver.phases.flatMap { phase ->
            listOfNotNull(phase.nameKey, phase.headlineKey, phase.cautionKey) +
                (phase.expectation + phase.coping).map { it.textKey }
        }

        assertEquals(keys.size, keys.distinct().size, "阶段文案 key 有重复：$keys")
    }

    @Test
    fun `时间线内容必须引用 05-cases 实证材料`() {
        resolver.phases.forEach { phase ->
            assertTrue(
                phase.sourceArticleIds.any { it.startsWith("cases-") },
                "${phase.id} 没有引用 05-cases 的任何文章",
            )
        }
    }

    @Test
    fun `高风险阶段带求助提示`() {
        assertNotNull(resolver.resolve(0).cautionKey, "急性期应带求助提示")
        assertNotNull(resolver.resolve(60).cautionKey, "重连期应带求助提示")
        // 「不含疗效宣称」「求助提示写到了什么」都是对资源文案的断言：文案外移后由
        // :app 的 PhaseTextTest 覆盖（本测试只保证关键阶段确实带上了 caution key）。
    }
}
