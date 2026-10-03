package com.anchor.recovery.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StageHeader(state = state)

        when (state.stage) {
            UrgeSurfingStage.INTRO -> {
                IntensityPicker(intensity = intensity, onIntensityChange = { intensity = it })
                Button(
                    onClick = { state = session.start(state, intensity) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(text = "开始") }
            }

            UrgeSurfingStage.RATE_AGAIN -> {
                IntensityPicker(
                    intensity = intensity,
                    label = "现在的渴求强度",
                    onIntensityChange = { intensity = it },
                )
                Button(
                    onClick = { state = session.finish(state, intensity) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(text = "完成这一轮") }
            }

            UrgeSurfingStage.DONE -> {
                ResultCard(state = state)
                Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "回到工具页")
                }
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
                Button(
                    onClick = { state = session.skipStage(state) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(text = "跳过这一步") }
                OutlinedButton(
                    onClick = { state = session.abandon(state) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(text = "先到这里") }
            }
        }

        Text(
            text = "自助工具，不构成医疗建议；若痛苦持续加重，请咨询医生。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StageHeader(state: UrgeSurfingState) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "${state.stage.ordinal + 1}/6 · ${state.stage.label}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(text = state.stage.hint, style = MaterialTheme.typography.bodyLarge)
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
            Text(text = label, style = MaterialTheme.typography.labelLarge)
            Text(text = "$intensity / 10", style = MaterialTheme.typography.titleMedium)
        }
        Slider(
            value = intensity.toFloat(),
            onValueChange = { onIntensityChange(it.toInt().coerceIn(1, 10)) },
            valueRange = 1f..10f,
            steps = 8,
        )
    }
}

@Composable
private fun TimerCard(state: UrgeSurfingState, remainingSec: Int, progress: Float) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = formatSeconds(remainingSec),
                style = MaterialTheme.typography.displaySmall,
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "已持续 ${state.elapsedSecTotal} 秒",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BreathingHint(elapsedSecInStage: Int) {
    val cue = BreathingPattern.cue(elapsedSecInStage)
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = cue.label, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "保持 ${cue.remainingSec} 秒 · 吸气 4 / 停 2 / 呼气 6",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ResultCard(state: UrgeSurfingState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = if (state.abandoned) "这一轮记作部分完成" else "这一轮结束了",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "用时 ${state.elapsedSecTotal} 秒 · 峰值 ${state.peakIntensity ?: "-"}/10" +
                    (state.endIntensity?.let { " · 结束 $it/10" } ?: ""),
                style = MaterialTheme.typography.bodyMedium,
            )
            state.delta?.let { delta ->
                Text(
                    text = when {
                        delta > 0 -> "下降 $delta 分：它自己退下去了。"
                        delta == 0 -> "强度没变，但也没有继续升高。"
                        else -> "比开始时更高。冲浪不保证每次都下降，你已经做到不立刻行动。"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            } ?: Text(
                text = "没有结束评分，这次不算完整一轮，但记录已保存。",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun formatSeconds(totalSec: Int): String {
    val safe = totalSec.coerceAtLeast(0)
    return "%d:%02d".format(safe / 60, safe % 60)
}
