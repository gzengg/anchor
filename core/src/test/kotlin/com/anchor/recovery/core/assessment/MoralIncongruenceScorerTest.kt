package com.anchor.recovery.core.assessment

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** F8 道德冲突计分：四象限、维度阈值边界、缺答与越界异常。 */
class MoralIncongruenceScorerTest {

    private val itemCount = MoralIncongruenceScale.questionCount

    @Test
    fun `题项为 12 题且行为与道德各 6 题`() {
        assertEquals(12, itemCount)
        assertEquals((1..12).toList(), MoralIncongruenceScale.questions.map { it.id })
        assertEquals(6, MoralIncongruenceScale.ofKind(MoralItemKind.BEHAVIOR).size)
        assertEquals(6, MoralIncongruenceScale.ofKind(MoralItemKind.MORAL).size)
        assertEquals(24, MoralIncongruenceScale.maxScorePerKind)
        assertEquals(12, MoralIncongruenceScale.HIGH_THRESHOLD)
        assertTrue(MoralIncongruenceScale.questions.all { it.text.isNotBlank() })
    }

    @Test
    fun `行为高道德高为双高象限`() {
        val result = MoralIncongruenceScorer.score(answersWith(behavior = 20, moral = 16))

        assertEquals(MoralQuadrant.HIGH_BOTH, result.quadrant)
        assertTrue(result.behaviorHigh)
        assertTrue(result.moralHigh)
    }

    @Test
    fun `行为高道德低为行为影响象限`() {
        val result = MoralIncongruenceScorer.score(answersWith(behavior = 18, moral = 6))

        assertEquals(MoralQuadrant.HIGH_BEHAVIOR_ONLY, result.quadrant)
        assertTrue(result.behaviorHigh)
        assertFalse(result.moralHigh)
    }

    @Test
    fun `行为低道德高为价值观冲突象限`() {
        val result = MoralIncongruenceScorer.score(answersWith(behavior = 4, moral = 19))

        assertEquals(MoralQuadrant.HIGH_MORAL_ONLY, result.quadrant)
        assertFalse(result.behaviorHigh)
        assertTrue(result.moralHigh)
        assertTrue(
            result.quadrant.interpretation.contains("价值观冲突不等于成瘾"),
            "该象限必须明确价值观冲突不等于成瘾",
        )
    }

    @Test
    fun `行为低道德低为双低象限`() {
        val result = MoralIncongruenceScorer.score(answersWith(behavior = 0, moral = 11))

        assertEquals(MoralQuadrant.LOW_BOTH, result.quadrant)
        assertFalse(result.behaviorHigh)
        assertFalse(result.moralHigh)
    }

    @Test
    fun `维度阈值边界 11 与 12`() {
        val belowBehavior = MoralIncongruenceScorer.score(answersWith(behavior = 11, moral = 12))
        assertFalse(belowBehavior.behaviorHigh, "11 分不算偏高")
        assertTrue(belowBehavior.moralHigh, "12 分算偏高")
        assertEquals(MoralQuadrant.HIGH_MORAL_ONLY, belowBehavior.quadrant)

        val atBehavior = MoralIncongruenceScorer.score(answersWith(behavior = 12, moral = 11))
        assertTrue(atBehavior.behaviorHigh, "12 分算偏高")
        assertFalse(atBehavior.moralHigh, "11 分不算偏高")
        assertEquals(MoralQuadrant.HIGH_BEHAVIOR_ONLY, atBehavior.quadrant)
    }

    @Test
    fun `满分与零分的极端取值`() {
        val max = MoralIncongruenceScorer.score(answersWith(behavior = 24, moral = 24))
        assertEquals(24, max.behaviorScore)
        assertEquals(24, max.moralScore)
        assertEquals(MoralQuadrant.HIGH_BOTH, max.quadrant)

        val zero = MoralIncongruenceScorer.score(answersWith(behavior = 0, moral = 0))
        assertEquals(0, zero.behaviorScore)
        assertEquals(0, zero.moralScore)
        assertEquals(MoralQuadrant.LOW_BOTH, zero.quadrant)
    }

    @Test
    fun `两维度得分分别由前后各 6 题求出`() {
        val answers = MutableList(itemCount) { 0 }
        answers[0] = 4 // 第 1 题：行为
        answers[11] = 4 // 第 12 题：道德

        val result = MoralIncongruenceScorer.score(answers)

        assertEquals(4, result.behaviorScore)
        assertEquals(4, result.moralScore)
        assertEquals(8, answers.sum())
    }

    @Test
    fun `缺答抛异常`() {
        assertFailsWith<IllegalArgumentException> { MoralIncongruenceScorer.score(List(itemCount - 1) { 0 }) }
        assertFailsWith<IllegalArgumentException> { MoralIncongruenceScorer.score(List(itemCount + 1) { 0 }) }
        assertFailsWith<IllegalArgumentException> { MoralIncongruenceScorer.score(emptyList()) }
    }

    @Test
    fun `答案越界抛异常`() {
        assertFailsWith<IllegalArgumentException> {
            MoralIncongruenceScorer.score(MutableList(itemCount) { 0 }.also { it[0] = 5 })
        }
        assertFailsWith<IllegalArgumentException> {
            MoralIncongruenceScorer.score(MutableList(itemCount) { 0 }.also { it[7] = -2 })
        }
    }

    /** 生成行为维度总分 [behavior]、道德维度总分 [moral] 的作答（每题 0–4）。 */
    private fun answersWith(behavior: Int, moral: Int): List<Int> =
        distribute(behavior, MoralItemKind.BEHAVIOR) + distribute(moral, MoralItemKind.MORAL)

    private fun distribute(total: Int, kind: MoralItemKind): List<Int> {
        var remaining = total.coerceIn(0, MoralIncongruenceScale.maxScorePerKind)
        return List(kind.questionCount) {
            val value = minOf(MoralIncongruenceScale.MAX_PER_ITEM, remaining)
            remaining -= value
            value
        }
    }
}
