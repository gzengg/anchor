package com.anchor.recovery.core.streak

import com.anchor.recovery.core.clock.Clock
import kotlinx.datetime.LocalDate

sealed interface CheckInDecision {
    /** 允许打卡，落库日期固定为 [date]（今天）。 */
    data class Allowed(val date: LocalDate) : CheckInDecision

    /** 今天已经打过卡，无需重复插入（date 唯一索引）。 */
    data class AlreadyCheckedIn(val date: LocalDate) : CheckInDecision

    /** 不允许打卡，[reason] 用于直接展示给用户。 */
    data class Rejected(val date: LocalDate, val reason: String) : CheckInDecision
}

/**
 * 打卡策略：本 App 只允许为**今天**打卡。
 *
 * 不提供补打卡：产品口径是「漏打一整天即断」，允许回填会让连续天数的语义失真。
 */
class CheckInPolicy(private val clock: Clock) {

    fun evaluate(
        existingDates: Collection<LocalDate>,
        requestedDate: LocalDate? = null,
    ): CheckInDecision {
        val today = clock.today()
        val target = requestedDate ?: today
        return when {
            target.toEpochDays() > today.toEpochDays() ->
                CheckInDecision.Rejected(target, "不能为未来日期打卡")

            target.toEpochDays() < today.toEpochDays() ->
                CheckInDecision.Rejected(target, "只能为今天打卡，不支持补打卡")

            existingDates.contains(today) -> CheckInDecision.AlreadyCheckedIn(today)

            else -> CheckInDecision.Allowed(today)
        }
    }
}
