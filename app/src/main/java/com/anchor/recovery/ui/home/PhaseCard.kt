package com.anchor.recovery.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anchor.recovery.core.phase.PhaseNote
import com.anchor.recovery.core.phase.WithdrawalPhase
import com.anchor.recovery.core.phase.WithdrawalPhaseCatalog
import com.anchor.recovery.core.phase.WithdrawalPhaseResolver
import com.anchor.recovery.ui.library.SourceChips

private val phaseResolver = WithdrawalPhaseResolver()

/** 由当前连续天数解析阶段（无记录时按第 0 天处理）。 */
fun phaseFor(dayNumber: Int): WithdrawalPhase = phaseResolver.resolve(dayNumber)

/**
 * F2 戒断阶段卡：当前阶段 + 可能经历 + 可做的事，每条文案都带来源文章 id 可点开。
 */
@Composable
fun PhaseCard(
    dayNumber: Int,
    onOpenArticle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val phase = phaseFor(dayNumber)
    val index = phaseResolver.indexOf(dayNumber)

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "当前阶段：${phase.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${index + 1}/${WithdrawalPhaseCatalog.phases.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = phase.dayLabel + "（连续 ${dayNumber.coerceAtLeast(0)} 天）",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(text = phase.headline, style = MaterialTheme.typography.bodyMedium)

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
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(
                        text = caution,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            Text(
                text = WithdrawalPhaseCatalog.HELP_SEEKING_NOTICE,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        notes.forEach { note ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = note.text, style = MaterialTheme.typography.bodyMedium)
                SourceChips(articleIds = note.sourceArticleIds, onOpenArticle = onOpenArticle)
            }
        }
    }
}
