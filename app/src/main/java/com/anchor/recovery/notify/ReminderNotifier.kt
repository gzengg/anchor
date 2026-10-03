package com.anchor.recovery.notify

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.anchor.recovery.AnchorApplication
import com.anchor.recovery.MainActivity
import com.anchor.recovery.R
import com.anchor.recovery.core.notify.ReminderMessages
import kotlinx.coroutines.flow.first

/**
 * 发一条每日记录提醒。
 *
 * 不精确提醒（WorkManager）与精确提醒（AlarmManager）走同一条出口，保证两条路径的通知
 * 文案、点击行为、失败判定完全一致。前置条件不满足就静默返回：提醒不是关键功能，
 * 用户关掉开关、没给通知权限时，什么都不做才是对的。
 */
object ReminderNotifier {

    const val NOTIFICATION_ID = 1001

    suspend fun postDailyReminder(context: Context) {
        // 进程被系统重建时 applicationContext 一定是本应用的 Application；
        // 仍然用安全转换，避免将来替换 Application 时直接崩掉。
        val application = context.applicationContext as? AnchorApplication ?: return

        // 用户在设置里关掉提醒后，任务/闹钟可能还残留一次触发：这里再确认一次。
        val settings = application.settings.snapshot.first()
        if (!settings.reminderEnabled) return

        if (!canPostNotifications(context)) return

        val streak = application.repository.streak.first()
        post(context, ReminderTexts.title(context), ReminderTexts.body(context, streak.currentDays))
    }

    private fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun post(context: Context, title: String, body: String) {
        NotificationChannels.ensure(context)

        val notification = NotificationCompat.Builder(context, ReminderMessages.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_anchor)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openAppPendingIntent(context))
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun openAppPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
