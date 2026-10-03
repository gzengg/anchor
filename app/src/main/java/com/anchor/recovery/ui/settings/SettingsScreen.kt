package com.anchor.recovery.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.anchor.recovery.AppInfo
import com.anchor.recovery.R
import com.anchor.recovery.core.legal.Disclaimer
import com.anchor.recovery.notify.ExactReminderScheduler
import com.anchor.recovery.notify.ReminderTexts
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

    // 非 @Composable 的帮助函数与回调取不到 stringResource，统一在这里解析后传进去。
    val cannotWriteError = stringResource(R.string.settings_error_cannot_write)
    val unknownError = stringResource(R.string.settings_error_unknown)
    val unknownFileName = stringResource(R.string.settings_file_unknown_name)
    val unknownTime = stringResource(R.string.settings_time_unknown)

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(EXPORT_MIME),
    ) { uri ->
        val json = state.pendingExportJson
        if (uri == null || json == null) {
            viewModel.onExportCancelled()
        } else {
            runCatching {
                val stream = context.contentResolver.openOutputStream(uri)
                    ?: error(cannotWriteError)
                stream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            }.onSuccess { viewModel.onExportWritten() }
                .onFailure { viewModel.onExportFailed(it.message ?: unknownError) }
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

    // 从系统「闹钟与提醒」页面回来：能排上就存 true，否则当场把开关拨回去，
    // 不让界面停在「准点提醒」而实际没有精确排程。
    val exactAlarmLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        if (ExactReminderScheduler.canScheduleExact(context)) {
            viewModel.setExactReminderEnabled(true)
        } else {
            viewModel.onExactReminderDenied()
        }
    }

    // 个别机型的系统设置里没有这个页面（ActivityNotFound）：那就直接当作没授权。
    fun requestExactAlarmPermission() {
        runCatching {
            exactAlarmLauncher.launch(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    .setData(Uri.fromParts("package", context.packageName, null)),
            )
        }.onFailure { viewModel.onExactReminderDenied() }
    }

    // 不限定 MIME：部分文件管理器把 .json 报成 octet-stream，限定后反而选不中。
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) {
            viewModel.cancelImport()
            return@rememberLauncherForActivityResult
        }
        val fileName = importFileName(uri.lastPathSegment, unknownFileName)
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
        AnchorLargeTitle(stringResource(R.string.settings_title))

        AnchorSectionHeader(stringResource(R.string.settings_section_reminder))
        AnchorListGroup {
            AnchorSwitchRow(
                title = stringResource(R.string.settings_reminder_switch),
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
                        text = stringResource(R.string.settings_shift_minus),
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
                        text = stringResource(R.string.settings_shift_plus),
                        onClick = { viewModel.shiftReminderTime(STEP_MINUTES) },
                        style = AnchorButtonStyle.Tinted,
                    )
                }
                AnchorHairline(inset = 0.dp)
                AnchorSwitchRow(
                    title = stringResource(R.string.settings_exact_reminder),
                    subtitle = stringResource(R.string.settings_exact_reminder_subtitle),
                    checked = state.exactReminderEnabled,
                    onCheckedChange = { checked ->
                        if (checked && !ExactReminderScheduler.canScheduleExact(context)) {
                            requestExactAlarmPermission()
                        } else {
                            viewModel.setExactReminderEnabled(checked)
                        }
                    },
                )
            }
            AnchorHairline(inset = 0.dp)
            Text(
                // 提前把锁屏可见的文案展示出来：提醒不含任何用途描述。
                text = stringResource(
                    R.string.settings_notification_preview,
                    ReminderTexts.title(context),
                    ReminderTexts.body(context, 0),
                ),
                style = AnchorType.footnote,
                color = AnchorTheme.colors.labelSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }

        AnchorSectionHeader(stringResource(R.string.settings_section_prompts))
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
                    label = { Text(text = stringResource(R.string.settings_prompts_hint)) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                AnchorButton(
                    text = stringResource(R.string.settings_save_prompts),
                    onClick = viewModel::savePrompts,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        AnchorSectionHeader(stringResource(R.string.settings_section_export))
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
                    text = stringResource(R.string.settings_export_json),
                    onClick = viewModel::prepareExport,
                    enabled = state.hasAnyRecord,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.settings_export_note),
                    style = AnchorType.footnote,
                    color = AnchorTheme.colors.labelSecondary,
                )
            }
        }

        AnchorSectionHeader(stringResource(R.string.settings_section_import))
        AnchorListGroup {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_import_note),
                    style = AnchorType.body,
                )
                AnchorButton(
                    text = stringResource(R.string.settings_import_from_file),
                    onClick = { importLauncher.launch(arrayOf("*/*")) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.settings_import_scope_note),
                    style = AnchorType.footnote,
                    color = AnchorTheme.colors.labelSecondary,
                )
            }
        }

        AnchorSectionHeader(stringResource(R.string.settings_section_clear))
        AnchorListGroup {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_clear_note),
                    style = AnchorType.body,
                )
                AnchorButton(
                    text = stringResource(R.string.settings_clear_all),
                    onClick = viewModel::requestClear,
                    enabled = state.hasAnyRecord,
                    destructive = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        AnchorSectionHeader(stringResource(R.string.settings_section_privacy))
        AnchorListGroup {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = Disclaimer.PRIVACY_PARAGRAPH, style = AnchorType.body)
                Text(
                    text = stringResource(R.string.settings_privacy_note),
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
                    text = stringResource(
                        R.string.settings_version_line,
                        AppInfo.DISPLAY_NAME,
                        AppInfo.VERSION,
                        state.disclaimerVersion,
                    ),
                    style = AnchorType.footnote,
                    color = AnchorTheme.colors.labelSecondary,
                )
            }
            AnchorHairline(inset = 0.dp)
            AnchorButton(
                text = if (showFullDisclaimer) {
                    stringResource(R.string.settings_hide_full_disclaimer)
                } else {
                    stringResource(R.string.settings_show_full_disclaimer)
                },
                onClick = { showFullDisclaimer = !showFullDisclaimer },
                style = AnchorButtonStyle.Plain,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        state.messageRes?.let { res ->
            Text(
                text = stringResource(res, *state.messageArgs.toTypedArray()),
                style = AnchorType.footnote,
                color = AnchorTheme.colors.tint,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
            )
        }
    }

    state.pendingImport?.let { pending ->
        AnchorAlert(
            title = stringResource(R.string.settings_import_confirm_title),
            message = stringResource(
                R.string.settings_import_confirm_message,
                pending.fileName,
                humanReadableTime(pending.exportedAt, unknownTime),
                pending.countsLine,
            ),
            confirmLabel = stringResource(R.string.settings_import_confirm_action),
            onConfirm = viewModel::confirmImport,
            dismissLabel = stringResource(R.string.settings_cancel),
            onDismiss = viewModel::cancelImport,
        )
    }

    if (state.awaitingClearConfirm) {
        AnchorAlert(
            title = stringResource(R.string.settings_clear_confirm_title),
            message = stringResource(R.string.settings_clear_confirm_message),
            confirmLabel = stringResource(R.string.settings_clear_confirm_action),
            onConfirm = viewModel::confirmClear,
            dismissLabel = stringResource(R.string.settings_cancel),
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
private fun importFileName(lastPathSegment: String?, fallback: String): String =
    lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':').orEmpty()
        .ifBlank { fallback }

/** `2024-05-04T09:31:00Z` → `2024-05-04 09:31`；解析不了就原样显示。 */
private fun humanReadableTime(exportedAt: String, fallback: String): String =
    exportedAt.take(16).replace('T', ' ').ifBlank { fallback }

/** Android 13 起 POST_NOTIFICATIONS 需要运行时授权；更低版本安装即授权。 */
private fun needsNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) != PackageManager.PERMISSION_GRANTED
