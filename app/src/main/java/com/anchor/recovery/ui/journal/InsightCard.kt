package com.anchor.recovery.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.anchor.recovery.core.relapse.DayPart
import com.anchor.recovery.core.relapse.FrequencyItem
import com.anchor.recovery.core.relapse.RelapseInsight

/**
 * F5 统计卡：情绪 Top-N、触发源 Top-N、时段分布、复吸间隔中位数。
 *
 * 展示口径刻意保守：只呈现"记录里出现过多少次"，不做因果或诊断式解释。
 *
 * 版式：日志页的主体是下面的时间线，所以默认只留「三个关键数字 + 免责口径」，
 * 详细分布收在「展开」里（展开状态用 rememberSaveable 记住，旋转不丢）。
 */
@Composable
fun RelapseInsightCard(insight: RelapseInsight, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "触发因素统计", style = MaterialTheme.typography.titleSmall)
                if (!insight.isEmpty) {
                    TextButton(onClick = { expanded = !expanded }) {
                        Text(text = if (expanded) "收起" else "展开")
                    }
                }
            }

            if (insight.isEmpty) {
                Text(
                    text = "还没有破戒记录。这里只统计你写下的内容，无记录时不显示任何推断。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }

            // 三个关键数字并成一行：原来每格两行，仅这一块就占掉约 40dp。
            Text(
                text = "破戒 ${insight.totalRelapses} 次 · 中位间隔 " +
                    (insight.medianIntervalHours?.let(::formatHours) ?: "不足 2 条记录") +
                    " · 带备注 ${insight.noteCount} 条",
                style = MaterialTheme.typography.bodySmall,
            )

            if (expanded) {
                FrequencyLine(title = "情绪 Top ${insight.emotionTop.size}", items = insight.emotionTop)
                FrequencyLine(
                    title = "触发源 Top ${insight.triggerTop.size}",
                    items = insight.triggerTop,
                )
                DayPartBars(counts = insight.dayPartCounts)
            }

            Text(
                text = "只说明记录里出现过什么，不代表因果关系或医学结论。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 一行一条 Top 榜：标题加粗，条目用「·」内联，避免每个条目独占一行。 */
@Composable
private fun FrequencyLine(title: String, items: List<FrequencyItem>, modifier: Modifier = Modifier) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(title) }
            append("：")
            if (items.isEmpty()) {
                append("暂无数据")
            } else {
                append(items.joinToString(" · ") { "${it.label} ${it.count} 次" })
            }
        },
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier,
    )
}

/**
 * 时段分布：四个时段各占一列，条长为列宽、填充按占最大值比例。
 * 原来四个整行进度条（每行 label + 条）要 100dp 以上，且 0 值那条会由
 * MaterialProgressIndicator 画上末端圆点，看着像一个数值标记。
 */
@Composable
private fun DayPartBars(counts: Map<DayPart, Int>, modifier: Modifier = Modifier) {
    val max = counts.values.maxOrNull()?.coerceAtLeast(1) ?: 1
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DayPart.entries.forEach { part ->
            val count = counts[part] ?: 0
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(text = "$count 次", style = MaterialTheme.typography.labelSmall)
                DistributionBar(fraction = count.toFloat() / max)
                Text(
                    text = part.shortName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DistributionBar(fraction: Float, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(2.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        if (fraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

/** 紧凑排版只放时段名（"上午 05–11" → "上午"），区间本身是固定值，不必每屏重复。 */
private val DayPart.shortName: String get() = label.substringBefore(' ')

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
