package com.anchor.recovery.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.anchor.recovery.core.streak.CheckInDecision
import com.anchor.recovery.core.streak.RebootFramework
import com.anchor.recovery.core.streak.RebootMilestone
import com.anchor.recovery.core.streak.StreakState
import com.anchor.recovery.data.repo.AnchorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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

fun anchorViewModelFactory(repository: AnchorRepository) = viewModelFactory {
    initializer { HomeViewModel(repository) }
    initializer { CheckInViewModel(repository) }
}
