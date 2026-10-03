package com.anchor.recovery.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.anchor.recovery.core.notify.ReminderMessages

/**
 * 通知渠道（Android 8.0+ 必需）。id 取自 :core 的 [ReminderMessages]（规则单一来源），
 * 渠道名与描述取自资源（用户可见文案集中管理）。
 */
object NotificationChannels {

    fun ensure(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(ReminderMessages.CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            ReminderMessages.CHANNEL_ID,
            ReminderTexts.channelName(context),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = ReminderTexts.channelDescription(context)
        }
        manager.createNotificationChannel(channel)
    }
}
