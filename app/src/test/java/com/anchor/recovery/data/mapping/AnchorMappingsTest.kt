package com.anchor.recovery.data.mapping

import com.anchor.recovery.core.model.AssessmentRecord
import com.anchor.recovery.core.model.AssessmentType
import com.anchor.recovery.core.model.CheckInRecord
import com.anchor.recovery.core.model.RelapseRecord
import com.anchor.recovery.core.model.UrgeEpisodeRecord
import com.anchor.recovery.core.model.UrgeTool
import com.anchor.recovery.data.db.entity.AssessmentResultEntity
import com.anchor.recovery.data.db.entity.RelapseEntity
import com.anchor.recovery.data.db.entity.UrgeEpisodeEntity
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnchorMappingsTest {

    private val instant = Instant.fromEpochMilliseconds(1_718_400_000_000)

    @Test
    fun `打卡记录往返映射保持一致且日期存为 ISO 字符串`() {
        val record = CheckInRecord(
            date = LocalDate.parse("2024-06-15"),
            note = "今天想去跑步",
            createdAt = instant,
        )

        val entity = record.toEntity()
        assertEquals("2024-06-15", entity.date)
        assertEquals(1_718_400_000_000, entity.createdAt)
        assertEquals(record, entity.toDomain())
    }

    @Test
    fun `复吸记录往返映射保持情绪与触发源列表`() {
        val record = RelapseRecord(
            id = 7,
            occurredAt = instant,
            situation = "深夜独自在家",
            emotions = listOf("焦虑", "无聊"),
            triggers = listOf("失眠", "手机"),
            note = "刷到凌晨",
        )

        val entity = record.toEntity()
        assertEquals("焦虑,无聊", entity.emotions)
        assertEquals("失眠,手机", entity.triggers)
        assertEquals(record, entity.toDomain())
    }

    @Test
    fun `标签编解码会去掉空白项并裁剪空格`() {
        assertEquals("焦虑,无聊", encodeLabels(listOf(" 焦虑 ", "", "无聊")))
        assertEquals(emptyList(), decodeLabels(""))
        assertEquals(listOf("焦虑", "无聊"), decodeLabels(" 焦虑 , ,无聊 ,"))
    }

    @Test
    fun `渴求记录往返映射保持一致`() {
        val record = UrgeEpisodeRecord(
            id = 3,
            startedAt = instant,
            durationSec = 600,
            peakIntensity = 9,
            endIntensity = 4,
            tool = UrgeTool.DELAY_TOOL,
        )

        val entity = record.toEntity()
        assertEquals("DELAY_TOOL", entity.tool)
        assertEquals(record, entity.toDomain())
    }

    @Test
    fun `未知的工具枚举名兜底为渴求冲浪而不是崩溃`() {
        val entity = UrgeEpisodeEntity(
            id = 1,
            startedAt = 0,
            durationSec = 60,
            peakIntensity = 5,
            endIntensity = 2,
            tool = "SOMETHING_NEW",
        )

        assertEquals(UrgeTool.URGE_SURFING, entity.toDomain().tool)
        assertEquals(UrgeTool.URGE_SURFING, UrgeTool.of(null))
    }

    @Test
    fun `问卷结果往返映射保持作答数组与类型`() {
        val record = AssessmentRecord(
            id = 11,
            type = AssessmentType.MORAL,
            takenAt = instant,
            totalScore = 14,
            level = "象限：高行为 · 高道德冲突",
            answers = listOf(1, 2, 3, 4),
        )

        val entity = record.toEntity()
        assertEquals("MORAL", entity.type)
        assertEquals("[1,2,3,4]", entity.answersJson)
        assertEquals(record, entity.toDomain())
    }

    @Test
    fun `作答 JSON 损坏时退回空数组不抛异常`() {
        val entity = AssessmentResultEntity(
            id = 1,
            type = "CSBD",
            takenAt = 0,
            totalScore = 0,
            level = "轻度",
            answersJson = "{不是数组",
        )

        assertTrue(entity.toDomain().answers.isEmpty())
        assertEquals(AssessmentType.CSBD, AssessmentType.of("不存在的类型"))
    }
}
