package com.anchor.recovery.ui.milestones

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anchor.recovery.core.streak.MilestoneStatus
import com.anchor.recovery.ui.components.AnchorHairline
import com.anchor.recovery.ui.components.AnchorLargeTitle
import com.anchor.recovery.ui.components.AnchorListGroup
import com.anchor.recovery.ui.components.PublishAnchorNavBar
import com.anchor.recovery.ui.theme.AnchorTheme
import com.anchor.recovery.ui.theme.AnchorType

/**
 * 里程碑徽章墙。
 *
 * 展示口径（与首页 streak 同一套 history 推导，见 `MilestoneAchievements`）：
 * 已达成 → 点亮 + 最近达成日期（多次达成时附上次数）；未达成 → 灰显 + 还差多少天。
 *
 * 文案边界：里程碑是**自助记录里的阶段参照**，不是疗效或「痊愈」判定，
 * 页脚固定免责说明，与知识库循证口径一致。
 */
@Composable
fun MilestoneWallScreen(
    viewModel: MilestoneWallViewModel,
    modifier: Modifier = Modifier,
) {
    val wall by viewModel.wall.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    PublishAnchorNavBar(scrollState)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AnchorTheme.colors.groupedBackground)
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp),
    ) {
        AnchorLargeTitle("里程碑")

        Text(
            text = "徽章按连续打卡天数点亮。断签或破戒后重新开始，已经点亮的徽章会保留。",
            style = AnchorType.footnote,
            color = AnchorTheme.colors.labelSecondary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        )

        AnchorListGroup {
            wall.forEachIndexed { index, status ->
                if (index > 0) AnchorHairline()
                MilestoneRow(status)
            }
        }

        Text(
            text = "里程碑只表示你在这里记录了多少天，不代表治疗结果或「痊愈」。",
            style = AnchorType.footnote,
            color = AnchorTheme.colors.labelSecondary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
        )
    }
}

@Composable
private fun MilestoneRow(status: MilestoneStatus) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Badge(days = status.milestone.days, achieved = status.achieved)

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = status.milestone.title,
                style = AnchorType.body,
                color = AnchorTheme.colors.label,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = statusLine(status),
                style = AnchorType.footnote,
                color = if (status.achieved) {
                    AnchorTheme.colors.tint
                } else {
                    AnchorTheme.colors.labelSecondary
                },
            )
        }
    }
}

@Composable
private fun Badge(days: Int, achieved: Boolean) {
    // 已达成：实心品牌色 + 白字（与 AnchorButton 的 Filled 同口径）；未达成：浅填充 + 次级字色。
    val background = if (achieved) AnchorTheme.colors.tint else AnchorTheme.colors.fill
    val foreground = if (achieved) Color.White else AnchorTheme.colors.labelTertiary
    Column(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = days.toString(),
            style = AnchorType.title2,
            color = foreground,
        )
        Text(
            text = "天",
            style = AnchorType.caption1,
            color = foreground,
        )
    }
}

/** 已达成：`6 月 1 日达成` / 多次达成时 `6 月 1 日达成 · 累计 3 次`；未达成：`还差 7 天`。 */
private fun statusLine(status: MilestoneStatus): String = when {
    status.achieved -> {
        val date = status.latestAchievedDate?.let { "${it.monthNumber} 月 ${it.dayOfMonth} 日" }.orEmpty()
        if (status.eraCount > 1) "$date 达成 · 累计 ${status.eraCount} 次" else "$date 达成"
    }

    else -> "还差 ${status.daysRemaining} 天"
}
