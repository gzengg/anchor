package com.anchor.recovery.ui.milestones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anchor.recovery.core.streak.MilestoneStatus
import com.anchor.recovery.data.repo.AnchorRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * 徽章墙 ViewModel：状态全部来自 Repository（当前历史推导 ∪ 已落库达成记录，见 `AnchorRepository.milestoneWall`）。
 *
 * 初值用空列表：首帧渲染为「未点亮」占位，数据到达后立即替换，
 * 避免先显示一屏错误的「已达成」。
 */
class MilestoneWallViewModel(repository: AnchorRepository) : ViewModel() {

    val wall: StateFlow<List<MilestoneStatus>> = repository.milestoneWall
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
