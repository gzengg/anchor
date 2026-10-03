package com.anchor.recovery.core.assessment

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** F7 CSBD 自评计分：分级边界、维度小计、缺答与越界异常。 */
class CsbdScorerTest {

    private val itemCount = CsbdQuestionnaire.questionCount

    @Test
    fun `题项为 19 题 5 维且 id 连续`() {
        assertEquals(19, itemCount, "题项应为 19 题")
        assertEquals(19, CsbdQuestionnaire.questions.map { it.id }.distinct().size)
        assertEquals((1..19).toList(), CsbdQuestionnaire.questions.map { it.id })
        assertEquals(5, CsbdDimension.entries.size, "维度应为 5 个")
        CsbdDimension.entries.forEach { dimension ->
            val actual = CsbdQuestionnaire.questions.count { it.dimension == dimension }
            assertEquals(dimension.questionCount, actual, "维度 $dimension 题数不一致")
        }
        assertEquals(76, CsbdQuestionnaire.maxTotalScore)
        assertFalse(CsbdQuestionnaire.questions.any { it.reverseScored }, "本问卷不使用反向计分题")
    }

    @Test
    fun `全 0 作答为低风险且各维度为 0`() {
        val result = CsbdScorer.score(List(itemCount) { 0 })

        assertEquals(0, result.totalScore)
        assertEquals(CsbdLevel.LOW, result.level)
        assertEquals(CsbdDimension.entries.associateWith { 0 }, result.dimensionScores)
        assertEquals(0, result.topDimensionScore)
    }

    @Test
    fun `满分作答为强烈建议就医且维度小计之和等于总分`() {
        val result = CsbdScorer.score(List(itemCount) { CsbdQuestionnaire.MAX_PER_ITEM })

        assertEquals(76, result.totalScore)
        assertEquals(CsbdLevel.URGENT, result.level)
        assertEquals(16, result.dimensionScores[CsbdDimension.CONTROL])
        assertEquals(16, result.dimensionScores[CsbdDimension.COPING])
        assertEquals(16, result.dimensionScores[CsbdDimension.COMPULSIVE])
        assertEquals(16, result.dimensionScores[CsbdDimension.CONSEQUENCE])
        assertEquals(12, result.dimensionScores[CsbdDimension.ATTEMPTS])
        assertEquals(result.totalScore, result.dimensionScores.values.sum())
    }

    @Test
    fun `分级边界 19 与 20`() {
        assertEquals(CsbdLevel.LOW, CsbdScorer.score(answersWithTotal(19)).level)
        assertEquals(CsbdLevel.WATCH, CsbdScorer.score(answersWithTotal(20)).level)
    }

    @Test
    fun `分级边界 39 与 40`() {
        assertEquals(CsbdLevel.WATCH, CsbdScorer.score(answersWithTotal(39)).level)
        assertEquals(CsbdLevel.HIGH, CsbdScorer.score(answersWithTotal(40)).level)
    }

    @Test
    fun `分级边界 55 与 56`() {
        assertEquals(CsbdLevel.HIGH, CsbdScorer.score(answersWithTotal(55)).level)
        assertEquals(CsbdLevel.URGENT, CsbdScorer.score(answersWithTotal(56)).level)
    }

    @Test
    fun `分数单调递增且维度小计之和始终等于总分`() {
        val samples = listOf(
            answersWithTotal(0),
            answersWithTotal(7),
            answersWithTotal(23),
            answersWithTotal(48),
            answersWithTotal(76),
        )

        var previous = -1
        samples.forEach { answers ->
            val result = CsbdScorer.score(answers)
            assertEquals(answers.sum(), result.totalScore)
            assertEquals(result.totalScore, result.dimensionScores.values.sum())
            assertTrue(result.totalScore > previous, "总分应随作答递增")
            previous = result.totalScore
        }
    }

    @Test
    fun `最高维度指向实际得分最高的维度`() {
        val answers = MutableList(itemCount) { 0 }
        // 只在"情绪应对"维度（第 5–8 题）作答 3 分。
        (5..8).forEach { answers[it - 1] = 3 }

        val result = CsbdScorer.score(answers)

        assertEquals(12, result.totalScore)
        assertEquals(CsbdDimension.COPING, result.topDimension)
        assertEquals(12, result.topDimensionScore, "维度小计（4 题 × 3 分）")
    }

    @Test
    fun `缺答抛异常`() {
        assertFailsWith<IllegalArgumentException> { CsbdScorer.score(List(itemCount - 1) { 0 }) }
        assertFailsWith<IllegalArgumentException> { CsbdScorer.score(List(itemCount + 1) { 0 }) }
        assertFailsWith<IllegalArgumentException> { CsbdScorer.score(emptyList()) }
    }

    @Test
    fun `答案越界抛异常`() {
        assertFailsWith<IllegalArgumentException> {
            CsbdScorer.score(MutableList(itemCount) { 0 }.also { it[3] = -1 })
        }
        assertFailsWith<IllegalArgumentException> {
            CsbdScorer.score(MutableList(itemCount) { 0 }.also { it[10] = 5 })
        }
    }

    /** 生成总分恰好为 [total] 的作答（每题 0–4，超出的部分不再计分）。 */
    private fun answersWithTotal(total: Int): List<Int> {
        var remaining = total.coerceIn(0, CsbdQuestionnaire.maxTotalScore)
        return List(itemCount) {
            val value = minOf(CsbdQuestionnaire.MAX_PER_ITEM, remaining)
            remaining -= value
            value
        }
    }
}
