package com.anchor.recovery.ui.text

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.anchor.recovery.core.model.AssessmentType
import com.anchor.recovery.core.model.UrgeTool
import com.anchor.recovery.core.relapse.DayPart
import com.anchor.recovery.core.urge.BreathingCue
import com.anchor.recovery.core.urge.UrgeSurfingStage
import com.anchor.recovery.ui.TimelineFilter
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * `:core` 工具 / 日志侧枚举 → 文案资源的映射完整性。
 *
 * 与 `CoreTextTest` 同一套路：`:core` 只用枚举/常量表达可判定状态，文案全在
 * `values/strings_tools_journal.xml`。所有映射函数都返回无参数资源，因此这里只断言
 * 「非空 + 无残留占位符 + 枚举分支互不相同」；将来给某条文案加参数时，`%` 断言会先失败。
 */
@RunWith(RobolectricTestRunner::class)
class ToolsJournalTextTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun assertRes(res: Int, what: String) {
        val text = context.getString(res)
        assertTrue(text.isNotBlank(), "$what 文案为空")
        assertFalse(text.contains("%"), "$what 有未替换的占位符：$text")
    }

    @Test
    fun `每个冲浪阶段都有互不相同的阶段名与提示语`() {
        UrgeSurfingStage.entries.forEach { stage ->
            assertRes(urgeSurfingStageLabelRes(stage), "$stage 阶段名")
            assertRes(urgeSurfingStageHintRes(stage), "$stage 提示语")
        }
        assertEquals(
            UrgeSurfingStage.entries.size,
            UrgeSurfingStage.entries.map { urgeSurfingStageLabelRes(it) }.toSet().size,
            "阶段名不应重复",
        )
        assertEquals(
            UrgeSurfingStage.entries.size,
            UrgeSurfingStage.entries.map { urgeSurfingStageHintRes(it) }.toSet().size,
            "提示语不应重复",
        )
    }

    @Test
    fun `呼吸相位文案完整 未知相位按呼气兜底`() {
        assertRes(breathingPhaseLabelRes(BreathingCue.PHASE_IN), "吸气相位")
        assertRes(breathingPhaseLabelRes(BreathingCue.PHASE_HOLD), "停顿相位")
        assertRes(breathingPhaseLabelRes(BreathingCue.PHASE_OUT), "呼气相位")
        assertEquals(
            breathingPhaseLabelRes(BreathingCue.PHASE_OUT),
            breathingPhaseLabelRes(99),
            "未知相位应与 :core 原实现一致，按呼气渲染",
        )
        assertEquals(
            3,
            setOf(
                breathingPhaseLabelRes(BreathingCue.PHASE_IN),
                breathingPhaseLabelRes(BreathingCue.PHASE_HOLD),
                breathingPhaseLabelRes(BreathingCue.PHASE_OUT),
            ).size,
            "吸气 / 停 / 呼气 三条文案不应重复",
        )
    }

    @Test
    fun `每个时段都有互不相同的完整区间名与紧凑名`() {
        DayPart.entries.forEach { part ->
            assertRes(dayPartLabelRes(part), "$part 时段名")
            assertRes(dayPartShortRes(part), "$part 紧凑时段名")
        }
        assertEquals(
            DayPart.entries.size,
            DayPart.entries.map { dayPartShortRes(it) }.toSet().size,
            "紧凑时段名不应重复",
        )
    }

    @Test
    fun `每个工具都有互不相同的展示名`() {
        UrgeTool.entries.forEach { tool -> assertRes(urgeToolLabelRes(tool), "$tool 展示名") }
        assertEquals(
            UrgeTool.entries.size,
            UrgeTool.entries.map { urgeToolLabelRes(it) }.toSet().size,
            "工具展示名不应重复",
        )
    }

    @Test
    fun `每个问卷类型都有互不相同的展示名`() {
        AssessmentType.entries.forEach { type -> assertRes(assessmentTypeLabelRes(type), "$type 展示名") }
        assertEquals(
            AssessmentType.entries.size,
            AssessmentType.entries.map { assessmentTypeLabelRes(it) }.toSet().size,
            "问卷类型展示名不应重复",
        )
    }

    @Test
    fun `每个日志筛选项都有非空文案`() {
        TimelineFilter.entries.forEach { filter -> assertRes(filter.label, "$filter 筛选文案") }
    }
}
