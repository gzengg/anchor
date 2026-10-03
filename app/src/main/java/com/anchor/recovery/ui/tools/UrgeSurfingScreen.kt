package com.anchor.recovery.ui.tools

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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.anchor.recovery.R
import com.anchor.recovery.core.urge.BreathingPattern
import com.anchor.recovery.core.urge.UrgeSurfingSession
import com.anchor.recovery.core.urge.UrgeSurfingStage
import com.anchor.recovery.core.urge.UrgeSurfingState
import com.anchor.recovery.ui.components.AnchorButton
import com.anchor.recovery.ui.components.AnchorButtonStyle
import com.anchor.recovery.ui.components.AnchorLargeTitle
import com.anchor.recovery.ui.components.AnchorListGroup
import com.anchor.recovery.ui.components.PublishAnchorNavBar
import com.anchor.recovery.ui.text.breathingPhaseLabelRes
import com.anchor.recovery.ui.text.urgeSurfingStageHintRes
import com.anchor.recovery.ui.text.urgeSurfingStageLabelRes
import com.anchor.recovery.ui.theme.AnchorTheme
import com.anchor.recovery.ui.theme.AnchorType
import kotlinx.coroutines.delay

/**
 * F3 渴求冲浪：引入 → 呼吸引导 → 观察 → 等待 → 再次评分 → 完成。
 *
 * 计时只在这里用 `LaunchedEffect` 每秒推进一次，全部判断（阶段顺序、评分钳制、峰值/下降值）
 * 都在 [UrgeSurfingSession] 里完成。
 */
