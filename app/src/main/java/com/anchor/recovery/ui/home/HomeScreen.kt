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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anchor.recovery.R
import com.anchor.recovery.ui.HomeUiState
import com.anchor.recovery.ui.HomeViewModel
import com.anchor.recovery.ui.components.AnchorButton
import com.anchor.recovery.ui.components.AnchorButtonStyle
import com.anchor.recovery.ui.components.AnchorListGroup
import com.anchor.recovery.ui.components.AnchorSectionHeader
import com.anchor.recovery.ui.components.PublishAnchorNavBar
import com.anchor.recovery.ui.text.milestoneTitleRes
import com.anchor.recovery.ui.theme.AnchorTheme
import com.anchor.recovery.ui.theme.AnchorType

/**
 * F1 首页：连续记录、今日打卡、戒断阶段、速援工具、知识库入口。
 *
 * 版式为 iOS 分组列表：分节小标题 + 圆角分组卡片。天数大数字留在首张卡里当视觉锚点，
 * 不再叠加大标题，内联标题「磐石」常显。
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenCheckIn: () -> Unit,
    onOpenUrgeSurfing: () -> Unit,
    onOpenDelayTool: () -> Unit,
    onOpenArticle: (String) -> Unit,
    onOpenLibrary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()
    // 首屏已有天数大数字，顶栏内联标题常显即可，不做大标题收缩联动。
    PublishAnchorNavBar(scrollState, hasLargeTitle = false)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AnchorTheme.colors.groupedBackground)
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp),
    ) {
        StreakCard(state = state, modifier = Modifier.padding(top = 8.dp))

        AnchorSectionHeader(stringResource(R.string.home_section_today))
        TodayCard(
            state = state,
            onCheckIn = viewModel::checkInToday,
            onOpenCheckIn = onOpenCheckIn,
        )

        AnchorSectionHeader(stringResource(R.string.home_section_struggling))
        QuickToolCard(
            onOpenUrgeSurfing = onOpenUrgeSurfing,
            onOpenDelayTool = onOpenDelayTool,
        )

        // 阶段卡自带阶段名（动态文案），不另加节标题，用上间距与前面分组拉开。
        PhaseCard(
            dayNumber = state.streak.currentDays,
            onOpenArticle = onOpenArticle,
            modifier = Modifier.padding(top = 20.dp),
        )

        AnchorSectionHeader(stringResource(R.string.home_section_library))
        LibraryCard(onOpenLibrary = onOpenLibrary)
    }
}

@Composable
private fun StreakCard(state: HomeUiState, modifier: Modifier = Modifier) {
    val colors = AnchorTheme.colors
    AnchorListGroup(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.home_streak_current_label),
                style = AnchorType.footnote,
                color = colors.labelSecondary,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = state.streak.currentDays.toString(),
                    style = AnchorType.largeTitle,
                    color = colors.tint,
                )
                Text(
                    text = stringResource(R.string.home_streak_day_unit),
                    style = AnchorType.subheadline,
                    color = colors.labelSecondary,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            Text(
                text = pluralStringResource(
                    R.plurals.home_streak_history,
                    state.streak.longestDays,
                    state.streak.longestDays,
                    state.streak.totalCheckInDays,
                ),
                style = AnchorType.footnote,
                color = colors.labelSecondary,
            )
            state.streak.daysSinceLastRelapse?.let { days ->
                Text(
                    text = if (days == 0) {
                        stringResource(R.string.home_relapse_today)
                    } else {
                        pluralStringResource(R.plurals.home_relapse_days_ago, days, days)
                    },
                    style = AnchorType.footnote,
                    color = colors.labelSecondary,
                )
            }
            LinearProgressIndicator(
                progress = { state.progress },
                color = colors.tint,
                trackColor = colors.fill,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
            )
            Text(
                text = stringResource(R.string.home_progress_percent, (state.progress * 100).toInt()) +
                    (
                        state.nextMilestone?.let {
                            stringResource(
                                R.string.home_progress_next_milestone,
                                stringResource(milestoneTitleRes(it.days)),
                            )
                        } ?: stringResource(R.string.home_progress_all_done)
                    ),
                style = AnchorType.footnote,
                color = colors.labelSecondary,
            )
        }
    }
}

@Composable
private fun TodayCard(
    state: HomeUiState,
    onCheckIn: () -> Unit,
    onOpenCheckIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AnchorTheme.colors
    AnchorListGroup(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (state.streak.todayCheckedIn) {
                    stringResource(R.string.home_today_checked_in_message)
                } else {
                    stringResource(R.string.home_today_not_checked_in_message)
                },
                style = AnchorType.body,
                color = colors.label,
            )
            AnchorButton(
                text = if (state.streak.todayCheckedIn) {
                    stringResource(R.string.home_today_checked_in_button)
                } else {
                    stringResource(R.string.home_today_check_in_button)
                },
                onClick = onCheckIn,
                enabled = !state.streak.todayCheckedIn,
                modifier = Modifier.fillMaxWidth(),
            )
            AnchorButton(
                text = stringResource(R.string.home_open_check_in_button),
                onClick = onOpenCheckIn,
                style = AnchorButtonStyle.Plain,
                modifier = Modifier.fillMaxWidth(),
            )
            state.messageRes?.let { res ->
                Text(
                    text = stringResource(res, *state.messageArgs.toTypedArray()),
                    style = AnchorType.footnote,
                    color = colors.tint,
                )
            }
        }
    }
}

@Composable
private fun QuickToolCard(
    onOpenUrgeSurfing: () -> Unit,
    onOpenDelayTool: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AnchorTheme.colors
    AnchorListGroup(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.home_quick_tool_hint),
                style = AnchorType.body,
                color = colors.label,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnchorButton(
                    text = stringResource(R.string.home_urge_surfing_button),
                    onClick = onOpenUrgeSurfing,
                    modifier = Modifier.weight(1f),
                )
                AnchorButton(
                    text = stringResource(R.string.home_delay_tool_button),
                    onClick = onOpenDelayTool,
                    style = AnchorButtonStyle.Tinted,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = stringResource(R.string.home_quick_tool_footnote),
                style = AnchorType.footnote,
                color = colors.labelSecondary,
            )
        }
    }
}

@Composable
private fun LibraryCard(
    onOpenLibrary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AnchorTheme.colors
    AnchorListGroup(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.home_library_hint),
                style = AnchorType.body,
                color = colors.label,
            )
            AnchorButton(
                text = stringResource(R.string.home_library_browse_button),
                onClick = onOpenLibrary,
                style = AnchorButtonStyle.Tinted,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
