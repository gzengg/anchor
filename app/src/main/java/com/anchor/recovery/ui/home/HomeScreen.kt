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
import androidx.compose.ui.unit.dp
import com.anchor.recovery.ui.HomeUiState
import com.anchor.recovery.ui.HomeViewModel
import com.anchor.recovery.ui.components.AnchorButton
import com.anchor.recovery.ui.components.AnchorButtonStyle
import com.anchor.recovery.ui.components.AnchorListGroup
import com.anchor.recovery.ui.components.AnchorSectionHeader
import com.anchor.recovery.ui.components.PublishAnchorNavBar
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

        AnchorSectionHeader("今天")
        TodayCard(
            state = state,
            onCheckIn = viewModel::checkInToday,
            onOpenCheckIn = onOpenCheckIn,
        )

        AnchorSectionHeader("现在有点难熬？")
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

        AnchorSectionHeader("知识库")
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
                text = "当前连续记录",
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
                    text = " 天",
                    style = AnchorType.subheadline,
                    color = colors.labelSecondary,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            Text(
                text = "历史最长 ${state.streak.longestDays} 天 · 累计记录 ${state.streak.totalCheckInDays} 天",
                style = AnchorType.footnote,
                color = colors.labelSecondary,
            )
            state.streak.daysSinceLastRelapse?.let { days ->
                Text(
                    text = if (days == 0) "最近一次破戒：今天" else "距最近一次破戒：$days 天",
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
                text = "90 天进度 ${(state.progress * 100).toInt()}%" +
                    (state.nextMilestone?.let { " · 下一个里程碑：${it.title}" } ?: " · 已走完 90 天"),
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
                    "今天已打卡。"
                } else {
                    "还没打卡，一天一次。"
                },
                style = AnchorType.body,
                color = colors.label,
            )
            AnchorButton(
                text = if (state.streak.todayCheckedIn) "今天已打卡" else "打卡",
                onClick = onCheckIn,
                enabled = !state.streak.todayCheckedIn,
                modifier = Modifier.fillMaxWidth(),
            )
            AnchorButton(
                text = "打卡记录与感受",
                onClick = onOpenCheckIn,
                style = AnchorButtonStyle.Plain,
                modifier = Modifier.fillMaxWidth(),
            )
            state.message?.let { message ->
                Text(
                    text = message,
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
                text = "先做一次冲浪练习，或用十分钟延时把决定往后放。",
                style = AnchorType.body,
                color = colors.label,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnchorButton(
                    text = "渴求冲浪",
                    onClick = onOpenUrgeSurfing,
                    modifier = Modifier.weight(1f),
                )
                AnchorButton(
                    text = "十分钟延时",
                    onClick = onOpenDelayTool,
                    style = AnchorButtonStyle.Tinted,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = "90 天是社群参照，不是医学判定。",
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
                text = "71 篇离线文章，每篇标注来源与可信度。",
                style = AnchorType.body,
                color = colors.label,
            )
            AnchorButton(
                text = "按分类浏览",
                onClick = onOpenLibrary,
                style = AnchorButtonStyle.Tinted,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
