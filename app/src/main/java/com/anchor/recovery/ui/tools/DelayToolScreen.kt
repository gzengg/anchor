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
import com.anchor.recovery.core.urge.DelayTool
import com.anchor.recovery.ui.components.AnchorButton
import com.anchor.recovery.ui.components.AnchorButtonStyle
import com.anchor.recovery.ui.components.AnchorLargeTitle
import com.anchor.recovery.ui.components.AnchorListGroup
import com.anchor.recovery.ui.components.PublishAnchorNavBar
import com.anchor.recovery.ui.theme.AnchorTheme
import com.anchor.recovery.ui.theme.AnchorType
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
        AnchorLargeTitle(stringResource(R.string.delay_title))

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
            text = stringResource(R.string.delay_footnote),
            style = AnchorType.footnote,
            color = AnchorTheme.colors.labelSecondary,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun StartCard(
    initialIntensity: Int,
    onIntensityChange: (Int) -> Unit,
    onStart: () -> Unit,
) {
    AnchorListGroup {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.delay_start_heading),
                style = AnchorType.title3,
                color = AnchorTheme.colors.label,
            )
            Text(
                text = stringResource(R.string.delay_start_hint),
                style = AnchorType.body,
                color = AnchorTheme.colors.label,
            )
            IntensityRow(
                label = stringResource(R.string.delay_initial_intensity_label),
                intensity = initialIntensity,
                onIntensityChange = onIntensityChange,
            )
            AnchorButton(
                text = stringResource(R.string.delay_start_button),
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun RunningCard(uiState: DelayUiState, onCancel: () -> Unit) {
    val cue = BreathingPattern.cue(uiState.elapsedSec)
    val prompt = uiState.prompts.getOrNull(uiState.promptIndex) ?: uiState.prompts.firstOrNull().orEmpty()

    AnchorListGroup {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = uiState.remainingLabel,
                style = AnchorType.largeTitle,
                color = AnchorTheme.colors.label,
            )
            LinearProgressIndicator(
                progress = { uiState.progress },
                modifier = Modifier.fillMaxWidth(),
                color = AnchorTheme.colors.tint,
                trackColor = AnchorTheme.colors.fillStrong,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = cue.label,
                    style = AnchorType.title3,
                    color = AnchorTheme.colors.label,
                )
                Text(
                    text = pluralStringResource(R.plurals.delay_breathing_hold, cue.remainingSec, cue.remainingSec),
                    style = AnchorType.footnote,
                    color = AnchorTheme.colors.labelSecondary,
                )
            }
            if (prompt.isNotEmpty()) {
                Text(
                    text = "“$prompt”",
                    style = AnchorType.body,
                    color = AnchorTheme.colors.label,
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.delay_prompt_rotate_hint,
                        DelayTool.PROMPT_ROTATE_SEC,
                        DelayTool.PROMPT_ROTATE_SEC,
                    ),
                    style = AnchorType.footnote,
                    color = AnchorTheme.colors.labelSecondary,
                )
            }
            AnchorButton(
                text = stringResource(R.string.delay_stop_button),
                onClick = onCancel,
                style = AnchorButtonStyle.Tinted,
                modifier = Modifier.fillMaxWidth(),
            )
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
    AnchorListGroup {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.delay_done_heading),
                style = AnchorType.title3,
                color = AnchorTheme.colors.label,
            )
            Text(
                text = stringResource(R.string.delay_done_question),
                style = AnchorType.body,
                color = AnchorTheme.colors.label,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AnchorButton(
                    text = stringResource(R.string.delay_answer_passed),
                    onClick = { onAnswer(true) },
                    modifier = Modifier.weight(1f),
                )
                AnchorButton(
                    text = stringResource(R.string.delay_answer_still),
                    onClick = { onAnswer(false) },
                    style = AnchorButtonStyle.Tinted,
                    modifier = Modifier.weight(1f),
                )
            }
            IntensityRow(
                label = stringResource(R.string.delay_end_intensity_label),
                intensity = endIntensity,
                onIntensityChange = onIntensityChange,
            )
            Text(
                text = stringResource(
                    R.string.delay_intensity_compare,
                    initialIntensity,
                    endIntensity,
                ),
                style = AnchorType.footnote,
                color = AnchorTheme.colors.labelSecondary,
            )
            AnchorButton(
                text = if (answered == null) {
                    stringResource(R.string.delay_answer_prompt_button)
                } else {
                    stringResource(R.string.delay_save_button)
                },
                onClick = onSave,
                enabled = answered != null,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SavedCard(onDone: () -> Unit) {
    AnchorListGroup {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.delay_saved_heading),
                style = AnchorType.headline,
                color = AnchorTheme.colors.label,
            )
            Text(
                text = stringResource(R.string.delay_saved_hint),
                style = AnchorType.body,
                color = AnchorTheme.colors.label,
            )
            AnchorButton(
                text = stringResource(R.string.delay_back_to_tools_button),
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
            )
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
