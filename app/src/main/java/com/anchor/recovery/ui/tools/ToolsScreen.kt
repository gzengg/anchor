package com.anchor.recovery.ui.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 工具页：F3 渴求冲浪 / F4 十分钟延时 / F5 破戒记录 / F7+F8 自评问卷入口。
 * 具体流程页在各自阶段实现（S5 / S6）。
 */
@Composable
fun ToolsScreen(
    onOpenUrgeSurfing: () -> Unit,
    onOpenDelayTool: () -> Unit,
    onOpenRelapseEdit: () -> Unit,
    onOpenAssessmentHub: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ToolCard(
            title = "渴求冲浪",
            description = "把一次冲动拆成几步：说明 → 呼吸 → 观察 → 等待 → 再评分。",
            actionLabel = "开始",
            onAction = onOpenUrgeSurfing,
        )
        ToolCard(
            title = "十分钟延时",
            description = "先不做决定，把选择往后放十分钟，计时结束再评估。",
            actionLabel = "开始计时",
            onAction = onOpenDelayTool,
        )
        ToolCard(
            title = "记录一次破戒",
            description = "记下当时的情境、情绪和触发源，只看规律，不做评价。",
            actionLabel = "去记录",
            onAction = onOpenRelapseEdit,
        )
        ToolCard(
            title = "自评问卷",
            description = "成瘾倾向自评 19 题、道德冲突 vs 真实问题 12 题，都是参考，不是诊断。",
            actionLabel = "去作答",
            onAction = onOpenAssessmentHub,
        )
        Text(
            text = "工具仅用于自助记录与应对，不构成医疗建议。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ToolCard(
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = description, style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onAction) { Text(text = actionLabel) }
        }
    }
}
