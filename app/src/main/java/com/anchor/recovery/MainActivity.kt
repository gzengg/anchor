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

        // 动态取色下页面底色随壁纸变化，而 windowBackground 是资源层静态值：
        // 进程起来后立刻把窗口底色换成当前方案色，启动画面退出后就不会露出静态色。
        // uiMode 变化会重建 Activity，所以这个取值也覆盖了深/浅色切换。
        val nightMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        window.setBackgroundDrawable(
            ColorDrawable(anchorColorScheme(this, nightMode).background.toArgb()),
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
