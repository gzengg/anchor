package com.anchor.recovery.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anchor.recovery.core.relapse.RelapseTags

/**
 * F5 复吸记录表单：情境 / 情绪（多选）/ 触发源（多选）/ 备注。
 *
 * 时间取记录当下的时刻（一律经 Clock），页面不做任何评价性表达。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RelapseEditScreen(
    viewModel: RelapseEditViewModel,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "如实记录就好。复吸是常见现象，这条记录只用于回看规律。",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "保存后，连续天数会从今天重新计算。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = state.situation,
            onValueChange = viewModel::updateSituation,
            label = { Text(text = "情境（在哪、和谁、在做什么）") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
        )

        TagSection(
            title = "情绪（可多选）",
            options = RelapseTags.emotions,
            selected = state.emotions,
            onToggle = viewModel::toggleEmotion,
        )

        TagSection(
            title = "触发源（可多选）",
            options = RelapseTags.triggers,
            selected = state.triggers,
            onToggle = viewModel::toggleTrigger,
        )

        OutlinedTextField(
            value = state.note,
            onValueChange = viewModel::updateNote,
            label = { Text(text = "备注（当时在想什么）") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
        )

        state.message?.let { message ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(text = message, style = MaterialTheme.typography.bodyMedium)
                    if (state.saved) {
                        Button(onClick = onSaved, modifier = Modifier.fillMaxWidth()) {
                            Text(text = "去看日志与统计")
                        }
                    }
                }
            }
        }

        Button(
            onClick = viewModel::save,
            enabled = state.canSave,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(text = "保存记录") }

        Text(
            text = "情绪与触发源都是你自己选择的标签，统计数据只说明“记录里出现过多少次”，不代表因果关系。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagSection(
    title: String,
    options: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text = title, style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = option in selected,
                    onClick = { onToggle(option) },
                    label = { Text(text = option) },
                )
            }
        }
    }
}
