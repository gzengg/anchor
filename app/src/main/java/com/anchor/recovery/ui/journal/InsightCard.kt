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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.anchor.recovery.R
import com.anchor.recovery.core.relapse.DayPart
import com.anchor.recovery.core.relapse.FrequencyItem
import com.anchor.recovery.core.relapse.RelapseInsight
import com.anchor.recovery.ui.components.AnchorButton
import com.anchor.recovery.ui.components.AnchorButtonStyle
import com.anchor.recovery.ui.components.AnchorHairline
import com.anchor.recovery.ui.components.AnchorListGroup
import com.anchor.recovery.ui.components.AnchorSectionHeader
import com.anchor.recovery.ui.theme.AnchorTheme
import com.anchor.recovery.ui.theme.AnchorType

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

    Column(modifier = modifier.fillMaxWidth()) {
        AnchorSectionHeader(stringResource(R.string.insight_section_title))
        AnchorListGroup {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (insight.isEmpty) {
                    Text(
                        text = stringResource(R.string.insight_empty_hint),
                        style = AnchorType.footnote,
                        color = AnchorTheme.colors.labelSecondary,
                    )
                    return@Column
                }

                // 三个关键数字并成一行：原来每格两行，仅这一块就占掉约 40dp。
                val medianText = insight.medianIntervalHours?.let { formatHours(it) }
                    ?: stringResource(R.string.insight_median_insufficient)
                Text(
                    text = pluralStringResource(
                        R.plurals.insight_summary_prefix,
                        insight.totalRelapses,
                        insight.totalRelapses,
                    ) +
                        medianText +
                        pluralStringResource(
                            R.plurals.insight_summary_suffix,
                            insight.noteCount,
                            insight.noteCount,
                        ),
                    style = AnchorType.footnote,
                    color = AnchorTheme.colors.label,
                )

                if (expanded) {
                    FrequencyLine(
                        title = stringResource(R.string.insight_emotion_top, insight.emotionTop.size),
                        items = insight.emotionTop,
                    )
                    FrequencyLine(
                        title = stringResource(R.string.insight_trigger_top, insight.triggerTop.size),
                        items = insight.triggerTop,
                    )
                    DayPartBars(counts = insight.dayPartCounts)
                }

                Text(
                    text = stringResource(R.string.insight_footnote),
                    style = AnchorType.footnote,
                    color = AnchorTheme.colors.labelTertiary,
                )
            }

            if (!insight.isEmpty) {
                AnchorHairline(inset = 0.dp)
                AnchorButton(
                    text = if (expanded) {
                        stringResource(R.string.insight_collapse)
                    } else {
                        stringResource(R.string.insight_expand)
                    },
                    onClick = { expanded = !expanded },
                    style = AnchorButtonStyle.Plain,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** 一行一条 Top 榜：标题加粗，条目用「·」内联，避免每个条目独占一行。 */
@Composable
private fun FrequencyLine(title: String, items: List<FrequencyItem>, modifier: Modifier = Modifier) {
    // map 是 inline、joinToString 不是：条目文案先在可组合上下文里取好再拼。
    val itemTexts = items.map { item ->
        pluralStringResource(R.plurals.insight_frequency_item, item.count, item.label, item.count)
    }
    val body = if (itemTexts.isEmpty()) {
        stringResource(R.string.insight_frequency_empty)
    } else {
        itemTexts.joinToString(" · ")
    }
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(title) }
            append("：")
            append(body)
        },
        style = AnchorType.footnote,
        color = AnchorTheme.colors.label,
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
                Text(
                    text = pluralStringResource(R.plurals.insight_daypart_count, count, count),
                    style = AnchorType.footnote,
                    color = AnchorTheme.colors.label,
                )
                DistributionBar(fraction = count.toFloat() / max)
                Text(
                    text = part.shortName,
                    style = AnchorType.footnote,
                    color = AnchorTheme.colors.labelTertiary,
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
            .background(AnchorTheme.colors.fill),
    ) {
        if (fraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(AnchorTheme.colors.tint),
            )
        }
    }
}

/** 紧凑排版只放时段名（"上午 05–11" → "上午"），区间本身是固定值，不必每屏重复。 */
private val DayPart.shortName: String get() = label.substringBefore(' ')

/** 不足一天按小时显示，超过一天同时给出天数，避免"52.5 小时"这种读不出来的数字。 */
@Composable
private fun formatHours(hours: Double): String {
    val rounded = (hours * 10).toLong() / 10.0
    // 数量词用 plurals（中文只有 other）：count 只用于选形式，展示值仍是原样的 Double。
    return if (rounded < 24) {
        pluralStringResource(R.plurals.insight_hours, rounded.toInt(), rounded)
    } else {
        val days = (rounded / 24 * 10).toLong() / 10.0
        pluralStringResource(R.plurals.insight_hours_days, rounded.toInt(), rounded, days)
    }
}
