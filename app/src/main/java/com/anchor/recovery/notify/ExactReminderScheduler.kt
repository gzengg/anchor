package com.anchor.recovery.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.anchor.recovery.core.clock.Clock
import com.anchor.recovery.core.clock.SystemClock
import com.anchor.recovery.core.notify.ReminderTime
import com.anchor.recovery.core.notify.ReminderTimeCalculator

/**
 * 精确提醒排程：只在用户显式开启「精确提醒」且系统授权时使用（见 [ReminderScheduler.sync]）。
 *
 * 用 `setExactAndAllowWhileIdle`，**只排下一条**：触发后由 [ExactReminderReceiver] 再排第二天，
 * 这样每次都能按当时的时间设置重算，且不需要常驻周期任务。
 *
 * 权限：Android 12(API 31)+ 需要 `SCHEDULE_EXACT_ALARM`（用户可在系统设置里随时收回）。
 * 刻意**不申请** `USE_EXACT_ALARM`：那是闹钟/日历类应用专用，上架受限，记录提醒不属于该场景。
 */
object ExactReminderScheduler {

    const val REQUEST_CODE = 2001
    const val ACTION_FIRE = "com.anchor.recovery.action.EXACT_REMINDER"

    /** 系统当前是否允许精确闹钟；低版本（< API 31）没有这个开关，一律允许。 */
    fun canScheduleExact(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true

    /**
     * 排下一个触发点。返回 false 表示这次没排上（权限被收回等）：
     * 调用方据此回落到不精确提醒，而不是让用户以为提醒还在。
     */
    fun schedule(context: Context, time: ReminderTime, clock: Clock = SystemClock()): Boolean {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return false
        val triggerAt = clock.now().toEpochMilliseconds() +
            ReminderTimeCalculator.initialDelayForPeriodic(clock, time).inWholeMilliseconds
        return runCatching {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                newPendingIntent(context),
            )
            true
        }.getOrDefault(false)
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = existingPendingIntent(context) ?: return
        alarmManager.cancel(pending)
    }

    private fun newPendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        alarmIntent(context),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** FLAG_NO_CREATE：没排过就返回 null，避免「为了不空指针而先造一个再取消」。 */
    private fun existingPendingIntent(context: Context): PendingIntent? = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        alarmIntent(context),
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun alarmIntent(context: Context): Intent =
        Intent(context, ExactReminderReceiver::class.java).setAction(ACTION_FIRE)
}
