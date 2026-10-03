package com.anchor.recovery.core.notify

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 通知文案合规（提示词 §5.5：不得暴露敏感用途，用中性文案）。
 */
class ReminderMessagesTest {

    @Test
    fun `通知标题与正文不含敏感词`() {
        val texts = buildList {
            add(ReminderMessages.title())
            add(ReminderMessages.body(0))
            add(ReminderMessages.body(1))
            add(ReminderMessages.body(37))
            add(ReminderMessages.body(365))
        }

        texts.forEach { text ->
            assertFalse(
                ReminderMessages.containsSensitiveWording(text),
                "通知文案不得含敏感词：$text",
            )
        }
    }

    @Test
    fun `敏感词检测本身有效`() {
        assertTrue(ReminderMessages.containsSensitiveWording("戒色第 3 天"))
        assertTrue(ReminderMessages.containsSensitiveWording("复吸了"))
        assertFalse(ReminderMessages.containsSensitiveWording("今天也要记录哦。"))
    }

    @Test
    fun `正文随连续天数变化 且零天时不给天数`() {
        val zero = ReminderMessages.body(0)
        val three = ReminderMessages.body(3)

        assertEquals("今天也要记录哦。", zero)
        assertTrue(zero != three)
        assertTrue(three.contains("3"), "有天数时应给出天数")
    }

    @Test
    fun `渠道 id 与规格一致`() {
        assertEquals("daily_checkin", ReminderMessages.CHANNEL_ID)
    }
}
