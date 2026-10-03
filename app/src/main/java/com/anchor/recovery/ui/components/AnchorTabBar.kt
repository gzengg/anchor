package com.anchor.recovery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.anchor.recovery.ui.theme.AnchorTheme
import com.anchor.recovery.ui.theme.AnchorType

/**
 * 底部 Tab 栏的一个 Tab。
 *
 * @param icon 未选中时的图标（用线形）
 * @param selectedIcon 选中时的图标（用实心，iOS 的选中态是「填满 + 主色」）
 */
data class AnchorTab(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
)

/**
 * iOS 底部 Tab 栏：高 49dp、顶部 0.5dp 细线、选中项主色且图标填满。
 *
 * 选中态同时用「颜色 + 图标填充」两路表达，不靠放大或加粗——
 * 大字体设置下标签会被截断，形状差异比字号差异更稳。
 */
@Composable
fun AnchorTabBar(
    tabs: List<AnchorTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentWindowInsets: WindowInsets = WindowInsets.navigationBars,
) {
    val colors = AnchorTheme.colors
    val haptics = rememberAnchorHaptics()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.cardBackground)
            .windowInsetsPadding(contentWindowInsets),
    ) {
        AnchorHairline(inset = 0.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(49.dp)
                .selectableGroup(),
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = index == selectedIndex
                val tint = if (isSelected) colors.tint else colors.labelSecondary
                AnchorClickableSurface(
                    onClick = {
                        if (!isSelected) {
                            haptics.light()
                            onSelect(index)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .semantics { selected = isSelected },
                    role = Role.Tab,
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = if (isSelected) tab.selectedIcon else tab.icon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tab.label,
                            style = AnchorType.caption2,
                            color = tint,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 2.dp),
                        )
                    }
                }
            }
        }
    }
}
