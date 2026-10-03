package com.anchor.recovery.notify

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.anchor.recovery.R
import com.anchor.recovery.core.notify.ReminderMessages
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 通知文案合规（提示词 §5.5：锁屏可见文案不得暴露敏感用途）。
 *
 * 规则来自 :core 的 [ReminderMessages.containsSensitiveWording]，文案本身在资源里。
 * 文案一旦改成资源，这条守卫必须跟着文案走，否则「不许出现敏感词」就没人管了。
 */
@RunWith(RobolectricTestRunner::class)
class ReminderCopyComplianceTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private val notificationVisibleTexts = listOf(
        R.string.app_name,
        R.string.notify_channel_name,
        R.string.notify_channel_description,
        R.string.notify_body,
        R.string.notify_body_with_days,
    )

    @Test
    fun `通知可见文案不含敏感词`() {
        notificationVisibleTexts.forEach { res ->
            val text = context.getString(res)
            assertFalse(
                ReminderMessages.containsSensitiveWording(text),
                "通知可见文案不得含敏感词：$text",
            )
        }
    }

    @Test
    fun `标题只用应用名 不描述用途`() {
        assertEquals(context.getString(R.string.app_name), ReminderTexts.title(context))
    }

    @Test
    fun `正文随连续天数变化 且零天时不给天数`() {
        val zero = ReminderTexts.body(context, 0)
        val three = ReminderTexts.body(context, 3)

        assertTrue(zero.isNotBlank())
        assertTrue(zero != three, "有天数时正文应当不同")
        assertTrue(three.contains("3"), "有天数时应给出天数：$three")
        assertFalse(ReminderMessages.containsSensitiveWording(three))
    }

    @Test
    fun `渠道名与描述取自资源`() {
        assertEquals(context.getString(R.string.notify_channel_name), ReminderTexts.channelName(context))
        assertEquals(
            context.getString(R.string.notify_channel_description),
            ReminderTexts.channelDescription(context),
        )
    }
}
