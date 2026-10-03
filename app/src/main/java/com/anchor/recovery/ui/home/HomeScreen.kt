package com.anchor.recovery.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anchor.recovery.ui.HomeUiState
import com.anchor.recovery.ui.HomeViewModel

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StreakCard(state = state)
        TodayCard(
            state = state,
            onCheckIn = viewModel::checkInToday,
            onOpenCheckIn = onOpenCheckIn,
        )
        PhaseCard(
            dayNumber = state.streak.currentDays,
            onOpenArticle = onOpenArticle,
        )
        QuickToolCard(
            onOpenUrgeSurfing = onOpenUrgeSurfing,
            onOpenDelayTool = onOpenDelayTool,
        )
        LibraryCard(onOpenLibrary = onOpenLibrary)
    }
}

@Composable
private fun StreakCard(state: HomeUiState, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = "当前连续记录", style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = state.streak.currentDays.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = " 天",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Text(
                text = "历史最长 ${state.streak.longestDays} 天 · 累计记录 ${state.streak.totalCheckInDays} 天",
                style = MaterialTheme.typography.bodyMedium,
            )
            state.streak.daysSinceLastRelapse?.let { days ->
                Text(
                    text = if (days == 0) "最近一次复吸：今天" else "距最近一次复吸：$days 天",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
            )
            Text(
                text = "90 天进度 ${(state.progress * 100).toInt()}%" +
                    (state.nextMilestone?.let { " · 下一个里程碑：${it.title}" } ?: " · 已走完 90 天"),
                style = MaterialTheme.typography.bodySmall,
            )
            state.nextMilestone?.let { milestone ->
                Text(text = milestone.hint, style = MaterialTheme.typography.bodySmall)
            }
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
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = "今天", style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (state.streak.todayCheckedIn) {
                    "今天已打卡。"
                } else {
                    "还没打卡。一天一次，点一下就算记录。"
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = onCheckIn,
                enabled = !state.streak.todayCheckedIn,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = if (state.streak.todayCheckedIn) "今天已打卡" else "打卡：今天")
            }
            TextButton(onClick = onOpenCheckIn, modifier = Modifier.fillMaxWidth()) {
                Text(text = "查看打卡记录 / 写下感受")
            }
            state.message?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
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
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = "现在有点难熬？", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "可以先做一次冲浪练习，或者用十分钟延时把决定往后放。",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOpenUrgeSurfing) { Text(text = "渴求冲浪") }
                OutlinedButton(onClick = onOpenDelayTool) { Text(text = "十分钟延时") }
            }
            Text(
                text = "90 天是社群常用的阶段参照，不是医学意义上的判定。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LibraryCard(
    onOpenLibrary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = "知识库", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "离线整理的文章，按六个分类整理，每篇都标注可信度与原文出处。",
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(onClick = onOpenLibrary, modifier = Modifier.fillMaxWidth()) {
                Text(text = "按分类浏览")
            }
        }
    }
}
