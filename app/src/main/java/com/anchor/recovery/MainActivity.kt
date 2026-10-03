package com.anchor.recovery

import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.toArgb
import com.anchor.recovery.notify.NotificationChannels
import com.anchor.recovery.notify.ReminderScheduler
import com.anchor.recovery.ui.AnchorApp
import com.anchor.recovery.ui.theme.AnchorTheme
import com.anchor.recovery.ui.theme.anchorColorScheme
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // targetSdk 35（Android 15）强制 edge-to-edge：显式开启，避免不同 OEM 行为差异。
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // windowBackground 是资源层静态值，Compose 首帧之后页面底色由主题接管：
        // 这里把窗口底色设成同一个值，浅/深色切换（重建 Activity）时不会闪色。
        val nightMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        window.setBackgroundDrawable(
            ColorDrawable(anchorColorScheme(nightMode).background.toArgb()),
        )

        val anchorApplication = application as AnchorApplication
        NotificationChannels.ensure(this)

        setContent {
            AnchorTheme {
                // 提醒排程的唯一同步点（设置一变就重排），放在 UI 入口而不是 Application：
                // 单元测试不启动 Activity，因此不会碰到未初始化的 WorkManager。
                LaunchedEffect(Unit) {
                    anchorApplication.settings.snapshot.collectLatest { snapshot ->
                        ReminderScheduler.sync(applicationContext, snapshot)
                    }
                }

                AnchorApp(
                    repository = anchorApplication.repository,
                    contentRepository = anchorApplication.contentRepository,
                    settings = anchorApplication.settings,
                )
            }
        }
    }
}
