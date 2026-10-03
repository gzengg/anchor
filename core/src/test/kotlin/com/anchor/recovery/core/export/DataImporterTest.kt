package com.anchor.recovery.core.export

import com.anchor.recovery.core.model.AssessmentRecord
import com.anchor.recovery.core.model.AssessmentType
import com.anchor.recovery.core.model.CheckInRecord
import com.anchor.recovery.core.model.RelapseRecord
import com.anchor.recovery.core.model.UrgeEpisodeRecord
import com.anchor.recovery.core.model.UrgeTool
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * 导入解析器的判定：能认的认、认不了的必须带原因拒绝，绝不静默丢数据。
 */
class DataImporterTest {

    private val exportedAt = Instant.parse("2024-05-04T09:31:00Z")

    private val checkIn = CheckInRecord(
        date = LocalDate(2024, 5, 3),
        note = "今天还行",
        createdAt = Instant.parse("2024-05-03T13:00:00Z"),
    )
    private val relapse = RelapseRecord(
        id = 7,
        occurredAt = Instant.parse("2024-04-30T22:10:00Z"),
        situation = "独处",
        emotions = listOf("焦虑", "空虚"),
        triggers = listOf("刷到擦边内容"),
        note = "事后写了复盘",
    )
    private val urge = UrgeEpisodeRecord(
        id = 3,
        startedAt = Instant.parse("2024-05-02T20:00:00Z"),
        durationSec = 600,
        peakIntensity = 8,
        endIntensity = 3,
        tool = UrgeTool.DELAY_TOOL,
    )
    private val assessment = AssessmentRecord(
        id = 2,
        type = AssessmentType.MORAL,
        takenAt = Instant.parse("2024-05-01T10:00:00Z"),
        totalScore = 21,
        level = "中度",
        answers = listOf(1, 2, 3, 4, 5),
    )

    private fun json(
        checkIns: List<CheckInRecord> = listOf(checkIn),
        relapses: List<RelapseRecord> = listOf(relapse),
        urgeEpisodes: List<UrgeEpisodeRecord> = listOf(urge),
        assessments: List<AssessmentRecord> = listOf(assessment),
    ): String = DataExporter.toJson(
        DataExporter.build(
            appName = "磐石",
            appVersion = "0.1.0",
            exportedAt = exportedAt,
            checkIns = checkIns,
            relapses = relapses,
            urgeEpisodes = urgeEpisodes,
            assessments = assessments,
            reminderEnabled = true,
            reminderTime = "21:30",
            motivationPrompts = listOf("先不做决定", "去洗个澡"),
        ),
    )

    @Test
    fun `导出再导入后四类记录与设置逐字段一致`() {
        val result = DataImporter.parse(json())

        val ready = assertIs<ImportResult.Ready>(result)
        assertEquals(DataExporter.SCHEMA_VERSION, ready.schemaVersion)
        assertEquals(exportedAt.toString(), ready.exportedAt)
        assertEquals(listOf(checkIn), ready.data.checkIns)
        assertEquals(listOf(relapse), ready.data.relapses)
        assertEquals(listOf(urge), ready.data.urgeEpisodes)
        assertEquals(listOf(assessment), ready.data.assessments)
        assertTrue(ready.data.reminderEnabled)
        assertEquals("21:30", ready.data.reminderTime)
        assertEquals(listOf("先不做决定", "去洗个澡"), ready.data.motivationPrompts)
        assertEquals("打卡 1 天 · 破戒 1 次 · 渴求 1 次 · 问卷 1 份", ready.data.countsLine())
    }

    @Test
    fun `空记录文件也能导入`() {
        val ready = assertIs<ImportResult.Ready>(
            DataImporter.parse(
                json(checkIns = emptyList(), relapses = emptyList(), urgeEpisodes = emptyList(), assessments = emptyList()),
            ),
        )

        assertTrue(ready.data.isEmpty)
    }

    @Test
    fun `当前未知的更高版本被拒绝并提示升级`() {
        val newer = json().replace("\"schemaVersion\": 1", "\"schemaVersion\": 99")

        val rejected = assertIs<ImportResult.Rejected>(DataImporter.parse(newer))
        assertTrue(rejected.reason.contains("v99"), rejected.reason)
        assertTrue(rejected.reason.contains("升级"), rejected.reason)
    }

    @Test
    fun `缺版本号被拒绝`() {
        val noVersion = json().replace("\"schemaVersion\": 1,", "")

        assertIs<ImportResult.Rejected>(DataImporter.parse(noVersion))
    }

    @Test
    fun `不是 JSON 的文件被拒绝且不抛异常`() {
        val rejected = assertIs<ImportResult.Rejected>(DataImporter.parse("这是我的备忘录，不是导出文件"))

        assertTrue(rejected.reason.contains("磐石"), rejected.reason)
    }

    @Test
    fun `空文件被拒绝`() {
        assertIs<ImportResult.Rejected>(DataImporter.parse("   \n"))
    }

    @Test
    fun `单条记录时间戳坏了整份拒绝而不是静默跳过`() {
        val broken = json().replace("\"2024-05-03T13:00:00Z\"", "\"昨天下午\"")

        val rejected = assertIs<ImportResult.Rejected>(DataImporter.parse(broken))
        assertTrue(rejected.reason.contains("打卡时间"), rejected.reason)
    }

    @Test
    fun `重复打卡日期被拒绝而不是默默丢掉一天`() {
        val duplicated = json(
            checkIns = listOf(checkIn, checkIn.copy(note = "同一天又打了一次")),
        )

        val rejected = assertIs<ImportResult.Rejected>(DataImporter.parse(duplicated))
        assertTrue(rejected.reason.contains("2024-05-03"), rejected.reason)
    }

    @Test
    fun `未知工具名与问卷类型按兜底值导入而不是崩溃`() {
        val strange = json()
            .replace("\"tool\": \"DELAY_TOOL\"", "\"tool\": \"FUTURE_TOOL\"")
            .replace("\"type\": \"MORAL\"", "\"type\": \"FUTURE_SCALE\"")

        val ready = assertIs<ImportResult.Ready>(DataImporter.parse(strange))
        assertEquals(UrgeTool.URGE_SURFING, ready.data.urgeEpisodes.single().tool)
        assertEquals(AssessmentType.CSBD, ready.data.assessments.single().type)
    }
}
