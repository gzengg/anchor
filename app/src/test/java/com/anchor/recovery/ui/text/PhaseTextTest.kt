package com.anchor.recovery.ui.text

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.anchor.recovery.core.phase.PhaseTextKey
import com.anchor.recovery.core.phase.WithdrawalPhase
import com.anchor.recovery.core.phase.WithdrawalPhaseCatalog
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 阶段文案 key → 文案资源的完整性与合规。
 *
 * 文案外移到 `values/strings_phases.xml` 后，`:core` 只剩 key，所以「每个 key 都有非空文案、
 * 参数够用、没有残留占位符」这套守卫跟着搬到这里；「不含疗效宣称」也一并从
 * `WithdrawalPhaseResolverTest` 搬过来。
 */
@RunWith(RobolectricTestRunner::class)
class PhaseTextTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private val phases: List<WithdrawalPhase> get() = WithdrawalPhaseCatalog.phases

    private val bannedWords = listOf("治愈", "疗效", "提升睾酮", "治疗成瘾", "根治")

    @Test
    fun `目录文案 key 与枚举一一对应`() {
        val used = phases.flatMap { phase ->
            listOfNotNull(phase.nameKey, phase.headlineKey, phase.cautionKey) +
                (phase.expectation + phase.coping).map { it.textKey }
        }

        assertEquals(PhaseTextKey.entries.size, used.size, "目录里的文案 key 有重复：$used")
        assertEquals(PhaseTextKey.entries.toSet(), used.toSet(), "枚举与目录的文案 key 不一致")
    }

    @Test
    fun `每个文案 key 都映射到独一无二的资源`() {
        val resources = PhaseTextKey.entries.map { phaseTextRes(it) }

        assertTrue(resources.all { it != 0 }, "存在未映射到资源的文案 key")
        assertEquals(resources.size, resources.distinct().size, "有多个 key 指向了同一个资源")
    }

    @Test
    fun `每个阶段文案都能渲染出非空文案且没有残留占位符`() {
        phases.forEach { phase ->
            val texts = listOf(
                "名称" to phaseNameRes(phase),
                "标题" to phaseHeadlineRes(phase),
            ) + listOfNotNull(phase.cautionKey?.let { "提示" to phaseTextRes(it) }) +
                phase.expectation.map { "可能经历" to phaseNoteRes(it) } +
                phase.coping.map { "可做的事" to phaseNoteRes(it) }

            texts.forEach { (label, res) ->
                val text = context.getString(res)

                assertTrue(text.isNotBlank(), "${phase.id} 的$label 文案为空")
                assertFalse(text.contains("%"), "${phase.id} 的$label 有未替换的占位符：$text")
            }
        }

        val notice = context.getString(phaseHelpSeekingNoticeRes())
        assertTrue(notice.isNotBlank(), "求助提示文案为空")
        assertFalse(notice.contains("%"), "求助提示有未替换的占位符：$notice")
    }

    @Test
    fun `天数区间文案按天数渲染`() {
        phases.forEach { phase ->
            val text = context.getString(
                phaseDayRangeRes(phase),
                *phaseDayRangeArgs(phase).toTypedArray(),
            )

            assertTrue(text.isNotBlank(), "${phase.id} 的天数区间文案为空")
            assertFalse(text.contains("%"), "${phase.id} 的天数区间有未替换的占位符：$text")
            assertTrue(
                text.contains(phase.minDay.toString()),
                "${phase.id} 的天数区间缺少起始天数：$text",
            )
            phase.maxDay?.let { maxDay ->
                assertTrue(
                    text.contains(maxDay.toString()),
                    "${phase.id} 的天数区间缺少结束天数：$text",
                )
            }
        }
    }

    @Test
    fun `阶段文案不含疗效宣称且高风险阶段给出求助指引`() {
        val allText = phases.flatMap { phase ->
            listOf(context.getString(phaseNameRes(phase)), context.getString(phaseHeadlineRes(phase))) +
                phase.expectation.map { context.getString(phaseNoteRes(it)) } +
                phase.coping.map { context.getString(phaseNoteRes(it)) }
        }

        allText.forEach { text ->
            bannedWords.forEach { word ->
                assertFalse(text.contains(word), "阶段文案出现禁用宣称「$word」：$text")
            }
        }

        val acuteCaution = context.getString(assertNotNull(phaseCautionRes(phases.first())))
        assertTrue(acuteCaution.contains("专业医生"), "急性期提示应给出求助指引")
        // 「不能替代诊疗」统一由卡片底部的求助提示显示，不在每条 caution 里重复。
        assertTrue(
            context.getString(phaseHelpSeekingNoticeRes()).contains("不能替代诊疗"),
            "卡片底部求助提示应说明不能替代诊疗",
        )
    }
}
