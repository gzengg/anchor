package com.anchor.recovery.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * iOS 的轻触感反馈。
 *
 * 没用 Compose 的 `LocalHapticFeedback`：它只有长按与手柄两种，表达不了 iOS 那套
 * light / selection / success / error 的分档。直接走 `View.performHapticFeedback`：
 * 1. 系统「触感反馈」开关关掉时它自己会静默（返回 false），不用我们再判一次；
 * 2. CONFIRM / REJECT 是 API 30+ 才有的更强反馈，低版本回落到 CLOCK_TICK / LONG_PRESS。
 */
@Stable
class AnchorHaptics internal constructor(private val view: View) {

    /** 轻触：列表项按下、开关拨动。 */
    fun light() {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    /** 选项切换：分段控件换档。 */
    fun selection() {
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    /** 成功：打卡完成、记录已保存。 */
    fun confirm() {
        view.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.CLOCK_TICK
            },
        )
    }

    /** 失败/危险：破戒记录、破坏性确认。 */
    fun reject() {
        view.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.REJECT
            } else {
                HapticFeedbackConstants.LONG_PRESS
            },
        )
    }
}

@Composable
fun rememberAnchorHaptics(): AnchorHaptics {
    val view = LocalView.current
    return remember(view) { AnchorHaptics(view) }
}
