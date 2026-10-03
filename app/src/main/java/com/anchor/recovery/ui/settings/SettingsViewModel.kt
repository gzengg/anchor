package com.anchor.recovery.ui.settings

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anchor.recovery.R
import com.anchor.recovery.core.export.DataExporter
import com.anchor.recovery.core.export.DataImporter
import com.anchor.recovery.core.export.ImportResult
import com.anchor.recovery.core.export.ImportedData
import com.anchor.recovery.core.legal.Disclaimer
import com.anchor.recovery.core.notify.ReminderTime
import com.anchor.recovery.data.repo.AnchorRepository
import com.anchor.recovery.data.settings.AnchorSettings
import com.anchor.recovery.ui.text.importRejectionRes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 设置页状态：提醒、提示语、数据条数、导出与清空。 */
data class SettingsUiState(
    val reminderEnabled: Boolean = false,
    /** 用户是否要求准点提醒；系统未授权时界面会把开关拨回。 */
    val exactReminderEnabled: Boolean = false,
    val reminderTime: ReminderTime = ReminderTime.DEFAULT,
    val promptDraft: String = "",
    val countsLine: String = DataExporter.countsLine(0, 0, 0, 0),
    val hasAnyRecord: Boolean = false,
    /** 待用户选择保存位置的导出内容；非空时由界面弹出系统文件选择器。 */
    val pendingExportJson: String? = null,
    val pendingExportFileName: String = "",
    val awaitingClearConfirm: Boolean = false,
    /** 已解析待用户确认的导入文件；非空时界面弹确认框。 */
    val pendingImport: PendingImport? = null,
    /** 正在走「先备份当前数据、备份写盘后再导入」的流程。 */
    val importingAfterBackup: Boolean = false,
    val disclaimerVersion: Int = Disclaimer.VERSION,
    val acknowledgedVersion: Int = 0,
    @StringRes val messageRes: Int? = null,
    val messageArgs: List<Any> = emptyList(),
)

/** ViewModel 侧待展示的提示：资源 id + 格式化参数，切语言时由界面重新解析。 */
private data class SettingsMessage(
    @StringRes val res: Int,
    val args: List<Any> = emptyList(),
)

/** 待确认的导入内容：条数用于确认弹窗，文件名与导出时间帮用户认出这是哪一份文件。 */
data class PendingImport(
    val data: ImportedData,
    val exportedAt: String,
    val fileName: String,
) {
    val countsLine: String get() = data.countsLine()
}

/**
 * 设置页逻辑。导出内容由 :core 的 [DataExporter] 装配，这里只做状态转发与 DataStore 写入。
 */
