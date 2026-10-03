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
import androidx.compose.material3.TextButton
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
import com.anchor.recovery.core.urge.DelayTool
import kotlinx.coroutines.delay

/**
 * F4 十分钟延时：先不下判断，把决定往后放十分钟。
 *
 * 倒计时数值全部由 [DelayTool]（注入 Clock）算出，这里只用一个每秒自增的计数触发重组。
 */
@Composable
fun DelayToolScreen(
    viewModel: DelayToolViewModel,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var started by remember { mutableStateOf(false) }
    var initialIntensity by remember { mutableIntStateOf(6) }
    var endIntensity by remember { mutableIntStateOf(3) }
    var answered by remember { mutableStateOf<Boolean?>(null) }
    var uiState by remember { mutableStateOf(viewModel.state()) }
    var tick by remember { mutableIntStateOf(0) }

    // 计时 UI：LaunchedEffect 每秒重新向 core 要一次剩余时间。
    LaunchedEffect(started, tick) {
        if (started && !uiState.completed) {
            delay(1_000)
            uiState = viewModel.state()
            tick++
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        when {
            !started -> StartCard(
                initialIntensity = initialIntensity,
                onIntensityChange = { initialIntensity = it },
                onStart = {
                    viewModel.start()
                    uiState = viewModel.state()
                    started = true
                    tick = 0
                },
            )

            uiState.saved -> SavedCard(onDone = onDone)

            !uiState.completed -> RunningCard(uiState = uiState, onCancel = {
                viewModel.reset()
                started = false
            })

            else -> OutcomeCard(
                initialIntensity = initialIntensity,
                answered = answered,
                endIntensity = endIntensity,
                onAnswer = { passed ->
                    answered = passed
                    endIntensity = if (passed) 3 else 8
                },
                onIntensityChange = { endIntensity = it },
                onSave = {
                    viewModel.recordOutcome(
                        initialIntensity = initialIntensity,
                        endIntensity = endIntensity,
                    )
                    uiState = viewModel.state()
                },
            )
        }

        Text(
            text = "不替你决定做什么，只是把决定推迟十分钟。不构成医疗建议。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StartCard(
    initialIntensity: Int,
    onIntensityChange: (Int) -> Unit,
    onStart: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = "把决定往后放十分钟", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "这十分钟里只做两件事：跟着呼吸，看看自己写下的理由。",
                style = MaterialTheme.typography.bodyMedium,
            )
            IntensityRow(
                label = "开始前的渴求强度",
                intensity = initialIntensity,
                onIntensityChange = onIntensityChange,
            )
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                Text(text = "开始计时")
            }
        }
    }
}

@Composable
private fun RunningCard(uiState: DelayUiState, onCancel: () -> Unit) {
    val cue = BreathingPattern.cue(uiState.elapsedSec)
    val prompt = uiState.prompts.getOrNull(uiState.promptIndex) ?: uiState.prompts.firstOrNull().orEmpty()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = uiState.remainingLabel,
                style = MaterialTheme.typography.displayMedium,
            )
            LinearProgressIndicator(
                progress = { uiState.progress },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = cue.label, style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "保持 ${cue.remainingSec} 秒（吸气 4 / 停 2 / 呼气 6）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (prompt.isNotEmpty()) {
                Text(text = "“$prompt”", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "每 ${DelayTool.PROMPT_ROTATE_SEC} 秒换一条，可在设置里改成自己的话。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                Text(text = "结束计时")
            }
        }
    }
}

@Composable
private fun OutcomeCard(
    initialIntensity: Int,
    answered: Boolean?,
    endIntensity: Int,
    onAnswer: (Boolean) -> Unit,
    onIntensityChange: (Int) -> Unit,
    onSave: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "十分钟到了", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "刚才那股冲动，现在过去了吗？如实答就好。",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = { onAnswer(true) },
                    modifier = Modifier.weight(1f),
                ) { Text(text = "过去了") }
                OutlinedButton(
                    onClick = { onAnswer(false) },
                    modifier = Modifier.weight(1f),
                ) { Text(text = "还在") }
            }
            IntensityRow(
                label = "现在的渴求强度",
                intensity = endIntensity,
                onIntensityChange = onIntensityChange,
            )
            Text(
                text = "开始前 $initialIntensity / 10 → 现在 $endIntensity / 10",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onSave,
                enabled = answered != null,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(text = if (answered == null) "先回答问题" else "保存这次记录") }
        }
    }
}

@Composable
private fun SavedCard(onDone: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = "已记录", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "你刚把行动推迟了十分钟，这条记录会出现在日志页。",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text(text = "回到工具页")
            }
        }
    }
}

@Composable
private fun IntensityRow(
    label: String,
    intensity: Int,
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
