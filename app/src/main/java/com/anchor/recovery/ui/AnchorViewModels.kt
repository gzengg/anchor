package com.anchor.recovery.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.anchor.recovery.core.model.CheckInRecord
import com.anchor.recovery.core.model.RelapseRecord
import com.anchor.recovery.core.model.UrgeEpisodeRecord
import com.anchor.recovery.core.relapse.RelapseInsight
import com.anchor.recovery.core.relapse.TriggerAnalyzer
import com.anchor.recovery.core.streak.CheckInDecision
import com.anchor.recovery.core.streak.RebootFramework
import com.anchor.recovery.core.streak.RebootMilestone
import com.anchor.recovery.core.streak.StreakState
import com.anchor.recovery.data.repo.AnchorRepository
import com.anchor.recovery.data.settings.AnchorSettings
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
data class HomeUiState(
    val streak: StreakState = StreakState.EMPTY,
    val nextMilestone: RebootMilestone? = null,
    val progress: Float = 0f,
    val message: String? = null,
)

class HomeViewModel(private val repository: AnchorRepository) : ViewModel() {

    private val message = MutableStateFlow<String?>(null)

    val state: StateFlow<HomeUiState> =
        combine(repository.streak, message) { streak, message ->
            HomeUiState(
                streak = streak,
                nextMilestone = RebootFramework.next(streak.currentDays),
                progress = RebootFramework.progress(streak.currentDays),
                message = message,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun checkInToday() {
        viewModelScope.launch {
            message.value = when (val decision = repository.checkInToday()) {
                is CheckInDecision.Allowed -> "已记录今天。记录本身就是有效动作。"
                is CheckInDecision.AlreadyCheckedIn -> "今天已经记录过了。"
                is CheckInDecision.Rejected -> decision.reason
            }
        }
    }

    fun dismissMessage() {
        message.value = null
    }
}

enum class TimelineFilter(val label: String) {
    ALL("全部"),
    CHECK_IN("打卡"),
    URGE("渴求"),
    RELAPSE("复吸"),
}

/** 日志页的一条时间线条目（打卡 / 渴求事件 / 复吸）。 */
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
    val message: String? = null,
)

class CheckInViewModel(private val repository: AnchorRepository) : ViewModel() {

    private val note = MutableStateFlow("")
    private val message = MutableStateFlow<String?>(null)

    val state: StateFlow<CheckInUiState> = combine(
        repository.streak,
        repository.checkIns,
        note,
        message,
    ) { streak, checkIns, note, message ->
        CheckInUiState(
            today = repository.today(),
            todayCheckedIn = streak.todayCheckedIn,
            note = note,
            recent = checkIns.sortedByDescending { it.date }.take(RECENT_LIMIT),
            message = message,
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
                    "已记录今天。"
                }

                is CheckInDecision.AlreadyCheckedIn -> "今天已经记录过了。"
                is CheckInDecision.Rejected -> decision.reason
            }
        }
    }

    fun undoToday() {
        viewModelScope.launch {
            repository.removeCheckIn(repository.today())
            message.value = "已撤销今天的打卡。"
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
    initializer { TimelineViewModel(repository) }
    initializer { UrgeSurfingViewModel(repository) }
    initializer { DelayToolViewModel(repository, settings) }
    initializer { RelapseEditViewModel(repository) }
}
