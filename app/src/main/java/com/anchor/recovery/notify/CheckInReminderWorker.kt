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
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.anchor.recovery.AnchorApplication
import com.anchor.recovery.MainActivity
import com.anchor.recovery.R
import com.anchor.recovery.core.notify.ReminderMessages
import kotlinx.coroutines.flow.first

/**
 * 每日记录提醒。
 *
 * 只做三件事：读设置 → 用 :core 算当前连续天数 → 发一条中性通知。
 * 通知文案（含「不许出现哪些词」的规则）在 :core 里被测试覆盖，这里不做文案拼接。
 */
class CheckInReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        // 进程被系统重建时 applicationContext 一定是本应用的 Application；
        // 仍然用安全转换，避免将来替换 Application 时直接崩掉。
        val application = applicationContext as? AnchorApplication ?: return Result.failure()

        // 用户在设置里关掉提醒后，任务可能还残留一次触发：这里再确认一次。
        val settings = application.settings.snapshot.first()
        if (!settings.reminderEnabled) return Result.success()

        if (!canPostNotifications()) return Result.success()

        val streak = application.repository.streak.first()
        post(title = ReminderMessages.title(), body = ReminderMessages.body(streak.currentDays))
        return Result.success()
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        val granted = ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        return granted
    }

    private fun post(title: String, body: String) {
        NotificationChannels.ensure(applicationContext)

        val notification = NotificationCompat.Builder(applicationContext, ReminderMessages.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_anchor)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openAppPendingIntent())
            .build()

        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
    }

    private fun openAppPendingIntent(): PendingIntent {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    companion object {
        const val NOTIFICATION_ID = 1001
    }
}
