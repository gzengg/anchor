package com.anchor.recovery.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anchor.recovery.ui.CheckInUiState
import com.anchor.recovery.ui.CheckInViewModel
import com.anchor.recovery.ui.components.AnchorButton
import com.anchor.recovery.ui.components.AnchorButtonStyle
import com.anchor.recovery.ui.components.AnchorHairline
import com.anchor.recovery.ui.components.AnchorLargeTitle
import com.anchor.recovery.ui.components.AnchorListGroup
import com.anchor.recovery.ui.components.AnchorListItem
import com.anchor.recovery.ui.components.AnchorSectionHeader
import com.anchor.recovery.ui.components.PublishAnchorNavBar
import com.anchor.recovery.ui.theme.AnchorTheme
import com.anchor.recovery.ui.theme.AnchorType

/**
 * F1 今日打卡：当天状态、感受备注（一天一次）、最近打卡记录。
 *
 * 版式为 iOS 分组列表：大标题 + 分组卡片，备注输入框与动作放同组，最近记录用分组行。
 */
@Composable
fun CheckInScreen(
    viewModel: CheckInViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val colors = AnchorTheme.colors
    val scrollState = rememberScrollState()
    PublishAnchorNavBar(scrollState)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.groupedBackground)
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp),
    ) {
        AnchorLargeTitle("今日打卡")

        TodayStatusCard(state = state, modifier = Modifier.padding(top = 8.dp))

        AnchorListGroup(modifier = Modifier.padding(top = 16.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = state.note,
                    onValueChange = viewModel::updateNote,
                    label = { Text(text = "今天的感受 / 触发情境（可留空）") },
                    minLines = 3,
                    enabled = !state.todayCheckedIn,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.tint,
                        unfocusedBorderColor = colors.opaqueSeparator,
                        disabledBorderColor = colors.separator,
                        focusedTextColor = colors.label,
                        unfocusedTextColor = colors.label,
                        disabledTextColor = colors.labelTertiary,
                        cursorColor = colors.tint,
                        focusedLabelColor = colors.tint,
                        unfocusedLabelColor = colors.labelSecondary,
                        disabledLabelColor = colors.labelTertiary,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AnchorButton(
                        text = if (state.todayCheckedIn) "今天已打卡" else "保存打卡",
                        onClick = viewModel::save,
                        enabled = !state.todayCheckedIn,
                        modifier = Modifier.weight(1f),
                    )
                    AnchorButton(
                        text = "撤销今天",
                        onClick = viewModel::undoToday,
                        style = AnchorButtonStyle.Tinted,
                        enabled = state.todayCheckedIn,
                    )
                }
                state.message?.let { message ->
                    Text(
                        text = message,
                        style = AnchorType.footnote,
                        color = colors.tint,
                    )
                }
            }
        }

        AnchorSectionHeader("最近 ${state.recent.size} 次打卡")
        AnchorListGroup {
            if (state.recent.isEmpty()) {
                Text(
                    text = "还没有打卡记录。",
                    style = AnchorType.body,
                    color = colors.labelSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
            }
            state.recent.forEachIndexed { index, record ->
                if (index > 0) AnchorHairline()
                AnchorListItem(
                    title = record.date.toString(),
                    subtitle = record.note.takeIf { it.isNotBlank() },
                )
            }
        }
    }
}

@Composable
private fun TodayStatusCard(state: CheckInUiState, modifier: Modifier = Modifier) {
    val colors = AnchorTheme.colors
    AnchorListGroup(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = state.today.toString(), style = AnchorType.headline, color = colors.label)
            Text(
                text = if (state.todayCheckedIn) "状态：已打卡" else "状态：今天还没打卡",
                style = AnchorType.body,
                color = colors.label,
            )
            Text(
                text = "只能为今天打卡：漏一天就断签，补打会让连续天数失真。",
                style = AnchorType.footnote,
                color = colors.labelSecondary,
            )
        }
    }
}
