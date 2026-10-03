package com.anchor.recovery.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.anchor.recovery.AppInfo
import com.anchor.recovery.R
import com.anchor.recovery.core.model.CheckInRecord
import com.anchor.recovery.core.model.RelapseRecord
import com.anchor.recovery.core.model.UrgeEpisodeRecord
import com.anchor.recovery.core.relapse.RelapseInsight
import com.anchor.recovery.core.relapse.TriggerAnalyzer
import com.anchor.recovery.core.streak.CheckInDecision
import com.anchor.recovery.core.streak.RebootFramework
import com.anchor.recovery.core.streak.RebootMilestone
import com.anchor.recovery.core.streak.StreakState
import com.anchor.recovery.ui.text.checkInRejectionRes
import com.anchor.recovery.data.repo.AnchorRepository
import com.anchor.recovery.data.settings.AnchorSettings
import com.anchor.recovery.ui.assessment.AssessmentHubViewModel
import com.anchor.recovery.ui.assessment.CsbdQuizViewModel
import com.anchor.recovery.ui.assessment.CsbdResultViewModel
import com.anchor.recovery.ui.assessment.MoralQuizViewModel
import com.anchor.recovery.ui.assessment.MoralResultViewModel
import com.anchor.recovery.ui.milestones.MilestoneWallViewModel
import com.anchor.recovery.ui.settings.SettingsViewModel
import com.anchor.recovery.ui.tools.DelayToolViewModel
import com.anchor.recovery.ui.tools.RelapseEditViewModel
import com.anchor.recovery.ui.tools.UrgeSurfingViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/**
 * ViewModel 只做状态转发：所有业务判断都在 :core 与 Repository。
 */
/** ViewModel 侧待展示的提示：资源 id + 格式化参数，切语言时由界面重新解析。 */
private data class UiMessage(
    @StringRes val res: Int,
    val args: List<Any> = emptyList(),
)

data class HomeUiState(
    val streak: StreakState = StreakState.EMPTY,
    val nextMilestone: RebootMilestone? = null,
    val progress: Float = 0f,
    @StringRes val messageRes: Int? = null,
    val messageArgs: List<Any> = emptyList(),
)

class HomeViewModel(private val repository: AnchorRepository) : ViewModel() {

    private val message = MutableStateFlow<UiMessage?>(null)

