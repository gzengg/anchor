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
import androidx.compose.ui.unit.dp
import com.anchor.recovery.core.urge.BreathingPattern
import com.anchor.recovery.core.urge.UrgeSurfingSession
import com.anchor.recovery.core.urge.UrgeSurfingStage
import com.anchor.recovery.core.urge.UrgeSurfingState
import com.anchor.recovery.ui.components.AnchorButton
import com.anchor.recovery.ui.components.AnchorButtonStyle
import com.anchor.recovery.ui.components.AnchorLargeTitle
import com.anchor.recovery.ui.components.AnchorListGroup
import com.anchor.recovery.ui.components.PublishAnchorNavBar
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
        AnchorLargeTitle("渴求冲浪")

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
                            text = "开始",
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
                            label = "现在的渴求强度",
                            onIntensityChange = { intensity = it },
                        )
                        AnchorButton(
                            text = "完成这一轮",
                            onClick = { state = session.finish(state, intensity) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            UrgeSurfingStage.DONE -> {
                ResultCard(state = state)
                AnchorButton(
                    text = "回到工具页",
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
                        text = "跳过这一步",
                        onClick = { state = session.skipStage(state) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    AnchorButton(
                        text = "先到这里",
                        onClick = { state = session.abandon(state) },
                        style = AnchorButtonStyle.Plain,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        Text(
            text = "自助工具，不构成医疗建议；若痛苦持续加重，请咨询医生。",
            style = AnchorType.footnote,
            color = AnchorTheme.colors.labelSecondary,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun StageHeader(state: UrgeSurfingState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "${state.stage.ordinal + 1}/6 · ${state.stage.label}",
            style = AnchorType.footnoteSemibold,
            color = AnchorTheme.colors.tint,
        )
        Text(
            text = state.stage.hint,
            style = AnchorType.body,
            color = AnchorTheme.colors.label,
        )
    }
}

@Composable
private fun IntensityPicker(
    intensity: Int,
    label: String = "现在的渴求强度",
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
                text = "已持续 ${state.elapsedSecTotal} 秒",
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
                text = cue.label,
                style = AnchorType.title3,
                color = AnchorTheme.colors.label,
            )
            Text(
                text = "保持 ${cue.remainingSec} 秒 · 吸气 4 / 停 2 / 呼气 6",
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
                text = if (state.abandoned) "这一轮记作部分完成" else "这一轮结束了",
                style = AnchorType.headline,
                color = AnchorTheme.colors.label,
            )
            Text(
                text = "用时 ${state.elapsedSecTotal} 秒 · 峰值 ${state.peakIntensity ?: "-"}/10" +
                    (state.endIntensity?.let { " · 结束 $it/10" } ?: ""),
                style = AnchorType.body,
                color = AnchorTheme.colors.label,
            )
            state.delta?.let { delta ->
                Text(
                    text = when {
                        delta > 0 -> "下降 $delta 分：它自己退下去了。"
                        delta == 0 -> "强度没变，但也没有继续升高。"
                        else -> "比开始时更高。冲浪不保证每次都下降，你已经做到不立刻行动。"
                    },
                    style = AnchorType.body,
                    color = AnchorTheme.colors.label,
                )
            } ?: Text(
                text = "没有结束评分，这次不算完整一轮，但记录已保存。",
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
