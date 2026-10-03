package com.anchor.recovery.ui.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anchor.recovery.ui.components.AnchorHairline
import com.anchor.recovery.ui.components.AnchorLargeTitle
import com.anchor.recovery.ui.components.AnchorListGroup
import com.anchor.recovery.ui.components.AnchorListItem
import com.anchor.recovery.ui.components.AnchorSectionHeader
import com.anchor.recovery.ui.components.PublishAnchorNavBar
import com.anchor.recovery.ui.theme.AnchorTheme
import com.anchor.recovery.ui.theme.AnchorType

/**
 * 工具页：F3 渴求冲浪 / F4 十分钟延时 / F5 破戒记录 / F7+F8 自评问卷入口。
 * 具体流程页在各自阶段实现（S5 / S6）。
 */
@Composable
fun ToolsScreen(
    onOpenUrgeSurfing: () -> Unit,
    onOpenDelayTool: () -> Unit,
    onOpenRelapseEdit: () -> Unit,
    onOpenAssessmentHub: () -> Unit,
    onOpenMilestones: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    PublishAnchorNavBar(scrollState)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AnchorTheme.colors.groupedBackground)
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp),
    ) {
        AnchorLargeTitle("工具")

        AnchorListGroup {
            AnchorListItem(
                title = "渴求冲浪",
                subtitle = "把一次冲动拆成几步：说明 → 呼吸 → 观察 → 等待 → 再评分。",
                value = "开始",
                showChevron = true,
                onClick = onOpenUrgeSurfing,
            )
            AnchorHairline()
            AnchorListItem(
                title = "十分钟延时",
                subtitle = "先不做决定，把选择往后放十分钟，计时结束再评估。",
                value = "开始计时",
                showChevron = true,
                onClick = onOpenDelayTool,
            )
            AnchorHairline()
            AnchorListItem(
                title = "记录一次破戒",
                subtitle = "记下当时的情境、情绪和触发源，只看规律，不做评价。",
                value = "去记录",
                showChevron = true,
                onClick = onOpenRelapseEdit,
            )
            AnchorHairline()
            AnchorListItem(
                title = "自评问卷",
                subtitle = "成瘾倾向自评 19 题、道德冲突 vs 真实问题 12 题，都是参考，不是诊断。",
                value = "去作答",
                showChevron = true,
                onClick = onOpenAssessmentHub,
            )
        }

        AnchorSectionHeader("进度")
        AnchorListGroup {
            AnchorListItem(
                title = "里程碑徽章",
                subtitle = "7 / 14 / 30 / 60 / 90 天各一枚；破戒后重新开始，之前的徽章会保留。",
                value = "查看",
                showChevron = true,
                onClick = onOpenMilestones,
            )
        }

        Text(
            text = "工具仅用于自助记录与应对，不构成医疗建议。",
            style = AnchorType.footnote,
            color = AnchorTheme.colors.labelSecondary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
        )
    }
}
