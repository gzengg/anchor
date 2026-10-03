package com.anchor.recovery.core.relapse

import com.anchor.recovery.core.clock.FakeClock
import com.anchor.recovery.core.model.RelapseRecord
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * F5 统计逻辑：Top-N 排序、时段分布、复吸间隔中位数（含奇数/偶数与空日志）。
 * 时区固定 Asia/Shanghai（UTC+8），便于构造跨时段用例。
 */
class TriggerAnalyzerTest {

    private val clock = FakeClock.at(LocalDate(2026, 5, 1), hour = 12)
    private val analyzer = TriggerAnalyzer(clock, topN = 3)

    @Test
    fun `空日志返回空统计而不抛异常`() {
        val insight = analyzer.analyze(emptyList())
        assertTrue(insight.isEmpty)
        assertEquals(0, insight.totalRelapses)
        assertTrue(insight.emotionTop.isEmpty())
        assertTrue(insight.triggerTop.isEmpty())
        assertNull(insight.medianIntervalHours)
        assertNull(insight.busiestDayPart)
        assertNull(analyzer.medianIntervalHours(emptyList()))
        assertNull(analyzer.medianIntervalHours(listOf(instant("2026-05-01T22:00"))))
    }

    @Test
    fun `情绪与触发源按频次降序取 Top-N`() {
        val relapses = listOf(
            relapse("2026-05-01T23:30", emotions = listOf("焦虑", "孤独"), triggers = listOf("深夜独处")),
            relapse("2026-05-03T23:40", emotions = listOf("焦虑"), triggers = listOf("深夜独处", "压力事件")),
            relapse("2026-05-05T14:00", emotions = listOf("无聊", "焦虑"), triggers = listOf("闲暇无聊")),
            relapse("2026-05-07T15:00", emotions = listOf("孤独"), triggers = listOf("压力事件")),
        )

        val insight = analyzer.analyze(relapses)
        assertEquals(4, insight.totalRelapses)
        assertEquals(listOf("焦虑" to 3, "孤独" to 2, "无聊" to 1), insight.emotionTop.map { it.label to it.count })
        assertEquals(listOf("压力事件" to 2, "深夜独处" to 2, "闲暇无聊" to 1), insight.triggerTop.map { it.label to it.count })
        assertEquals(3, insight.emotionTop.size)
    }

    @Test
    fun `频次相同按标签序稳定排序并忽略空白标签`() {
        val items = analyzer.topFrequencies(listOf("b", "a", "b", "a", "  ", "", " c "))
        assertEquals(listOf("a" to 2, "b" to 2, "c" to 1), items.map { it.label to it.count })
        assertEquals(2, analyzer.topFrequencies(listOf("a", "b"), limit = 2).size)
        assertTrue(analyzer.topFrequencies(emptyList()).isEmpty())
    }

    @Test
    fun `时段分布按上午下午晚上深夜四段统计`() {
        val relapses = listOf(
            relapse("2026-05-02T08:30"),
            relapse("2026-05-03T07:00"),
            relapse("2026-05-04T13:00"),
            relapse("2026-05-05T20:00"),
            relapse("2026-05-06T23:30"),
            relapse("2026-05-07T02:30"),
            relapse("2026-05-08T04:59"),
        )

        val insight = analyzer.analyze(relapses)
        assertEquals(2, insight.dayPartCounts[DayPart.MORNING])
        assertEquals(1, insight.dayPartCounts[DayPart.AFTERNOON])
        assertEquals(1, insight.dayPartCounts[DayPart.EVENING])
        assertEquals(3, insight.dayPartCounts[DayPart.LATE_NIGHT])
        assertEquals(DayPart.LATE_NIGHT, insight.busiestDayPart)
    }

    @Test
    fun `时段边界分别落在正确的分段`() {
        // 本地时间（UTC+8）：05:00 上午、11:59 上午、12:00 下午、17:59 下午、18:00 晚上、22:59 晚上、23:00 深夜、04:59 深夜
        assertEquals(DayPart.MORNING, analyzer.dayPartOf(instant("2026-05-01T05:00")))
        assertEquals(DayPart.MORNING, analyzer.dayPartOf(instant("2026-05-01T11:59")))
        assertEquals(DayPart.AFTERNOON, analyzer.dayPartOf(instant("2026-05-01T12:00")))
        assertEquals(DayPart.AFTERNOON, analyzer.dayPartOf(instant("2026-05-01T17:59")))
        assertEquals(DayPart.EVENING, analyzer.dayPartOf(instant("2026-05-01T18:00")))
        assertEquals(DayPart.EVENING, analyzer.dayPartOf(instant("2026-05-01T22:59")))
        assertEquals(DayPart.LATE_NIGHT, analyzer.dayPartOf(instant("2026-05-01T23:00")))
        assertEquals(DayPart.LATE_NIGHT, analyzer.dayPartOf(instant("2026-05-01T04:59")))
    }

    @Test
    fun `复吸间隔中位数取偶数个数的中间两值平均`() {
        // 三条记录 → 两个间隔：2 小时与 4 小时 → 中位数 3 小时
        val three = listOf(
            relapse("2026-05-01T20:00"),
            relapse("2026-05-01T22:00"),
            relapse("2026-05-02T02:00"),
        )
        assertEquals(3.0, analyzer.analyze(three).medianIntervalHours)

        // 四条记录 → 三个间隔：1 / 2 / 100 小时 → 中位数 2 小时
        val four = listOf(
            relapse("2026-05-01T00:00"),
            relapse("2026-05-01T01:00"),
            relapse("2026-05-01T03:00"),
            relapse("2026-05-05T07:00"),
        )
        assertEquals(2.0, analyzer.medianIntervalHours(four.map { it.occurredAt }))
        assertEquals(2.0, analyzer.analyze(four).medianIntervalHours)
    }

    @Test
    fun `复吸间隔中位数取奇数个数的中间值且不受输入顺序影响`() {
        val shuffled = listOf(
            relapse("2026-05-03T09:00"),
            relapse("2026-05-01T09:00"),
            relapse("2026-05-04T09:00"),
            relapse("2026-05-06T09:00"),
        )
        // 排序后间隔：48 / 24 / 48 小时 → 中位数 48
        assertEquals(48.0, analyzer.medianIntervalHours(shuffled.map { it.occurredAt }))

        assertEquals(2.0, analyzer.median(listOf(1.0, 2.0, 3.0)))
        assertEquals(2.5, analyzer.median(listOf(1.0, 2.0, 3.0, 4.0)))
        assertNull(analyzer.median(emptyList()))
    }

    @Test
    fun `统计卡同时统计带备注的记录数`() {
        val relapses = listOf(
            relapse("2026-05-01T20:00", note = "加班到很晚"),
            relapse("2026-05-02T20:00", note = " "),
        )
        assertEquals(1, analyzer.analyze(relapses).noteCount)
    }

    private fun relapse(
        localDateTime: String,
        emotions: List<String> = emptyList(),
        triggers: List<String> = emptyList(),
        note: String = "",
    ): RelapseRecord = RelapseRecord(
        occurredAt = instant(localDateTime),
        emotions = emotions,
        triggers = triggers,
        note = note,
    )

    /** 把「本地时间」字符串（Asia/Shanghai）转成 Instant。 */
    private fun instant(localDateTime: String): Instant = FakeClock.at(
        date = LocalDate.parse(localDateTime.substring(0, 10)),
        hour = localDateTime.substring(11, 13).toInt(),
        minute = localDateTime.substring(14, 16).toInt(),
    ).now()
}
