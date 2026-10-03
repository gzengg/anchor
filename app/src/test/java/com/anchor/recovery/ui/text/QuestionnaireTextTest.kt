package com.anchor.recovery.ui.text

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.anchor.recovery.R
import com.anchor.recovery.core.assessment.CsbdDimension
import com.anchor.recovery.core.assessment.CsbdLevel
import com.anchor.recovery.core.assessment.CsbdQuestionnaire
import com.anchor.recovery.core.assessment.MoralIncongruenceScale
import com.anchor.recovery.core.assessment.MoralItemKind
import com.anchor.recovery.core.assessment.MoralQuadrant
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 问卷文案（题干 / 维度名 / 时间窗 / 免责说明 / 等级 / 象限）→ 资源的映射完整性。
 *
 * `:core` 只保留稳定题号与枚举，文案在 `values/strings_questionnaire.xml`。这里对全部题号与
 * 全部枚举取值断言「有非空文案、无残留占位符」，并固化原来的合规守卫（价值观冲突不等于成瘾）——
 * 守卫随文案一起从 `:core` 搬到这里。
 */
@RunWith(RobolectricTestRunner::class)
class QuestionnaireTextTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun text(res: Int): String = context.getString(res)

    private fun assertClean(text: String, subject: Any) {
        assertTrue(text.isNotBlank(), "$subject 文案为空")
        assertFalse(text.contains("%"), "$subject 有未替换占位符：$text")
    }

    @Test
    fun `F7 每题都有非空题干`() {
        assertEquals((1..19).toList(), CsbdQuestionnaire.questions.map { it.id })

        CsbdQuestionnaire.questions.forEach { question ->
            val res = csbdQuestionRes(question.id)
            assertTrue(res != 0, "第 ${question.id} 题没有映射到资源")
            assertClean(text(res), "F7 第 ${question.id} 题")
        }
    }

    @Test
    fun `F8 每题都有非空题干`() {
        assertEquals((1..12).toList(), MoralIncongruenceScale.questions.map { it.id })

        MoralIncongruenceScale.questions.forEach { question ->
            val res = moralQuestionRes(question.id)
            assertTrue(res != 0, "第 ${question.id} 题没有映射到资源")
            assertClean(text(res), "F8 第 ${question.id} 题")
        }
    }

    @Test
    fun `维度名都有非空文案`() {
        CsbdDimension.entries.forEach { dimension ->
            assertClean(text(csbdDimensionRes(dimension)), "F7 维度 $dimension")
        }
        MoralItemKind.entries.forEach { kind ->
            assertClean(text(moralItemKindRes(kind)), "F8 维度 $kind")
        }
    }

    @Test
    fun `F7 等级名 分区间 建议都有非空文案`() {
        CsbdLevel.entries.forEach { level ->
            assertClean(text(csbdLevelLabelRes(level)), "$level 等级名")
            assertClean(text(csbdLevelRangeRes(level)), "$level 分区间")
            assertClean(text(csbdLevelAdviceRes(level)), "$level 建议")
        }
    }

    @Test
    fun `F8 四象限名与解读都有非空文案`() {
        MoralQuadrant.entries.forEach { quadrant ->
            assertClean(text(moralQuadrantLabelRes(quadrant)), "$quadrant 象限名")
            assertClean(text(moralQuadrantInterpretationRes(quadrant)), "$quadrant 解读")
        }
    }

    @Test
    fun `时间窗与免责说明都有非空文案`() {
        listOf(
            R.string.csbd_time_window,
            R.string.moral_time_window,
            R.string.csbd_disclaimer,
            R.string.moral_disclaimer,
        ).forEach { res ->
            assertClean(text(res), "资源 $res")
        }
    }

    @Test
    fun `等级 象限 维度 映射一一对应且不重复`() {
        assertEquals(
            CsbdDimension.entries.size,
            CsbdDimension.entries.map { csbdDimensionRes(it) }.distinct().size,
        )
        assertEquals(CsbdLevel.entries.size, CsbdLevel.entries.map { csbdLevelLabelRes(it) }.distinct().size)
        assertEquals(CsbdLevel.entries.size, CsbdLevel.entries.map { csbdLevelRangeRes(it) }.distinct().size)
        assertEquals(CsbdLevel.entries.size, CsbdLevel.entries.map { csbdLevelAdviceRes(it) }.distinct().size)
        assertEquals(MoralItemKind.entries.size, MoralItemKind.entries.map { moralItemKindRes(it) }.distinct().size)
        assertEquals(MoralQuadrant.entries.size, MoralQuadrant.entries.map { moralQuadrantLabelRes(it) }.distinct().size)
        assertEquals(
            MoralQuadrant.entries.size,
            MoralQuadrant.entries.map { moralQuadrantInterpretationRes(it) }.distinct().size,
        )
    }

    @Test
    fun `阈值口径模板的参数够用且无残留占位符`() {
        val csbd = context.getString(
            R.string.csbd_threshold_note,
            text(csbdLevelRangeRes(CsbdLevel.LOW)),
            text(csbdLevelRangeRes(CsbdLevel.WATCH)),
            text(csbdLevelRangeRes(CsbdLevel.HIGH)),
            text(csbdLevelRangeRes(CsbdLevel.URGENT)),
        )
        assertClean(csbd, "F7 阈值口径")
        assertTrue(csbd.contains(text(csbdLevelRangeRes(CsbdLevel.LOW))))

        val moral = context.getString(
            R.string.moral_threshold_note,
            MoralIncongruenceScale.maxScorePerKind,
            MoralIncongruenceScale.HIGH_THRESHOLD,
        )
        assertClean(moral, "F8 阈值口径")
        assertTrue(moral.contains(MoralIncongruenceScale.HIGH_THRESHOLD.toString()))
    }

    /**
     * 合规守卫（原本在 :core 的 `MoralIncongruenceScorerTest`）：价值观冲突不得被病理化，
     * 该象限必须明确「价值观冲突不等于成瘾」。文案外移到资源后守卫跟着搬到这里。
     */
    @Test
    fun `价值观冲突象限必须写明不等于成瘾`() {
        val text = text(moralQuadrantInterpretationRes(MoralQuadrant.HIGH_MORAL_ONLY))

        assertTrue(
            text.contains("价值观冲突不等于成瘾"),
            "该象限必须明确价值观冲突不等于成瘾：$text",
        )
    }
}