    val state: StateFlow<HomeUiState> =
        combine(repository.streak, message) { streak, pending ->
            HomeUiState(
                streak = streak,
                nextMilestone = RebootFramework.next(streak.currentDays),
                progress = RebootFramework.progress(streak.currentDays),
                messageRes = pending?.res,
                messageArgs = pending?.args.orEmpty(),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun checkInToday() {
        viewModelScope.launch {
            message.value = when (val decision = repository.checkInToday()) {
                is CheckInDecision.Allowed -> UiMessage(R.string.msg_check_in_recorded)
                is CheckInDecision.AlreadyCheckedIn -> UiMessage(R.string.msg_check_in_duplicate)
                is CheckInDecision.Rejected -> UiMessage(checkInRejectionRes(decision.reason))
            }
        }
    }

    fun dismissMessage() {
        message.value = null
    }
}

enum class TimelineFilter(@StringRes val label: Int) {
    ALL(R.string.journal_filter_all),
    CHECK_IN(R.string.journal_filter_check_in),
    URGE(R.string.journal_filter_urge),
    RELAPSE(R.string.journal_filter_relapse),
}

/** 日志页的一条时间线条目（打卡 / 渴求事件 / 破戒）。 */
sealed interface TimelineEntry {
    val at: Instant

    data class CheckIn(val record: CheckInRecord) : TimelineEntry {
        override val at: Instant get() = record.createdAt
    }

    data class Urge(val record: UrgeEpisodeRecord) : TimelineEntry {
        override val at: Instant get() = record.startedAt
    }

    data class Relapse(val record: RelapseRecord) : TimelineEntry {
        override val at: Instant get() = record.occurredAt
    }
}

private fun TimelineFilter.matches(entry: TimelineEntry): Boolean = when (this) {
    TimelineFilter.ALL -> true
    TimelineFilter.CHECK_IN -> entry is TimelineEntry.CheckIn
    TimelineFilter.URGE -> entry is TimelineEntry.Urge
    TimelineFilter.RELAPSE -> entry is TimelineEntry.Relapse
}

data class TimelineUiState(
    val entries: List<TimelineEntry> = emptyList(),
    val filter: TimelineFilter = TimelineFilter.ALL,
    val totalCount: Int = 0,
    val checkInCount: Int = 0,
    val urgeCount: Int = 0,
    val relapseCount: Int = 0,
    /** F5 统计卡：情绪/触发源 Top-N、时段分布、复吸间隔中位数。 */
    val insight: RelapseInsight = RelapseInsight(),
)

/** 把三张表合并成一条按时间倒序的日志，并顺带算出复吸统计。 */
class TimelineViewModel(private val repository: AnchorRepository) : ViewModel() {

    private val filter = MutableStateFlow(TimelineFilter.ALL)
    private val triggerAnalyzer = TriggerAnalyzer(repository.clock)

    val state: StateFlow<TimelineUiState> = combine(
        repository.checkIns,
        repository.relapses,
        repository.urgeEpisodes,
        filter,
    ) { checkIns, relapses, urges, filter ->
        val all = buildList {
            checkIns.forEach { add(TimelineEntry.CheckIn(it)) }
            relapses.forEach { add(TimelineEntry.Relapse(it)) }
            urges.forEach { add(TimelineEntry.Urge(it)) }
        }.sortedByDescending { it.at }
        TimelineUiState(
            entries = all.filter { filter.matches(it) },
            filter = filter,
            totalCount = all.size,
            checkInCount = checkIns.size,
            urgeCount = urges.size,
            relapseCount = relapses.size,
            insight = triggerAnalyzer.analyze(relapses),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimelineUiState())

    fun selectFilter(value: TimelineFilter) {
        filter.value = value
    }
}

data class CheckInUiState(
    val today: LocalDate,
    val todayCheckedIn: Boolean = false,
    val note: String = "",
    val recent: List<com.anchor.recovery.core.model.CheckInRecord> = emptyList(),
    @StringRes val messageRes: Int? = null,
    val messageArgs: List<Any> = emptyList(),
)

class CheckInViewModel(private val repository: AnchorRepository) : ViewModel() {

    private val note = MutableStateFlow("")
    private val message = MutableStateFlow<UiMessage?>(null)

    val state: StateFlow<CheckInUiState> = combine(
        repository.streak,
        repository.checkIns,
        note,
        message,
    ) { streak, checkIns, note, pending ->
        CheckInUiState(
            today = repository.today(),
            todayCheckedIn = streak.todayCheckedIn,
            note = note,
            recent = checkIns.sortedByDescending { it.date }.take(RECENT_LIMIT),
            messageRes = pending?.res,
            messageArgs = pending?.args.orEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CheckInUiState(repository.today()))

    fun updateNote(value: String) {
        note.value = value
    }

    fun save() {
        viewModelScope.launch {
            message.value = when (val decision = repository.checkInToday(note.value)) {
                is CheckInDecision.Allowed -> {
                    note.value = ""
                    UiMessage(R.string.msg_check_in_recorded)
                }

                is CheckInDecision.AlreadyCheckedIn -> UiMessage(R.string.msg_check_in_duplicate)
                is CheckInDecision.Rejected -> UiMessage(checkInRejectionRes(decision.reason))
            }
        }
    }

    fun undoToday() {
        viewModelScope.launch {
            repository.removeCheckIn(repository.today())
            message.value = UiMessage(R.string.msg_check_in_undone)
        }
    }

    fun dismissMessage() {
        message.value = null
    }

    private companion object {
        const val RECENT_LIMIT = 14
    }
}

fun anchorViewModelFactory(repository: AnchorRepository, settings: AnchorSettings) = viewModelFactory {
    initializer { HomeViewModel(repository) }
    initializer { CheckInViewModel(repository) }
    initializer { MilestoneWallViewModel(repository) }
    initializer { TimelineViewModel(repository) }
    initializer { UrgeSurfingViewModel(repository) }
    initializer { DelayToolViewModel(repository, settings) }
    initializer { RelapseEditViewModel(repository) }
    initializer { AssessmentHubViewModel(repository) }
    initializer { CsbdQuizViewModel(repository) }
    initializer { CsbdResultViewModel(repository) }
    initializer { MoralQuizViewModel(repository) }
    initializer { MoralResultViewModel(repository) }
    initializer {
        SettingsViewModel(
            repository = repository,
            settings = settings,
            appName = AppInfo.DISPLAY_NAME,
            appVersion = AppInfo.VERSION,
        )
    }
}
