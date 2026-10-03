package com.anchor.recovery.core.notify

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 敏感词规则本身的行为（提示词 §5.5：锁屏上不得暴露敏感用途）。
 *
 * 通知文案已外移到 `:app` 资源，由 `ReminderCopyComplianceTest` 用这里的规则校验；
 * 本测试只保证规则有效，防止有人为了「通过校验」把词表删空。
 */
class ReminderMessagesTest {

    @Test
    fun `敏感词检测本身有效`() {
        assertTrue(ReminderMessages.containsSensitiveWording("戒色第 3 天"))
        assertTrue(ReminderMessages.containsSensitiveWording("复吸了"))
        assertTrue(ReminderMessages.containsSensitiveWording("破戒了"), "界面用「破戒」，同样不得出现在通知里")
        assertFalse(ReminderMessages.containsSensitiveWording("今天也要记录哦。"))
    }

    @Test
    fun `渠道 id 与规格一致`() {
        assertEquals("daily_checkin", ReminderMessages.CHANNEL_ID)
    }
}
