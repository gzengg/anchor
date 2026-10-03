package com.anchor.recovery.ui.theme

import android.content.Context
import androidx.compose.ui.graphics.toArgb
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import com.anchor.recovery.R

/**
 * 冷启动不掉色的硬约束：windowBackground 是资源层静态值（进程启动前就解析完），
 * Compose 首帧之后由主题接管，两边必须恒等，否则冷启动或深浅色切换会闪一下不同颜色。
 *
 * v1 靠注释维护这条约定，v2 把动态取色去掉后它才真正可测：主题色不再随壁纸变，
 * 于是「资源里的 anchor_background」必须永远等于「AnchorTheme 的背景色」。
 */
@RunWith(RobolectricTestRunner::class)
class AnchorThemeColorsTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test
    fun `浅色窗口底色与主题背景一致`() {
        assertEquals(
            anchorColorScheme(darkTheme = false).background.toArgb(),
            context.getColor(R.color.anchor_background),
        )
    }

    @Test
    @Config(qualifiers = "night")
    fun `夜间窗口底色与主题背景一致`() {
        assertEquals(
            anchorColorScheme(darkTheme = true).background.toArgb(),
            context.getColor(R.color.anchor_background),
        )
    }
}
