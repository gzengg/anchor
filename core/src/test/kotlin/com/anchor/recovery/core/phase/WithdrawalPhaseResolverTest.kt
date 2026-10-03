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
                name = "全部",
                minDay = 0,
                maxDay = null,
                headline = "h",
                expectation = listOf(PhaseNote("t", listOf("cases-001"))),
                coping = listOf(PhaseNote("c", listOf("cases-001"))),
            ),
        )

        // 单个开放区间是合法的
        assertEquals("ONLY", WithdrawalPhaseResolver(openEndedFirst).resolve(10_000).id)

        // 但后面还有阶段时，前一个阶段不能没有上界
        val overlapping = listOf(
            WithdrawalPhase("A", "a", 0, null, "h", listOf(PhaseNote("t", listOf("cases-001"))), emptyList()),
            WithdrawalPhase("B", "b", 10, null, "h", listOf(PhaseNote("t", listOf("cases-001"))), emptyList()),
        )
        assertFailsWith<IllegalArgumentException> { WithdrawalPhaseResolver(overlapping) }
    }

    @Test
    fun `首个阶段必须从第0天开始`() {
        val shifted = listOf(
            WithdrawalPhase("A", "a", 1, null, "h", emptyList(), emptyList()),
        )

        assertFailsWith<IllegalArgumentException> { WithdrawalPhaseResolver(shifted) }
    }

    @Test
    fun `每个阶段都有预期反应与应对建议且逐条标注来源`() {
        assertEquals(5, resolver.phases.size)
        resolver.phases.forEach { phase ->
            assertTrue(phase.expectation.isNotEmpty(), "${phase.id} 缺预期反应")
            assertTrue(phase.coping.isNotEmpty(), "${phase.id} 缺应对建议")
            assertTrue(phase.headline.isNotBlank(), "${phase.id} 缺阶段标题")
            (phase.expectation + phase.coping).forEach { note ->
                assertTrue(note.text.isNotBlank(), "${phase.id} 出现空文案")
                assertTrue(note.sourceArticleIds.isNotEmpty(), "${phase.id} 的文案没标来源：${note.text}")
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
    fun `时间线内容必须引用 05-cases 实证材料`() {
        resolver.phases.forEach { phase ->
            assertTrue(
                phase.sourceArticleIds.any { it.startsWith("cases-") },
                "${phase.id} 没有引用 05-cases 的任何文章",
            )
        }
    }

    @Test
    fun `阶段文案不含疗效宣称且高风险阶段带求助提示`() {
        val forbidden = listOf("治愈", "疗效", "提升睾酮", "治疗成瘾", "根治")
        val allText = resolver.phases.flatMap { phase ->
            listOf(phase.headline, phase.caution.orEmpty()) +
                (phase.expectation + phase.coping).map { it.text }
        }

        allText.forEach { text ->
            forbidden.forEach { word ->
                assertTrue(!text.contains(word), "阶段文案出现禁用宣称「$word」：$text")
            }
        }

        val acute = resolver.resolve(0)
        assertNotNull(acute.caution)
        assertTrue(acute.caution.contains("专业医生"), "急性期提示应给出求助指引")
        // 「不能替代诊疗」统一由卡片底部的 HELP_SEEKING_NOTICE 显示，不在每条 caution 里重复。
        assertTrue(WithdrawalPhaseCatalog.HELP_SEEKING_NOTICE.contains("不能替代诊疗"))
    }
}
