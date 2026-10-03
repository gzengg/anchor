package com.anchor.recovery.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.anchor.recovery.ui.theme.AnchorTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 段控件的高度回归（v0.2.1 修复）。
 *
 * v0.2.0 里选中块用 `fillMaxHeight()` 铺底，控件被放进 Column 时这条约束解析成「剩余高度」，
 * 结果段控件撑满整页、下方列表被挤成 0 高——真机截图里知识库/日志页那一整块空白就是这么来的。
 * 这里钉死两件事：控件自身高度 = 段高 + 内边距；下方兄弟节点仍拿得到剩余空间。
 */
@RunWith(RobolectricTestRunner::class)
class AnchorSegmentedControlTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `段控件高度固定且不吃掉下方列表的空间`() {
        compose.setContent {
            AnchorTheme {
                Column(modifier = Modifier.fillMaxSize()) {
                    AnchorSegmentedControl(
                        options = listOf("全部", "打卡"),
                        selected = "全部",
                        onSelect = {},
                        modifier = Modifier.fillMaxWidth().testTag("segmented"),
                    )
                    Box(modifier = Modifier.fillMaxSize().testTag("list"))
                }
            }
        }

        // 轨道段高 32dp + 轨道上下各 2dp 内边距。
        compose.onNodeWithTag("segmented").assertHeightIsEqualTo(36.dp)
        // 关键回归点：修好前这里是 0dp（被段控件吃光）。
        compose.onNodeWithTag("list").assertHeightIsAtLeast(1.dp)
    }
}
