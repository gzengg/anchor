package com.anchor.recovery.ui.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.anchor.recovery.AppInfo
import com.anchor.recovery.core.legal.Disclaimer
import com.anchor.recovery.core.notify.ReminderMessages

/**
 * 设置页（提示词 §S7）：每日提醒与通知权限、F4 提示语、数据导出 JSON、一键清空、隐私说明。
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var showFullDisclaimer by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(EXPORT_MIME),
    ) { uri ->
        val json = state.pendingExportJson
        if (uri == null || json == null) {
            viewModel.onExportCancelled()
        } else {
            runCatching {
                val stream = context.contentResolver.openOutputStream(uri)
                    ?: error("无法写入所选位置")
                stream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            }.onSuccess { viewModel.onExportWritten() }
                .onFailure { viewModel.onExportFailed(it.message ?: "未知错误") }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.setReminderEnabled(true)
        } else {
            viewModel.onPermissionDenied()
        }
    }

    // 导出内容就绪后再弹系统文件选择器，避免先弹出空文件。
    LaunchedEffect(state.pendingExportJson) {
        if (state.pendingExportJson != null) {
            exportLauncher.launch(state.pendingExportFileName)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingsCard(title = "每日提醒") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "每天提醒我记录一次",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = state.reminderEnabled,
                    onCheckedChange = { checked ->
                        if (checked && needsNotificationPermission(context)) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.setReminderEnabled(checked)
                        }
                    },
                )
            }
            if (state.reminderEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(onClick = { viewModel.shiftReminderTime(-STEP_MINUTES) }) {
                        Text(text = "−5 分钟")
                    }
                    Text(
                        text = state.reminderTime.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(onClick = { viewModel.shiftReminderTime(STEP_MINUTES) }) {
                        Text(text = "+5 分钟")
                    }
                }
            }
            Text(
                // 提前把锁屏可见的文案展示出来：提醒不含任何用途描述。
                text = "通知文案（锁屏可见）：「${ReminderMessages.title()}」" +
                    "${ReminderMessages.body(0)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SettingsCard(title = "提示语（十分钟延时里轮播）") {
            OutlinedTextField(
                value = state.promptDraft,
                onValueChange = viewModel::updatePromptDraft,
                label = { Text(text = "每行一条，留空则用默认三条") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = viewModel::savePrompts) { Text(text = "保存提示语") }
        }

        SettingsCard(title = "数据导出") {
            Text(text = state.countsLine, style = MaterialTheme.typography.bodyMedium)
            Button(
                onClick = viewModel::prepareExport,
                enabled = state.hasAnyRecord,
            ) {
                Text(text = "导出为 JSON")
            }
            Text(
                text = "导出文件含打卡、破戒、渴求与问卷结果，位置由你选，磐石不会外发。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SettingsCard(title = "清空数据") {
            Text(
                text = "清空后无法恢复：记录不会有云端备份，也不会有回收站。",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = viewModel::requestClear,
                enabled = state.hasAnyRecord,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text(text = "清空全部数据")
            }
        }

        SettingsCard(title = "隐私说明") {
            Text(text = Disclaimer.PRIVACY_PARAGRAPH, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "磐石没有账号，不联网传输记录，也不读通讯录、位置或相册。",
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = { showFullDisclaimer = !showFullDisclaimer }) {
                Text(text = if (showFullDisclaimer) "收起完整声明" else "查看完整声明")
            }
            if (showFullDisclaimer) {
                Disclaimer.paragraphs.forEach { paragraph ->
                    Text(text = "· $paragraph", style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(
                text = "${AppInfo.DISPLAY_NAME} v${AppInfo.VERSION} · 声明版本 ${state.disclaimerVersion}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        state.message?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }

    if (state.awaitingClearConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::cancelClear,
            title = { Text(text = "确认清空全部数据？") },
            text = {
                Text(
                    text = "打卡、破戒、渴求和问卷记录都会被删除，且无法恢复；提醒时间与提示语会保留。",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmClear,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(text = "清空")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelClear) { Text(text = "取消") }
            },
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

private const val EXPORT_MIME = "application/json"
private const val STEP_MINUTES = 5

/** Android 13 起 POST_NOTIFICATIONS 需要运行时授权；更低版本安装即授权。 */
private fun needsNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) != PackageManager.PERMISSION_GRANTED
