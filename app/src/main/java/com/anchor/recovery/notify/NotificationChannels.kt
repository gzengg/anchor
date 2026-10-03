package com.anchor.recovery.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.anchor.recovery.core.notify.ReminderMessages

/**
 * 通知渠道（Android 8.0+ 必需）。id / 名称 / 描述都取自 :core 的 [ReminderMessages]，
 * 保证「通知文案不许暴露用途」的规则只有一份来源，并被 :core 测试覆盖。
 */
object NotificationChannels {

    fun ensure(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(ReminderMessages.CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            ReminderMessages.CHANNEL_ID,
            ReminderMessages.CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = ReminderMessages.CHANNEL_DESCRIPTION
        }
        manager.createNotificationChannel(channel)
    }
}
