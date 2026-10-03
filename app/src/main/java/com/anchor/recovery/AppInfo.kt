package com.anchor.recovery

import com.anchor.recovery.core.AnchorCore

/**
 * :app 侧的应用级常量。显示名等共享值一律取自 :core，避免双份字面量。
 */
object AppInfo {
    val DISPLAY_NAME: String = AnchorCore.APP_DISPLAY_NAME
    val VERSION: String = AnchorCore.VERSION
}
