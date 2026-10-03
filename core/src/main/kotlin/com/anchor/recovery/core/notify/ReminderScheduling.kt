package com.anchor.recovery.core.notify

/** 提醒的排程策略：精确到点（AlarmManager）或交给系统合并（WorkManager，不精确）。 */
enum class ReminderStrategy {
    INEXACT,
    EXACT,
}

/**
 * 排程策略选择：只看「用户是否要求精确」与「系统是否允许」两个布尔量，不碰 Android API，
 * 便于单测穷举（见 `ReminderSchedulingTest`）。
 *
 * 默认策略是不精确：记录提醒不需要精确到分钟，交给系统在维护窗口合并执行更省电；
 * 只有用户显式打开精确提醒、且系统确实授权时才用精确闹钟。
 */
object ReminderScheduling {

    fun strategyFor(exactRequested: Boolean, exactAllowed: Boolean): ReminderStrategy =
        if (exactRequested && exactAllowed) ReminderStrategy.EXACT else ReminderStrategy.INEXACT

    /**
     * 是否需要把开关回落为关闭并说明原因：用户开着精确提醒，但系统没给（或已收回）这个能力。
     * 界面据此关掉开关，避免留一个做不到的承诺。
     */
    fun shouldFallBack(exactRequested: Boolean, exactAllowed: Boolean): Boolean =
        exactRequested && !exactAllowed
}
