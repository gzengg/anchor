package com.anchor.recovery.ui.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anchor.recovery.ui.TimelineEntry
import com.anchor.recovery.ui.TimelineFilter
import com.anchor.recovery.ui.TimelineViewModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * F5 日志页：打卡 / 渴求 / 复吸 合并成一条时间线（三张表在 ViewModel 里合成）。
 */
@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TimelineFilter.entries.forEach { filter ->
                FilterChip(
                    selected = state.filter == filter,
                    onClick = { viewModel.selectFilter(filter) },
                    label = { Text(text = "${filter.label} ${countOf(state, filter)}") },
                )
            }
        }

        Text(
            text = if (state.totalCount == 0) {
                "还没有记录。打卡、渴求练习或复吸记录都会出现在这里。"
            } else {
                "共 ${state.totalCount} 条记录 · 打卡 ${state.checkInCount} · 渴求 ${state.urgeCount} · 复吸 ${state.relapseCount}"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            items(items = state.entries) { entry ->
                TimelineRow(entry = entry)
            }
        }
    }
}

private fun countOf(state: com.anchor.recovery.ui.TimelineUiState, filter: TimelineFilter): Int =
    when (filter) {
        TimelineFilter.ALL -> state.totalCount
        TimelineFilter.CHECK_IN -> state.checkInCount
        TimelineFilter.URGE -> state.urgeCount
        TimelineFilter.RELAPSE -> state.relapseCount
    }

@Composable
private fun TimelineRow(entry: TimelineEntry, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = entry.label(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = entry.at.formatLocal(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            entry.details().forEach { line ->
                Text(text = line, style = MaterialTheme.typography.bodySmall)
            }
            entry.accent()?.let { accent ->
                Surface(
                    color = accent.container,
                    contentColor = accent.content,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        text = accent.text,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        }
    }
}

private fun TimelineEntry.label(): String = when (this) {
    is TimelineEntry.CheckIn -> if (record.note.isBlank()) "打卡" else "打卡 · 有备注"
    is TimelineEntry.Urge -> "渴求事件 · ${record.tool.label}"
    is TimelineEntry.Relapse -> "复吸记录"
}

private fun TimelineEntry.details(): List<String> = when (this) {
    is TimelineEntry.CheckIn -> listOfNotNull(record.note.takeIf { it.isNotBlank() })
    is TimelineEntry.Urge -> listOf(
        "持续 ${record.durationSec / 60} 分 ${record.durationSec % 60} 秒",
        "峰值强度 ${record.peakIntensity}/10 → 结束时 ${record.endIntensity}/10",
    )

    is TimelineEntry.Relapse -> buildList {
        if (record.situation.isNotBlank()) add("情境：${record.situation}")
        if (record.emotions.isNotEmpty()) add("情绪：${record.emotions.joinToString("、")}")
        if (record.triggers.isNotEmpty()) add("触发因素：${record.triggers.joinToString("、")}")
        if (record.note.isNotBlank()) add(record.note)
    }
}

/** 条目底部的一行标注（颜色 + 文案）。 */
private data class Accent(val container: Color, val content: Color, val text: String)

/** 复吸用醒目色、渴求用中性色，避免把“又记录了”渲染成负面判决。 */
private fun TimelineEntry.accent(): Accent? = when (this) {
    is TimelineEntry.Relapse -> Accent(
        container = Color(0xFFFFDAD6),
        content = Color(0xFF410002),
        text = "已在复吸后重置天数 · 记录本身就是有效动作",
    )

    is TimelineEntry.Urge -> Accent(
        container = Color(0xFFDDE3EA),
        content = Color(0xFF161C22),
        text = "渴求平均 3–5 分钟达峰后自行下降",
    )

    is TimelineEntry.CheckIn -> null
}

internal fun Instant.formatLocal(): String {
    val local = toLocalDateTime(TimeZone.currentSystemDefault())
    val hour = local.hour.toString().padStart(2, '0')
    val minute = local.minute.toString().padStart(2, '0')
    return "${local.date} $hour:$minute"
}
