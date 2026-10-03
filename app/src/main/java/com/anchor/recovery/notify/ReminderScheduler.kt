package com.anchor.recovery.notify

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.anchor.recovery.core.clock.Clock
import com.anchor.recovery.core.clock.SystemClock
import com.anchor.recovery.core.notify.ReminderScheduling
import com.anchor.recovery.core.notify.ReminderStrategy
import com.anchor.recovery.core.notify.ReminderTime
import com.anchor.recovery.core.notify.ReminderTimeCalculator
import com.anchor.recovery.data.settings.AnchorSettingsSnapshot
import java.util.concurrent.TimeUnit

/**
 * 每日提醒的排程：把 DataStore 里的设置翻译成「一条唯一排程」。
 *
 * 默认走 `PeriodicWorkRequest`（inexact）：记录提醒不需要精确到分钟，交给系统在维护窗口里
 * 合并执行更省电，也不需要任何特殊权限。用户在设置页显式打开「精确提醒」且系统授权后，
 * 才改走 [ExactReminderScheduler]（AlarmManager 精确闹钟）。
 *
 * 同一时刻只允许一种排程存在：切换策略时先取消另一种，避免同一天收到两条通知。
 */
object ReminderScheduler {

    const val WORK_NAME = "daily_checkin_reminder"

    /** 设置变化 / App 启动 / 开机后调用，保证排程与设置一致。 */
    fun sync(context: Context, settings: AnchorSettingsSnapshot, clock: Clock = SystemClock()) {
        if (!settings.reminderEnabled) {
            cancel(context)
            return
        }

        val time = ReminderTime.parseOrNull(settings.reminderTime) ?: ReminderTime.DEFAULT
        val strategy = ReminderScheduling.strategyFor(
            exactRequested = settings.exactReminderEnabled,
            exactAllowed = ExactReminderScheduler.canScheduleExact(context),
        )

        when (strategy) {
            ReminderStrategy.EXACT -> {
                // 排不上（权限刚好被收回）就当场落回不精确，不留一个「开关开着但不会响」的状态。
                if (ExactReminderScheduler.schedule(context, time, clock)) {
                    cancelInexact(context)
                } else {
                    scheduleInexact(context, time, clock)
                }
            }

            ReminderStrategy.INEXACT -> {
                ExactReminderScheduler.cancel(context)
                scheduleInexact(context, time, clock)
            }
        }
    }

    fun cancel(context: Context) {
        ExactReminderScheduler.cancel(context)
        cancelInexact(context)
    }

    private fun scheduleInexact(context: Context, time: ReminderTime, clock: Clock) {
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

    private fun cancelInexact(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
