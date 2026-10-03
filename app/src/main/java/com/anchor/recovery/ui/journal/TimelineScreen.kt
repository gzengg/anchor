package com.anchor.recovery.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anchor.recovery.R
import com.anchor.recovery.ui.TimelineEntry
import com.anchor.recovery.ui.TimelineFilter
import com.anchor.recovery.ui.TimelineViewModel
import com.anchor.recovery.ui.components.AnchorButton
import com.anchor.recovery.ui.components.AnchorButtonStyle
import com.anchor.recovery.ui.components.AnchorHairline
import com.anchor.recovery.ui.components.AnchorListGroup
import com.anchor.recovery.ui.components.AnchorSegmentedControl
import com.anchor.recovery.ui.components.PublishAnchorNavBar
import com.anchor.recovery.ui.theme.AnchorTheme
import com.anchor.recovery.ui.theme.AnchorType
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * F5 日志页：打卡 / 渴求 / 破戒 合并成一条时间线（三张表在 ViewModel 里合成）。
 *
 * 版式为 iOS 分组列表：顶部分段控件筛类型，统计与动作收进白卡，下面是时间线。
 */
@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel,
    onOpenRelapseEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    // 页内不放醒目大标题（内联标题「日志」常驻），只把滚动状态交给顶栏。
    val listState = rememberLazyListState()
    PublishAnchorNavBar(listState, hasLargeTitle = false)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AnchorTheme.colors.groupedBackground),
    ) {
        // 分段控件的 label 参数不是 @Composable：先把每个筛选项的文案取成资源。
        val filterLabels = mapOf(
            TimelineFilter.ALL to stringResource(R.string.journal_filter_all),
            TimelineFilter.CHECK_IN to stringResource(R.string.journal_filter_check_in),
            TimelineFilter.URGE to stringResource(R.string.journal_filter_urge),
            TimelineFilter.RELAPSE to stringResource(R.string.journal_filter_relapse),
        )
        AnchorSegmentedControl(
            options = TimelineFilter.entries,
            selected = state.filter,
            onSelect = viewModel::selectFilter,
            label = { filter -> "${filterLabels.getValue(filter)} ${countOf(state, filter)}" },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )

        AnchorListGroup {
            Text(
                text = if (state.totalCount == 0) {
                    stringResource(R.string.journal_empty_hint)
                } else {
                    pluralStringResource(
                        R.plurals.journal_summary,
                        state.totalCount,
                        state.totalCount,
                        state.checkInCount,
                        state.urgeCount,
                        state.relapseCount,
                    )
                },
                style = AnchorType.footnote,
                color = AnchorTheme.colors.labelSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
            AnchorHairline(inset = 0.dp)
            AnchorButton(
                text = stringResource(R.string.journal_record_relapse),
                onClick = onOpenRelapseEdit,
                style = AnchorButtonStyle.Plain,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        RelapseInsightCard(insight = state.insight)

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(top = 12.dp),
            state = listState,
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
    AnchorListGroup(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = entry.label(),
                    style = AnchorType.subheadlineSemibold,
                    color = AnchorTheme.colors.label,
                )
                Text(
                    text = entry.at.formatLocal(),
                    style = AnchorType.footnote,
                    color = AnchorTheme.colors.labelTertiary,
                )
            }
            entry.details().forEach { line ->
                Text(
                    text = line,
                    style = AnchorType.footnote,
                    color = AnchorTheme.colors.labelSecondary,
                )
            }
            entry.accent()?.let { accent ->
                Surface(
                    color = accent.container,
                    contentColor = accent.content,
                    shape = RoundedCornerShape(6.dp),
                ) {
                    Text(
                        text = accent.text,
                        style = AnchorType.footnote,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineEntry.label(): String = when (this) {
    is TimelineEntry.CheckIn -> if (record.note.isBlank()) {
        stringResource(R.string.journal_label_check_in)
    } else {
        stringResource(R.string.journal_label_check_in_with_note)
    }

    is TimelineEntry.Urge -> stringResource(R.string.journal_label_urge, record.tool.label)
    is TimelineEntry.Relapse -> stringResource(R.string.journal_label_relapse)
}

@Composable
private fun TimelineEntry.details(): List<String> = when (this) {
    is TimelineEntry.CheckIn -> listOfNotNull(record.note.takeIf { it.isNotBlank() })
    is TimelineEntry.Urge -> listOf(
        pluralStringResource(
            R.plurals.journal_detail_duration,
            record.durationSec / 60,
            record.durationSec / 60,
            record.durationSec % 60,
        ),
        stringResource(R.string.journal_detail_intensity, record.peakIntensity, record.endIntensity),
    )

    is TimelineEntry.Relapse -> buildList {
        if (record.situation.isNotBlank()) {
            add(stringResource(R.string.journal_detail_situation, record.situation))
        }
        if (record.emotions.isNotEmpty()) {
            add(stringResource(R.string.journal_detail_emotions, record.emotions.joinToString("、")))
        }
        if (record.triggers.isNotEmpty()) {
            add(stringResource(R.string.journal_detail_triggers, record.triggers.joinToString("、")))
        }
        if (record.note.isNotBlank()) add(record.note)
    }
}

/** 条目底部的一行标注（颜色 + 文案）。 */
private data class Accent(val container: Color, val content: Color, val text: String)

/**
 * 破戒用醒目色、渴求用中性色，避免把“又记录了”渲染成负面判决。
 * 颜色一律取自 AnchorTheme 的语义色而不是硬编码值：浅色/深色方案都靠语义色才成立
 * （写死的浅色容器在深色模式下会变成刺眼的浅底）。
 */
@Composable
private fun TimelineEntry.accent(): Accent? {
    val colors = AnchorTheme.colors
    return when (this) {
        is TimelineEntry.Relapse -> Accent(
            // 与 AnchorButton 的 Tinted 同一种做法：危险色降透明度当容器色。
            container = colors.danger.copy(alpha = 0.14f),
            content = colors.danger,
            text = stringResource(R.string.journal_accent_relapse),
        )

        is TimelineEntry.Urge -> Accent(
            container = colors.fill,
            content = colors.labelSecondary,
            text = stringResource(R.string.journal_accent_urge),
        )

        is TimelineEntry.CheckIn -> null
    }
}

internal fun Instant.formatLocal(): String {
    val local = toLocalDateTime(TimeZone.currentSystemDefault())
    val hour = local.hour.toString().padStart(2, '0')
    val minute = local.minute.toString().padStart(2, '0')
    return "${local.date} $hour:$minute"
}
