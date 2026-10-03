package com.anchor.recovery.core.notify

/**
 * 每日提醒的渠道 id 与文案合规规则。
 *
 * 通知文案本身是用户可见文案，已外移到 `:app`（`values/strings_notify.xml`，
 * 由 `ReminderTexts` 解析）；本模块只保留固定渠道 id 与「锁屏不得暴露敏感用途」
 * 这条可测规则——`:app` 的 `ReminderCopyComplianceTest` 用 [containsSensitiveWording]
 * 校验资源文案，规则本身仍是单一事实来源。
 */
object ReminderMessages {

    /** 渠道 id 固定，创建渠道与发通知必须用同一个常量。 */
    const val CHANNEL_ID = "daily_checkin"

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
