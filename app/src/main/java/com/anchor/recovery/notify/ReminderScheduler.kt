package com.anchor.recovery.notify

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.anchor.recovery.core.clock.Clock
import com.anchor.recovery.core.clock.SystemClock
import com.anchor.recovery.core.notify.ReminderTime
import com.anchor.recovery.core.notify.ReminderTimeCalculator
import com.anchor.recovery.data.settings.AnchorSettingsSnapshot
import java.util.concurrent.TimeUnit

/**
 * 每日提醒的排程：把 DataStore 里的设置翻译成一条唯一的 24 小时周期任务。
 *
 * 采用 `PeriodicWorkRequest`（inexact），**不申请** `SCHEDULE_EXACT_ALARM`：
 * 记录提醒不需要精确到分钟，交给系统在维护窗口里合并执行更省电。
 */
object ReminderScheduler {

    const val WORK_NAME = "daily_checkin_reminder"

    /** 设置变化 / App 启动时调用，保证排程与设置一致。 */
    fun sync(context: Context, settings: AnchorSettingsSnapshot, clock: Clock = SystemClock()) {
        if (settings.reminderEnabled) {
            val time = ReminderTime.parseOrNull(settings.reminderTime) ?: ReminderTime.DEFAULT
            schedule(context, time, clock)
        } else {
            cancel(context)
        }
    }

    fun schedule(context: Context, time: ReminderTime, clock: Clock = SystemClock()) {
        val delay = ReminderTimeCalculator.initialDelayForPeriodic(clock, time)
        val request = PeriodicWorkRequestBuilder<CheckInReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay.inWholeMilliseconds, TimeUnit.MILLISECONDS)
            .addTag(WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
