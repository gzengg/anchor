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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anchor.recovery.R
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
        AnchorLargeTitle(stringResource(R.string.tools_title))

        AnchorListGroup {
            AnchorListItem(
                title = stringResource(R.string.tools_urge_surfing_title),
                subtitle = stringResource(R.string.tools_urge_surfing_subtitle),
                value = stringResource(R.string.tools_urge_surfing_action),
                showChevron = true,
                onClick = onOpenUrgeSurfing,
            )
            AnchorHairline()
            AnchorListItem(
                title = stringResource(R.string.tools_delay_tool_title),
                subtitle = stringResource(R.string.tools_delay_tool_subtitle),
                value = stringResource(R.string.tools_delay_tool_action),
                showChevron = true,
                onClick = onOpenDelayTool,
            )
            AnchorHairline()
            AnchorListItem(
                title = stringResource(R.string.tools_relapse_title),
                subtitle = stringResource(R.string.tools_relapse_subtitle),
                value = stringResource(R.string.tools_relapse_action),
                showChevron = true,
                onClick = onOpenRelapseEdit,
            )
            AnchorHairline()
            AnchorListItem(
                title = stringResource(R.string.tools_assessment_title),
                subtitle = stringResource(R.string.tools_assessment_subtitle),
                value = stringResource(R.string.tools_assessment_action),
                showChevron = true,
                onClick = onOpenAssessmentHub,
            )
        }

        AnchorSectionHeader(stringResource(R.string.tools_section_progress))
        AnchorListGroup {
            AnchorListItem(
                title = stringResource(R.string.tools_milestone_title),
                subtitle = stringResource(R.string.tools_milestone_subtitle),
                value = stringResource(R.string.tools_milestone_action),
                showChevron = true,
                onClick = onOpenMilestones,
            )
        }

        Text(
            text = stringResource(R.string.tools_footnote),
            style = AnchorType.footnote,
            color = AnchorTheme.colors.labelSecondary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
        )
    }
}
