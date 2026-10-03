package com.anchor.recovery.ui.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anchor.recovery.core.model.RelapseRecord
import com.anchor.recovery.core.model.UrgeEpisodeRecord
import com.anchor.recovery.core.model.UrgeTool
import com.anchor.recovery.core.urge.DelaySession
import com.anchor.recovery.core.urge.DelayTool
import com.anchor.recovery.core.urge.UrgeSurfingResult
import com.anchor.recovery.data.repo.AnchorRepository
import com.anchor.recovery.data.settings.AnchorSettings
import com.anchor.recovery.data.settings.AnchorSettingsSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant

/**
 * F3/F4/F5 三个工具的 ViewModel。
 *
 * 约定：状态机与倒计时公式全在 `:core`（[DelayTool]、`UrgeSurfingSession`、`TriggerAnalyzer`），
 * 这里只做"取当前时间 + 落库"的转发，不重新实现任何计时逻辑。
 */
class UrgeSurfingViewModel(private val repository: AnchorRepository) : ViewModel() {

    /** 冲浪开始时刻（一律经 Clock 获取）。 */
    fun startedAt(): Instant = Instant.fromEpochMilliseconds(repository.nowMillis())

    /** 整轮结束后落库；中途放弃也算部分完成，同样记录。 */
    fun save(result: UrgeSurfingResult) {
        viewModelScope.launch {
            repository.recordUrgeEpisode(
                UrgeEpisodeRecord(
                    startedAt = result.startedAt,
                    durationSec = result.durationSec,
                    peakIntensity = result.peakIntensity,
                    endIntensity = result.endIntensity,
                    tool = UrgeTool.URGE_SURFING,
                ),
            )
        }
    }
}

/** F4 界面需要的全部数值，每次都由 [DelayTool] 依当前时钟算出。 */
data class DelayUiState(
    val session: DelaySession? = null,
    val elapsedSec: Int = 0,
    val remainingSec: Int = 0,
    val remainingLabel: String = "10:00",
    val progress: Float = 0f,
    val completed: Boolean = false,
    val prompts: List<String> = AnchorSettingsSnapshot.DEFAULT_PROMPTS,
    val promptIndex: Int = 0,
    val saved: Boolean = false,
)

class DelayToolViewModel(
    private val repository: AnchorRepository,
    settings: AnchorSettings,
) : ViewModel() {

    private val tool = DelayTool(repository.clock)

    /** 用户在设置里写的「戒断理由/价值提示语」；没写时用默认三条。 */
    private val prompts: StateFlow<List<String>> = settings.snapshot
        .map { it.effectivePrompts }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AnchorSettingsSnapshot.DEFAULT_PROMPTS)

    private val currentSession = MutableStateFlow<DelaySession?>(null)
    private val recorded = MutableStateFlow(false)

    fun start(durationSec: Int = tool.defaultDurationSec) {
        recorded.value = false
        currentSession.value = tool.start(durationSec)
    }

    /** UI 每秒调一次（`LaunchedEffect` 里 `delay(1000)` 之后），拿到最新倒计时。 */
    fun state(): DelayUiState {
        val session = currentSession.value ?: return DelayUiState(prompts = prompts.value)
        val elapsedSec = tool.elapsedSec(session)
        return DelayUiState(
            session = session,
            elapsedSec = elapsedSec,
            remainingSec = tool.remainingSec(session),
            remainingLabel = tool.remainingLabel(session),
            progress = tool.progress(session),
            completed = tool.isComplete(session),
            prompts = prompts.value,
            promptIndex = tool.promptIndex(elapsedSec, prompts.value.size),
            saved = recorded.value,
        )
    }

    /** 用户回答"渴求是否过去"后落库：[endIntensity] 由答案与滑条共同决定。 */
    fun recordOutcome(initialIntensity: Int, endIntensity: Int) {
        val session = currentSession.value ?: return
        viewModelScope.launch {
            repository.recordUrgeEpisode(
                UrgeEpisodeRecord(
                    startedAt = session.startedAt,
                    durationSec = tool.elapsedSec(session),
                    peakIntensity = maxOf(initialIntensity, endIntensity),
                    endIntensity = endIntensity,
                    tool = UrgeTool.DELAY_TOOL,
                ),
            )
            recorded.value = true
        }
    }

    fun reset() {
        currentSession.value = null
        recorded.value = false
    }
}

/** F5 复吸记录表单。 */
data class RelapseFormState(
    val situation: String = "",
    val emotions: Set<String> = emptySet(),
    val triggers: Set<String> = emptySet(),
    val note: String = "",
    val saved: Boolean = false,
    val message: String? = null,
) {
    /** 一无所填时不允许保存，避免产生无信息量的记录。 */
    val canSave: Boolean
        get() = emotions.isNotEmpty() || triggers.isNotEmpty() ||
            situation.isNotBlank() || note.isNotBlank()
}

class RelapseEditViewModel(private val repository: AnchorRepository) : ViewModel() {

    private val mutable = MutableStateFlow(RelapseFormState())
    val state: StateFlow<RelapseFormState> = mutable.asStateFlow()

    fun updateSituation(value: String) {
        mutable.value = mutable.value.copy(situation = value)
    }

    fun updateNote(value: String) {
        mutable.value = mutable.value.copy(note = value)
    }

    fun toggleEmotion(value: String) {
        mutable.value = mutable.value.copy(emotions = mutable.value.emotions.toggle(value))
    }

    fun toggleTrigger(value: String) {
        mutable.value = mutable.value.copy(triggers = mutable.value.triggers.toggle(value))
    }

    fun save() {
        val current = mutable.value
        if (!current.canSave) {
            mutable.value = current.copy(message = "至少填一项：情境、情绪、触发源或备注。")
            return
        }
        viewModelScope.launch {
            repository.recordRelapse(
                RelapseRecord(
                    occurredAt = Instant.fromEpochMilliseconds(repository.nowMillis()),
                    situation = current.situation.trim(),
                    emotions = current.emotions.toList(),
                    triggers = current.triggers.toList(),
                    note = current.note.trim(),
                ),
            )
            mutable.value = RelapseFormState(
                saved = true,
                message = "已记录。写下它的价值是让你看清规律，而不是给自己下判断。",
            )
        }
    }

    fun dismissMessage() {
        mutable.value = mutable.value.copy(message = null)
    }

    private fun Set<String>.toggle(value: String): Set<String> =
        if (value in this) this - value else this + value
}
