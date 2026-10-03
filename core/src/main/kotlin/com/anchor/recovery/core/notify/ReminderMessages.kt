package com.anchor.recovery.core.notify

/**
 * 每日提醒的通知渠道与文案。
 *
 * 合规要求：通知文案不得暴露敏感用途（锁屏上可能被他人看到），因此一律用中性表达，
 * 且由 [containsSensitiveWording] 把「不许出现哪些词」变成可测规则。
 */
object ReminderMessages {

    /** 渠道 id 固定，创建渠道与发通知必须用同一个常量。 */
    const val CHANNEL_ID = "daily_checkin"
    const val CHANNEL_NAME = "每日记录提醒"
    const val CHANNEL_DESCRIPTION = "每天提醒你记录一次。可在设置里关闭。"

    /** 通知标题：只用 App 名称，不描述用途。 */
    fun title(): String = "磐石"

    /** 通知正文：按当前连续天数给一句中性提醒。 */
    fun body(currentDays: Int): String =
        if (currentDays <= 0) {
            "今天也要记录哦。"
        } else {
            "今天也要记录哦，已经连续 $currentDays 天。"
        }

    /** 锁屏可见的敏感词表（用途类词汇一律不得出现在通知里）。 */
    private val sensitiveWords = listOf(
        "戒色",
        "戒瘾",
        "戒断",
        "成瘾",
        "复吸",
        "破戒",
        "色情",
        "自慰",
        "看片",
        "治疗",
        "治愈",
    )

    fun containsSensitiveWording(text: String): Boolean = sensitiveWords.any { it in text }
}