@Composable
fun UrgeSurfingScreen(
    viewModel: UrgeSurfingViewModel,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val session = remember { UrgeSurfingSession() }
    var state by remember { mutableStateOf(session.initial()) }
    var intensity by remember { mutableIntStateOf(5) }
    val startedAt = remember { viewModel.startedAt() }

    // 计时阶段每秒推进一次；RATE_AGAIN 等用户操作，不自动推进。
    LaunchedEffect(state.stage, state.abandoned) {
        while (!state.finished && !state.abandoned && session.remainingSecInStage(state) > 0) {
            delay(1_000)
            state = session.tick(state)
        }
    }

    // 进入 DONE 后落库（无论是完成还是中途放弃）。
    LaunchedEffect(state.stage) {
        if (state.stage == UrgeSurfingStage.DONE) {
            viewModel.save(session.result(state, startedAt))
        }
    }

    val scrollState = rememberScrollState()
    PublishAnchorNavBar(scrollState)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AnchorTheme.colors.groupedBackground)
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AnchorLargeTitle(stringResource(R.string.urge_title))

        StageHeader(state = state)

        when (state.stage) {
            UrgeSurfingStage.INTRO -> {
                AnchorListGroup {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        IntensityPicker(intensity = intensity, onIntensityChange = { intensity = it })
                        AnchorButton(
                            text = stringResource(R.string.urge_start_button),
                            onClick = { state = session.start(state, intensity) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            UrgeSurfingStage.RATE_AGAIN -> {
                AnchorListGroup {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        IntensityPicker(
                            intensity = intensity,
                            label = stringResource(R.string.urge_intensity_label),
                            onIntensityChange = { intensity = it },
                        )
                        AnchorButton(
                            text = stringResource(R.string.urge_finish_button),
                            onClick = { state = session.finish(state, intensity) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            UrgeSurfingStage.DONE -> {
                ResultCard(state = state)
                AnchorButton(
                    text = stringResource(R.string.urge_back_to_tools_button),
                    onClick = onDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }

            else -> {
                TimerCard(
                    state = state,
                    remainingSec = session.remainingSecInStage(state),
                    progress = session.stageProgress(state),
                )
                if (state.stage == UrgeSurfingStage.BREATHE) {
                    BreathingHint(elapsedSecInStage = state.elapsedSecInStage)
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AnchorButton(
                        text = stringResource(R.string.urge_skip_button),
                        onClick = { state = session.skipStage(state) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    AnchorButton(
                        text = stringResource(R.string.urge_stop_button),
                        onClick = { state = session.abandon(state) },
                        style = AnchorButtonStyle.Plain,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.urge_footnote),
            style = AnchorType.footnote,
            color = AnchorTheme.colors.labelSecondary,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun StageHeader(state: UrgeSurfingState) {
    val stageLabel = stringResource(urgeSurfingStageLabelRes(state.stage))
    val stageHint = stringResource(urgeSurfingStageHintRes(state.stage))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "${state.stage.ordinal + 1}/6 · $stageLabel",
            style = AnchorType.footnoteSemibold,
            color = AnchorTheme.colors.tint,
        )
        Text(
            text = stageHint,
            style = AnchorType.body,
            color = AnchorTheme.colors.label,
        )
    }
}

@Composable
private fun IntensityPicker(
    intensity: Int,
    label: String = stringResource(R.string.urge_intensity_label),
    onIntensityChange: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = AnchorType.subheadlineSemibold,
                color = AnchorTheme.colors.label,
            )
            Text(
                text = "$intensity / 10",
                style = AnchorType.headline,
                color = AnchorTheme.colors.label,
            )
        }
        Slider(
            value = intensity.toFloat(),
            onValueChange = { onIntensityChange(it.toInt().coerceIn(1, 10)) },
            valueRange = 1f..10f,
            steps = 8,
            colors = SliderDefaults.colors(
                thumbColor = AnchorTheme.colors.tint,
                activeTrackColor = AnchorTheme.colors.tint,
                inactiveTrackColor = AnchorTheme.colors.fillStrong,
            ),
        )
    }
}

@Composable
private fun TimerCard(state: UrgeSurfingState, remainingSec: Int, progress: Float) {
    AnchorListGroup {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = formatSeconds(remainingSec),
                style = AnchorType.largeTitle,
                color = AnchorTheme.colors.label,
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = AnchorTheme.colors.tint,
                trackColor = AnchorTheme.colors.fillStrong,
            )
            Text(
                text = pluralStringResource(R.plurals.urge_elapsed_seconds, state.elapsedSecTotal, state.elapsedSecTotal),
                style = AnchorType.footnote,
                color = AnchorTheme.colors.labelSecondary,
            )
        }
    }
}

@Composable
private fun BreathingHint(elapsedSecInStage: Int) {
    val cue = BreathingPattern.cue(elapsedSecInStage)
    AnchorListGroup {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(breathingPhaseLabelRes(cue.phase)),
                style = AnchorType.title3,
                color = AnchorTheme.colors.label,
            )
            Text(
                text = pluralStringResource(R.plurals.urge_breathing_hold, cue.remainingSec, cue.remainingSec),
                style = AnchorType.footnote,
                color = AnchorTheme.colors.labelSecondary,
            )
        }
    }
}

@Composable
private fun ResultCard(state: UrgeSurfingState) {
    AnchorListGroup {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = if (state.abandoned) {
                    stringResource(R.string.urge_result_partial)
                } else {
                    stringResource(R.string.urge_result_finished)
                },
                style = AnchorType.headline,
                color = AnchorTheme.colors.label,
            )
            Text(
                text = pluralStringResource(
                    R.plurals.urge_result_summary,
                    state.elapsedSecTotal,
                    state.elapsedSecTotal,
                    state.peakIntensity?.toString() ?: "-",
                ) + (state.endIntensity?.let { stringResource(R.string.urge_result_end_intensity, it) } ?: ""),
                style = AnchorType.body,
                color = AnchorTheme.colors.label,
            )
            state.delta?.let { delta ->
                Text(
                    text = when {
                        delta > 0 -> stringResource(R.string.urge_delta_down, delta)
                        delta == 0 -> stringResource(R.string.urge_delta_same)
                        else -> stringResource(R.string.urge_delta_up)
                    },
                    style = AnchorType.body,
                    color = AnchorTheme.colors.label,
                )
            } ?: Text(
                text = stringResource(R.string.urge_no_end_score),
                style = AnchorType.body,
                color = AnchorTheme.colors.label,
            )
        }
    }
}

private fun formatSeconds(totalSec: Int): String {
    val safe = totalSec.coerceAtLeast(0)
    return "%d:%02d".format(safe / 60, safe % 60)
}
