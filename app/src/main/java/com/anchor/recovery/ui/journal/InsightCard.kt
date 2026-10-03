package com.anchor.recovery.ui.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anchor.recovery.core.relapse.DayPart
import com.anchor.recovery.core.relapse.FrequencyItem
import com.anchor.recovery.core.relapse.RelapseInsight

/**
 * F5 统计卡：情绪 Top-N、触发源 Top-N、时段分布、复吸间隔中位数。
 *
 * 展示口径刻意保守：只呈现"记录里出现过多少次"，不做因果或诊断式解释。
 */
@Composable
fun RelapseInsightCard(insight: RelapseInsight, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = "触发因素统计", style = MaterialTheme.typography.titleSmall)

            if (insight.isEmpty) {
                Text(
                    text = "还没有破戒记录。这里只统计你写下的内容，无记录时不显示任何推断。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                StatCell(label = "破戒次数", value = "${insight.totalRelapses} 次")
                StatCell(
                    label = "中位间隔",
                    value = insight.medianIntervalHours?.let { formatHours(it) } ?: "不足 2 条记录",
                )
                StatCell(label = "带备注", value = "${insight.noteCount} 条")
            }

            FrequencyBlock(title = "情绪 Top ${insight.emotionTop.size}", items = insight.emotionTop)
            FrequencyBlock(title = "触发源 Top ${insight.triggerTop.size}", items = insight.triggerTop)

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = "时段分布", style = MaterialTheme.typography.labelLarge)
                DayPart.entries.forEach { part ->
                    val count = insight.dayPartCounts[part] ?: 0
                    val max = insight.dayPartCounts.values.maxOrNull()?.coerceAtLeast(1) ?: 1
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(text = part.label, style = MaterialTheme.typography.bodySmall)
                            Text(text = "$count 次", style = MaterialTheme.typography.bodySmall)
                        }
                        LinearProgressIndicator(
                            progress = { count.toFloat() / max },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            Text(
                text = "只说明记录里出现过什么，不代表因果关系或医学结论。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatCell(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun FrequencyBlock(title: String, items: List<FrequencyItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = title, style = MaterialTheme.typography.labelLarge)
        if (items.isEmpty()) {
            Text(
                text = "暂无数据",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }
        items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = item.label, style = MaterialTheme.typography.bodySmall)
                Text(text = "${item.count} 次", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** 不足一天按小时显示，超过一天同时给出天数，避免"52.5 小时"这种读不出来的数字。 */
private fun formatHours(hours: Double): String {
    val rounded = (hours * 10).toLong() / 10.0
    return if (rounded < 24) {
        "$rounded 小时"
    } else {
        val days = (rounded / 24 * 10).toLong() / 10.0
        "$rounded 小时（约 $days 天）"
    }
}
