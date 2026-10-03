package com.anchor.recovery.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anchor.recovery.core.phase.PhaseNote
import com.anchor.recovery.core.phase.WithdrawalPhase
import com.anchor.recovery.core.phase.WithdrawalPhaseCatalog
import com.anchor.recovery.core.phase.WithdrawalPhaseResolver
import com.anchor.recovery.ui.components.AnchorListGroup
import com.anchor.recovery.ui.library.SourceChips
import com.anchor.recovery.ui.theme.AnchorTheme
import com.anchor.recovery.ui.theme.AnchorType

private val phaseResolver = WithdrawalPhaseResolver()

/** 由当前连续天数解析阶段（无记录时按第 0 天处理）。 */
fun phaseFor(dayNumber: Int): WithdrawalPhase = phaseResolver.resolve(dayNumber)

/**
 * F2 戒断阶段卡：当前阶段 + 可能经历 + 可做的事，每条文案都带来源文章 id 可点开。
 *
 * 外观用 iOS 分组卡片；阶段名与页码是卡片内标题，不另起节标题。
 */
@Composable
fun PhaseCard(
    dayNumber: Int,
    onOpenArticle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AnchorTheme.colors
    val phase = phaseFor(dayNumber)
    val index = phaseResolver.indexOf(dayNumber)

    AnchorListGroup(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "当前阶段：${phase.name}",
                    style = AnchorType.headline,
                    color = colors.label,
                )
                Text(
                    text = "${index + 1}/${WithdrawalPhaseCatalog.phases.size}",
                    style = AnchorType.footnote,
                    color = colors.labelSecondary,
                )
            }
            Text(
                text = phase.dayLabel + "（连续 ${dayNumber.coerceAtLeast(0)} 天）",
                style = AnchorType.footnote,
                color = colors.tint,
            )
            Text(text = phase.headline, style = AnchorType.body, color = colors.label)

            NoteSection(
                title = "可能会经历（自我报告归纳，不是诊断）",
                notes = phase.expectation,
                onOpenArticle = onOpenArticle,
            )
            NoteSection(
                title = "这一阶段可以试试",
                notes = phase.coping,
                onOpenArticle = onOpenArticle,
            )

            phase.caution?.let { caution ->
                // 组件库没有「警示条」，保留 M3 Surface 只换语义色。
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = colors.danger.copy(alpha = 0.12f),
                    contentColor = colors.danger,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = caution,
                        style = AnchorType.footnote,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            Text(
                text = WithdrawalPhaseCatalog.HELP_SEEKING_NOTICE,
                style = AnchorType.footnote,
                color = colors.labelSecondary,
            )
        }
    }
}

@Composable
private fun NoteSection(
    title: String,
    notes: List<PhaseNote>,
    onOpenArticle: (String) -> Unit,
) {
    val colors = AnchorTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = AnchorType.subheadlineSemibold,
            color = colors.tint,
        )
        notes.forEach { note ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = note.text, style = AnchorType.body, color = colors.label)
                SourceChips(articleIds = note.sourceArticleIds, onOpenArticle = onOpenArticle)
            }
        }
    }
}
