package com.anchor.recovery.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.anchor.recovery.AnchorApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 精确提醒的接收器，同时负责开机后重排。
 *
 * 闹钟在重启后会全部丢失（WorkManager 的周期任务则由系统自己恢复），所以精确提醒路径
 * 必须自己接 `BOOT_COMPLETED` 重排一次，否则用户重启手机后就再也收不到提醒。
 *
 * 两条动作都做 IO（读设置、读数据库、发通知），因此用 `goAsync()` 撑住进程直到协程跑完。
 */
class ExactReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                when (intent.action) {
                    Intent.ACTION_BOOT_COMPLETED -> syncFromSettings(context)
                    ExactReminderScheduler.ACTION_FIRE -> {
                        // 先发今天的通知，再排下一天：精确闹钟是一次性的。
                        ReminderNotifier.postDailyReminder(context)
                        syncFromSettings(context)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * 开机后 / 精确闹钟触发后都按当前设置重排：这里不猜时间也不猜策略，一律从 DataStore 读，
     * 与设置页保持一致；排不上精确闹钟（权限被收回）时 [ReminderScheduler.sync] 会回落到不精确。
     */
    private suspend fun syncFromSettings(context: Context) {
        val application = context.applicationContext as? AnchorApplication ?: return
        ReminderScheduler.sync(context, application.settings.snapshot.first())
    }
}
