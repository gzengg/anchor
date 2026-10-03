package com.anchor.recovery.notify

import android.content.Context
import com.anchor.recovery.R

/**
 * 通知文案的资源出口。
 *
 * 文案在 `values/strings_notify.xml`；渠道 id 与「不许出现哪些词」的规则仍留在 :core 的
 * `ReminderMessages`，因为那是 :core 的功能约束，且必须被测试固化。
 */
object ReminderTexts {

    fun channelName(context: Context): String = context.getString(R.string.notify_channel_name)

    fun channelDescription(context: Context): String =
        context.getString(R.string.notify_channel_description)

    /** 通知标题：只用 App 名称，不描述用途。 */
    fun title(context: Context): String = context.getString(R.string.app_name)

    /** 通知正文：按当前连续天数给一句中性提醒。 */
    fun body(context: Context, currentDays: Int): String =
        if (currentDays <= 0) {
            context.getString(R.string.notify_body)
        } else {
            context.getString(R.string.notify_body_with_days, currentDays)
        }
}
