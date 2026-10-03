package com.anchor.recovery.ui.home

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anchor.recovery.ui.CheckInUiState
import com.anchor.recovery.ui.CheckInViewModel

@Composable
fun CheckInScreen(
    viewModel: CheckInViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TodayStatusCard(state = state)

        OutlinedTextField(
            value = state.note,
            onValueChange = viewModel::updateNote,
            label = { Text(text = "今天的感受 / 触发情境（可留空）") },
            minLines = 3,
            enabled = !state.todayCheckedIn,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = viewModel::save,
                enabled = !state.todayCheckedIn,
                modifier = Modifier.weight(1f),
            ) {
                Text(text = if (state.todayCheckedIn) "今天已打卡" else "保存打卡")
            }
            OutlinedButton(
                onClick = viewModel::undoToday,
                enabled = state.todayCheckedIn,
            ) {
                Text(text = "撤销今天")
            }
        }

        state.message?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = "最近 ${state.recent.size} 次打卡", style = MaterialTheme.typography.titleMedium)
                if (state.recent.isEmpty()) {
                    Text(
                        text = "还没有打卡记录。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                state.recent.forEachIndexed { index, record ->
                    if (index > 0) HorizontalDivider()
                    Column {
                        Text(text = record.date.toString(), style = MaterialTheme.typography.bodyLarge)
                        if (record.note.isNotBlank()) {
                            Text(
                                text = record.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayStatusCard(state: CheckInUiState, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = state.today.toString(), style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (state.todayCheckedIn) "状态：已打卡" else "状态：今天还没打卡",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                text = "只支持为今天打卡：漏打一整天即断签，补打卡会让连续天数的含义失真。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
