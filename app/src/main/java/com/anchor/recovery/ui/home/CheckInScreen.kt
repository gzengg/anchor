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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anchor.recovery.R
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
        AnchorLargeTitle(stringResource(R.string.checkin_title))

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
                    label = { Text(text = stringResource(R.string.checkin_note_label)) },
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
                        text = if (state.todayCheckedIn) {
                            stringResource(R.string.checkin_checked_in_button)
                        } else {
                            stringResource(R.string.checkin_save_button)
                        },
                        onClick = viewModel::save,
                        enabled = !state.todayCheckedIn,
                        modifier = Modifier.weight(1f),
                    )
                    AnchorButton(
                        text = stringResource(R.string.checkin_undo_button),
                        onClick = viewModel::undoToday,
                        style = AnchorButtonStyle.Tinted,
                        enabled = state.todayCheckedIn,
                    )
                }
                state.messageRes?.let { messageRes ->
                    Text(
                        text = stringResource(messageRes, *state.messageArgs.toTypedArray()),
                        style = AnchorType.footnote,
                        color = colors.tint,
                    )
                }
            }
        }

        AnchorSectionHeader(
            pluralStringResource(
                R.plurals.checkin_recent_count,
                state.recent.size,
                state.recent.size,
            ),
        )
        AnchorListGroup {
            if (state.recent.isEmpty()) {
                Text(
                    text = stringResource(R.string.checkin_empty),
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
                text = if (state.todayCheckedIn) {
                    stringResource(R.string.checkin_status_checked_in)
                } else {
                    stringResource(R.string.checkin_status_not_checked_in)
                },
                style = AnchorType.body,
                color = colors.label,
            )
            Text(
                text = stringResource(R.string.checkin_only_today_notice),
                style = AnchorType.footnote,
                color = colors.labelSecondary,
            )
        }
    }
}
