package com.anchor.recovery.ui.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.anchor.recovery.ui.components.AnchorAlert
import com.anchor.recovery.ui.components.AnchorButton
import com.anchor.recovery.ui.components.AnchorButtonStyle
import com.anchor.recovery.ui.components.AnchorHairline
import com.anchor.recovery.ui.components.AnchorLargeTitle
import com.anchor.recovery.ui.components.AnchorListGroup
import com.anchor.recovery.ui.components.AnchorSectionHeader
import com.anchor.recovery.ui.components.AnchorSwitchRow
import com.anchor.recovery.ui.components.PublishAnchorNavBar
import com.anchor.recovery.ui.theme.AnchorTheme
import com.anchor.recovery.ui.theme.AnchorType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 设置页（提示词 §S7）：每日提醒与通知权限、F4 提示语、数据导出 JSON、一键清空、隐私说明。
 *
 * 版式为 iOS 分组列表：分节小标题 + 圆角分组卡片，动作放进卡片里而不是散在页面上。
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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

    // 不限定 MIME：部分文件管理器把 .json 报成 octet-stream，限定后反而选不中。
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) {
            viewModel.cancelImport()
            return@rememberLauncherForActivityResult
        }
        val fileName = importFileName(uri.lastPathSegment)
        scope.launch {
            val text = withContext(Dispatchers.IO) { readImportText(context, uri) }
            viewModel.onImportFilePicked(fileName, text)
        }
    }

    // 导出内容就绪后再弹系统文件选择器，避免先弹出空文件。
    LaunchedEffect(state.pendingExportJson) {
        if (state.pendingExportJson != null) {
            exportLauncher.launch(state.pendingExportFileName)
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
    ) {
        AnchorLargeTitle("设置")

        AnchorSectionHeader("每日提醒")
        AnchorListGroup {
            AnchorSwitchRow(
                title = "每天提醒我记录一次",
                checked = state.reminderEnabled,
                onCheckedChange = { checked ->
                    if (checked && needsNotificationPermission(context)) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.setReminderEnabled(checked)
                    }
                },
            )
            if (state.reminderEnabled) {
                AnchorHairline(inset = 0.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AnchorButton(
                        text = "−5 分钟",
                        onClick = { viewModel.shiftReminderTime(-STEP_MINUTES) },
                        style = AnchorButtonStyle.Tinted,
                    )
                    Text(
                        text = state.reminderTime.toString(),
                        style = AnchorType.body,
                        color = AnchorTheme.colors.label,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                    AnchorButton(
                        text = "+5 分钟",
                        onClick = { viewModel.shiftReminderTime(STEP_MINUTES) },
                        style = AnchorButtonStyle.Tinted,
                    )
                }
            }
            AnchorHairline(inset = 0.dp)
            Text(
                // 提前把锁屏可见的文案展示出来：提醒不含任何用途描述。
                text = "通知文案（锁屏可见）：「${ReminderMessages.title()}」" +
                    "${ReminderMessages.body(0)}",
                style = AnchorType.footnote,
                color = AnchorTheme.colors.labelSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }

        AnchorSectionHeader("提示语（十分钟延时里轮播）")
        AnchorListGroup {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = state.promptDraft,
                    onValueChange = viewModel::updatePromptDraft,
                    label = { Text(text = "每行一条，留空则用默认三条") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                AnchorButton(
                    text = "保存提示语",
                    onClick = viewModel::savePrompts,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        AnchorSectionHeader("数据导出")
        AnchorListGroup {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = state.countsLine,
                    style = AnchorType.body,
                    color = AnchorTheme.colors.labelSecondary,
                )
                AnchorButton(
                    text = "导出为 JSON",
                    onClick = viewModel::prepareExport,
                    enabled = state.hasAnyRecord,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "导出文件含打卡、破戒、渴求与问卷结果，位置由你选，磐石不会外发。",
                    style = AnchorType.footnote,
                    color = AnchorTheme.colors.labelSecondary,
                )
            }
        }

        AnchorSectionHeader("数据导入")
        AnchorListGroup {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "读一份之前导出的 JSON，整体替换当前记录。导入前会先自动导出一份当前数据作备份。",
                    style = AnchorType.body,
                )
                AnchorButton(
                    text = "从文件导入",
                    onClick = { importLauncher.launch(arrayOf("*/*")) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "只读你在系统文件选择器里选中的那一个文件，不认识别的位置。",
                    style = AnchorType.footnote,
                    color = AnchorTheme.colors.labelSecondary,
                )
            }
        }

        AnchorSectionHeader("清空数据")
        AnchorListGroup {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "清空后无法恢复：记录不会有云端备份，也不会有回收站。",
                    style = AnchorType.body,
                )
                AnchorButton(
                    text = "清空全部数据",
                    onClick = viewModel::requestClear,
                    enabled = state.hasAnyRecord,
                    destructive = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        AnchorSectionHeader("隐私说明")
        AnchorListGroup {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = Disclaimer.PRIVACY_PARAGRAPH, style = AnchorType.body)
                Text(
                    text = "磐石没有账号，不联网传输记录，也不读通讯录、位置或相册。",
                    style = AnchorType.body,
                )
                if (showFullDisclaimer) {
                    Disclaimer.paragraphs.forEach { paragraph ->
                        Text(
                            text = "· $paragraph",
                            style = AnchorType.footnote,
                            color = AnchorTheme.colors.labelSecondary,
                        )
                    }
                }
                Text(
                    text = "${AppInfo.DISPLAY_NAME} v${AppInfo.VERSION} · 声明版本 ${state.disclaimerVersion}",
                    style = AnchorType.footnote,
                    color = AnchorTheme.colors.labelSecondary,
                )
            }
            AnchorHairline(inset = 0.dp)
            AnchorButton(
                text = if (showFullDisclaimer) "收起完整声明" else "查看完整声明",
                onClick = { showFullDisclaimer = !showFullDisclaimer },
                style = AnchorButtonStyle.Plain,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        state.message?.let { message ->
            Text(
                text = message,
                style = AnchorType.footnote,
                color = AnchorTheme.colors.tint,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
            )
        }
    }

    state.pendingImport?.let { pending ->
        AnchorAlert(
            title = "用这份文件替换现有记录？",
            message = "文件：${pending.fileName}\n" +
                "导出时间：${humanReadableTime(pending.exportedAt)}\n" +
                "含 ${pending.countsLine}\n\n" +
                "当前记录会被这份文件整体替换，替换前先导出一份备份。",
            confirmLabel = "备份并导入",
            onConfirm = viewModel::confirmImport,
            dismissLabel = "取消",
            onDismiss = viewModel::cancelImport,
        )
    }

    if (state.awaitingClearConfirm) {
        AnchorAlert(
            title = "确认清空全部数据？",
            message = "打卡、破戒、渴求和问卷记录都会被删除，且无法恢复；提醒时间与提示语会保留。",
            confirmLabel = "清空",
            onConfirm = viewModel::confirmClear,
            dismissLabel = "取消",
            onDismiss = viewModel::cancelClear,
            destructive = true,
        )
    }
}

private const val EXPORT_MIME = "application/json"
private const val STEP_MINUTES = 5

/** 导入文件上限 4 MB：导出文件是纯文本记录，超过这个量级基本是选错了文件。 */
private const val MAX_IMPORT_BYTES = 4L * 1024 * 1024

/** 读选中的文件为文本；读不出来或过大都返回 null，由 ViewModel 统一提示。 */
private fun readImportText(context: Context, uri: Uri): String? = runCatching {
    val size = context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L
    if (size > MAX_IMPORT_BYTES) null else context.contentResolver.openInputStream(uri)
        ?.use { it.readBytes().toString(Charsets.UTF_8) }
}.getOrNull()

/** 系统给的多是 `primary:Download/xxx.json`，只留最后的文件名给人看。 */
private fun importFileName(lastPathSegment: String?): String =
    lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':').orEmpty()
        .ifBlank { "所选文件" }

/** `2024-05-04T09:31:00Z` → `2024-05-04 09:31`；解析不了就原样显示。 */
private fun humanReadableTime(exportedAt: String): String =
    exportedAt.take(16).replace('T', ' ').ifBlank { "未知" }

/** Android 13 起 POST_NOTIFICATIONS 需要运行时授权；更低版本安装即授权。 */
private fun needsNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) != PackageManager.PERMISSION_GRANTED
