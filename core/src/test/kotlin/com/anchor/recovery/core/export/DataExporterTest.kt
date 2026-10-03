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
import kotlin.test.assertTrue

class DataExporterTest {

    private val exportedAt = Instant.parse("2024-05-04T09:31:00Z")

    private fun payload(
        checkIns: List<CheckInRecord> = emptyList(),
        relapses: List<RelapseRecord> = emptyList(),
        urgeEpisodes: List<UrgeEpisodeRecord> = emptyList(),
        assessments: List<AssessmentRecord> = emptyList(),
    ) = DataExporter.build(
        appName = "磐石",
        appVersion = "0.1.0",
        exportedAt = exportedAt,
        checkIns = checkIns,
        relapses = relapses,
        urgeEpisodes = urgeEpisodes,
        assessments = assessments,
        reminderEnabled = true,
        reminderTime = "21:00",
        motivationPrompts = listOf("先不做决定"),
    )

    @Test
    fun `空数据也能导出且计数为零`() {
        val payload = payload()

        assertTrue(payload.checkIns.isEmpty())
        assertTrue(payload.relapses.isEmpty())
        assertEquals("打卡 0 天 · 复吸 0 次 · 渴求 0 次 · 问卷 0 份", payload.countsLine())
        assertEquals(DataExporter.SCHEMA_VERSION, payload.schemaVersion)
        assertEquals("磐石", payload.app)
    }

    @Test
    fun `四类记录与设置都被导出`() {
        val payload = payload(
            checkIns = listOf(
                CheckInRecord(
                    date = LocalDate(2024, 5, 3),
                    note = "今天还行",
                    createdAt = Instant.parse("2024-05-03T13:00:00Z"),
                ),
            ),
            relapses = listOf(
                RelapseRecord(
                    id = 7,
                    occurredAt = Instant.parse("2024-05-02T15:20:00Z"),
                    situation = "独自在家",
                    emotions = listOf("焦虑", "无聊"),
                    triggers = listOf("深夜"),
                    note = "睡前",
                ),
            ),
            urgeEpisodes = listOf(
                UrgeEpisodeRecord(
                    id = 9,
                    startedAt = Instant.parse("2024-05-02T14:00:00Z"),
                    durationSec = 128,
                    peakIntensity = 8,
                    endIntensity = 3,
                    tool = UrgeTool.URGE_SURFING,
                ),
            ),
            assessments = listOf(
                AssessmentRecord(
                    id = 2,
                    type = AssessmentType.MORAL,
                    takenAt = Instant.parse("2024-05-01T10:00:00Z"),
                    totalScore = 25,
                    level = "HIGH_BEHAVIOR_ONLY",
                    answers = listOf(4, 3, 2, 1, 4, 3, 1, 0, 2, 1, 3, 1),
                ),
            ),
        )

        assertEquals("2024-05-03", payload.checkIns.single().date)
        assertEquals(listOf("焦虑", "无聊"), payload.relapses.single().emotions)
        assertEquals("URGE_SURFING", payload.urgeEpisodes.single().tool)
        assertEquals(12, payload.assessments.single().answers.size)
        assertTrue(payload.settings.reminderEnabled)
        assertEquals("打卡 1 天 · 复吸 1 次 · 渴求 1 次 · 问卷 1 份", payload.countsLine())
    }

    @Test
    fun `JSON 往返后内容完全一致`() {
        val original = payload(
            checkIns = listOf(
                CheckInRecord(LocalDate(2024, 5, 3), "", Instant.parse("2024-05-03T13:00:00Z")),
            ),
            relapses = listOf(
                RelapseRecord(1, Instant.parse("2024-05-02T15:20:00Z"), "情境", listOf("焦虑"), listOf("深夜"), "备注"),
            ),
        )

        val restored = DataExporter.fromJson(DataExporter.toJson(original))

        assertEquals(original, restored)
    }

    @Test
    fun `时间以 ISO 字符串导出`() {
        val json = DataExporter.toJson(
            payload(
                checkIns = listOf(
                    CheckInRecord(LocalDate(2024, 5, 3), "", Instant.parse("2024-05-03T13:00:00Z")),
                ),
            ),
        )

        assertTrue(json.contains("\"2024-05-03T13:00:00Z\""), "时刻应为 ISO 8601")
        assertTrue(json.contains("\"exportedAt\": \"2024-05-04T09:31:00Z\""))
        assertTrue(json.contains("\"app\": \"磐石\""))
        assertTrue(json.contains("\"appVersion\": \"0.1.0\""))
    }

    @Test
    fun `文件名带导出日期且不含敏感词`() {
        val name = DataExporter.fileName(exportedAt)

        assertEquals("anchor-export-2024-05-04.json", name)
        assertTrue(!name.contains("戒"))
    }

    @Test
    fun `schema 版本写进文件头`() {
        val json = DataExporter.toJson(payload())

        assertTrue(json.contains("\"schemaVersion\": ${DataExporter.SCHEMA_VERSION}"))
    }
}
