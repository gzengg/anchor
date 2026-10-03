package com.anchor.recovery.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * 不精确的每日提醒（WorkManager 周期任务，默认策略）。
 *
 * 只负责触发：读设置、算连续天数、发一条中性通知都在 [ReminderNotifier] 里，
 * 与精确提醒（[ExactReminderReceiver]）共用同一条出口，两条路径行为一致。
 */
class CheckInReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        ReminderNotifier.postDailyReminder(applicationContext)
        return Result.success()
    }
}
