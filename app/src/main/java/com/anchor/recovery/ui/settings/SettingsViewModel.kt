package com.anchor.recovery.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anchor.recovery.core.export.DataExporter
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
    val disclaimerVersion: Int = Disclaimer.VERSION,
    val acknowledgedVersion: Int = 0,
    val message: String? = null,
)

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
        viewModelScope.launch {
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
            pendingExport.value = DataExporter.toJson(payload) to DataExporter.fileName(exportedAt)
        }
    }

    fun onExportWritten() {
        pendingExport.value = null
        message.value = "已导出。请把文件保存到你信任的位置。"
    }

    fun onExportCancelled() {
        pendingExport.value = null
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
