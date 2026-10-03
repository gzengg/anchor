package com.anchor.recovery.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anchor.recovery.core.export.DataExporter
import com.anchor.recovery.core.export.DataImporter
import com.anchor.recovery.core.export.ImportResult
import com.anchor.recovery.core.export.ImportedData
import com.anchor.recovery.core.legal.Disclaimer
import com.anchor.recovery.core.notify.ReminderTime
import com.anchor.recovery.data.repo.AnchorRepository
import com.anchor.recovery.data.settings.AnchorSettings
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
    val message: String? = null,
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
    private val message = MutableStateFlow<String?>(null)

    val state: StateFlow<SettingsUiState> = combine(
        settings.snapshot,
        repository.checkIns,
        repository.relapses,
        repository.urgeEpisodes,
        repository.assessments,
    ) { snapshot, checkIns, relapses, urges, assessments ->
        SettingsUiState(
            reminderEnabled = snapshot.reminderEnabled,
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
    }.combine(message) { base, text ->
        base.copy(message = text)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settings.setReminderEnabled(enabled)
            if (!enabled) {
                message.value = "已关闭每日提醒。"
            }
        }
    }

    fun onPermissionDenied() {
        message.value = "没有通知权限，提醒无法送达。可稍后在系统设置里为磐石打开通知。"
    }

    fun shiftReminderTime(minutes: Int) {
        val next = (state.value.reminderTime).shiftMinutes(minutes)
        viewModelScope.launch {
            settings.setReminderTime(next.toString())
            if (state.value.reminderEnabled) {
                message.value = "提醒时间已改为 ${next}。"
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
            message.value = "已保存提示语。"
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
            message.value = "读不到这个文件，请换一个再试。"
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

            is ImportResult.Rejected -> message.value = result.reason
        }
    }

    fun cancelImport() {
        pendingImport.value = null
        backupBeforeImport.value = false
        message.value = "已取消导入。"
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
                    message.value = "已导入：${pending.data.countsLine()}。原来的记录已被这份文件替换。"
                },
                onFailure = { error ->
                    message.value = "导入失败：${error.message ?: "文件内容有冲突"}。你原来的记录没有被改动。"
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
        message.value = "已导出。请把文件保存到你信任的位置。"
    }

    fun onExportCancelled() {
        pendingExport.value = null
        if (backupBeforeImport.value) {
            backupBeforeImport.value = false
            message.value = "没有备份成功，导入已取消。你原来的记录没有变动。"
            return
        }
        message.value = "已取消导出。"
    }

    fun onExportFailed(reason: String) {
        pendingExport.value = null
        message.value = "导出失败：$reason"
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
            message.value = "已清空全部记录。此操作无法撤销。"
        }
    }

    fun dismissMessage() {
        message.value = null
    }
}
