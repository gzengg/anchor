package com.anchor.recovery.core

/**
 * 模块级常量。App 显示名等信息在 :core 与 :app 之间共享，避免双份字面量漂移。
 */
object AnchorCore {
    /** 应用内部版本号（与 app/build.gradle.kts 的 versionName 对齐）。 */
    const val VERSION: String = "0.1.0"

    /** 应用显示名。 */
    const val APP_DISPLAY_NAME: String = "磐石"
}