class SettingsViewModel(
    private val repository: AnchorRepository,
    private val settings: AnchorSettings,
    private val appName: String,
    private val appVersion: String,
) : ViewModel() {

    private val promptDraft = MutableStateFlow<String?>(null)
    private val pendingExport = MutableStateFlow<Pair<String, String>?>(null)
    private val clearConfirm = MutableStateFlow(false)
    private val pendingImport = MutableStateFlow<PendingImport?>(null)
    private val backupBeforeImport = MutableStateFlow(false)
    private val message = MutableStateFlow<SettingsMessage?>(null)

    val state: StateFlow<SettingsUiState> = combine(
        settings.snapshot,
        repository.checkIns,
        repository.relapses,
        repository.urgeEpisodes,
        repository.assessments,
    ) { snapshot, checkIns, relapses, urges, assessments ->
        SettingsUiState(
            reminderEnabled = snapshot.reminderEnabled,
            exactReminderEnabled = snapshot.exactReminderEnabled,
            reminderTime = ReminderTime.parseOrNull(snapshot.reminderTime) ?: ReminderTime.DEFAULT,
            promptDraft = promptDraft.value ?: snapshot.effectivePrompts.joinToString("\n"),
            countsLine = DataExporter.countsLine(
                checkIns = checkIns.size,
                relapses = relapses.size,
                urgeEpisodes = urges.size,
                assessments = assessments.size,
            ),
            hasAnyRecord = checkIns.isNotEmpty() || relapses.isNotEmpty() ||
                urges.isNotEmpty() || assessments.isNotEmpty(),
            disclaimerVersion = Disclaimer.VERSION,
            acknowledgedVersion = snapshot.disclaimerAckVersion,
        )
    }.combine(pendingExport) { base, export ->
        base.copy(
            pendingExportJson = export?.first,
            pendingExportFileName = export?.second ?: base.pendingExportFileName,
        )
    }.combine(clearConfirm) { base, confirm ->
        base.copy(awaitingClearConfirm = confirm)
    }.combine(pendingImport) { base, import ->
        base.copy(pendingImport = import)
    }.combine(backupBeforeImport) { base, backing ->
        base.copy(importingAfterBackup = backing)
    }.combine(message) { base, pending ->
        base.copy(
            messageRes = pending?.res,
            messageArgs = pending?.args.orEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settings.setReminderEnabled(enabled)
            if (!enabled) {
                message.value = SettingsMessage(R.string.settings_message_reminder_off)
            }
        }
    }

    fun onPermissionDenied() {
        message.value = SettingsMessage(R.string.settings_message_permission_denied)
    }

    /**
     * 准点提醒开关。只写设置，不动排程：排程的唯一同步点是 MainActivity 里对设置的收集
     * （`ReminderScheduler.sync`），设置一变就重排，避免这里和那里各算一次策略、算出不同结果。
     */
    fun setExactReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settings.setExactReminderEnabled(enabled)
            message.value = SettingsMessage(
                if (enabled) R.string.settings_message_exact_on else R.string.settings_message_exact_off,
            )
        }
    }

    /** 系统没给「闹钟与提醒」权限（或用户在系统页面里没打开）：把开关拨回去，界面与实际一致。 */
    fun onExactReminderDenied() {
        viewModelScope.launch {
            settings.setExactReminderEnabled(false)
            message.value = SettingsMessage(R.string.settings_message_exact_denied)
        }
    }

    fun shiftReminderTime(minutes: Int) {
        val next = (state.value.reminderTime).shiftMinutes(minutes)
        viewModelScope.launch {
            settings.setReminderTime(next.toString())
            if (state.value.reminderEnabled) {
                message.value = SettingsMessage(
                    R.string.settings_message_reminder_time_changed,
                    listOf(next),
                )
            }
        }
    }

    fun updatePromptDraft(value: String) {
        promptDraft.value = value
    }

    fun savePrompts() {
        val text = promptDraft.value ?: return
        viewModelScope.launch {
            settings.setMotivationPrompts(text.split("\n"))
            promptDraft.value = null
            message.value = SettingsMessage(R.string.settings_message_prompts_saved)
        }
    }

    /** 装配导出 JSON，交给界面弹出系统文件选择器。 */
    fun prepareExport() {
        viewModelScope.launch { pendingExport.value = buildExport() }
    }

    private suspend fun buildExport(): Pair<String, String> {
        val snapshot = settings.snapshot.first()
        val exportedAt = repository.clock.now()
        val payload = DataExporter.build(
            appName = appName,
            appVersion = appVersion,
            exportedAt = exportedAt,
            checkIns = repository.checkIns.first(),
            relapses = repository.relapses.first(),
            urgeEpisodes = repository.urgeEpisodes.first(),
            assessments = repository.assessments.first(),
            reminderEnabled = snapshot.reminderEnabled,
            reminderTime = snapshot.reminderTime,
            motivationPrompts = snapshot.effectivePrompts,
        )
        return DataExporter.toJson(payload) to DataExporter.fileName(exportedAt)
    }

    /** 用户在系统文件选择器里选好文件后调用；[text] 为 null 表示文件读不出来。 */
    fun onImportFilePicked(fileName: String, text: String?) {
        if (text == null) {
            message.value = SettingsMessage(R.string.settings_message_import_unreadable)
            return
        }
        when (val result = DataImporter.parse(text)) {
            is ImportResult.Ready -> {
                pendingImport.value = PendingImport(
                    data = result.data,
                    exportedAt = result.exportedAt,
                    fileName = fileName,
                )
                message.value = null
            }

            is ImportResult.Rejected -> message.value = SettingsMessage(
                importRejectionRes(result.reason),
                result.args,
            )
        }
    }

    fun cancelImport() {
        pendingImport.value = null
        backupBeforeImport.value = false
        message.value = SettingsMessage(R.string.settings_message_import_cancelled)
    }

    /**
     * 确认导入：先强制导出当前数据做备份，备份写盘成功后才真正替换。
     * 整体替换会丢掉现有记录，所以不给「跳过备份」的选项。
     */
    fun confirmImport() {
        if (pendingImport.value == null) return
        viewModelScope.launch {
            backupBeforeImport.value = true
            pendingExport.value = buildExport()
        }
    }

    private fun applyImport() {
        val pending = pendingImport.value ?: return
        viewModelScope.launch {
            val outcome = runCatching { repository.replaceAll(pending.data) }
            backupBeforeImport.value = false
            pendingImport.value = null
            outcome.fold(
                onSuccess = {
                    promptDraft.value = null
                    settings.setReminderEnabled(pending.data.reminderEnabled)
                    settings.setReminderTime(pending.data.reminderTime)
                    settings.setMotivationPrompts(pending.data.motivationPrompts)
                    message.value = SettingsMessage(
                        R.string.settings_message_import_done,
                        listOf(pending.data.countsLine()),
                    )
                },
                onFailure = { error ->
                    val reason = error.message
                    message.value = if (reason == null) {
                        SettingsMessage(R.string.settings_message_import_failed_no_reason)
                    } else {
                        SettingsMessage(R.string.settings_message_import_failed, listOf(reason))
                    }
                },
            )
        }
    }

    fun onExportWritten() {
        pendingExport.value = null
        if (backupBeforeImport.value) {
            applyImport()
            return
        }
        message.value = SettingsMessage(R.string.settings_message_export_done)
    }

    fun onExportCancelled() {
        pendingExport.value = null
        if (backupBeforeImport.value) {
            backupBeforeImport.value = false
            message.value = SettingsMessage(R.string.settings_message_backup_failed)
            return
        }
        message.value = SettingsMessage(R.string.settings_message_export_cancelled)
    }

    fun onExportFailed(reason: String) {
        pendingExport.value = null
        message.value = SettingsMessage(R.string.settings_message_export_failed, listOf(reason))
    }

    fun requestClear() {
        clearConfirm.value = true
    }

    fun cancelClear() {
        clearConfirm.value = false
    }

    fun confirmClear() {
        viewModelScope.launch {
            repository.clearAll()
            clearConfirm.value = false
            message.value = SettingsMessage(R.string.settings_message_cleared)
        }
    }

    fun dismissMessage() {
        message.value = null
    }
}
