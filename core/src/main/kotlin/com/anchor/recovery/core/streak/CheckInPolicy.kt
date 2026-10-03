package com.anchor.recovery.core.streak

import com.anchor.recovery.core.clock.Clock
import kotlinx.datetime.LocalDate

sealed interface CheckInDecision {
    /** 允许打卡，落库日期固定为 [date]（今天）。 */
    data class Allowed(val date: LocalDate) : CheckInDecision

    /** 今天已经打过卡，无需重复插入（date 唯一索引）。 */
    data class AlreadyCheckedIn(val date: LocalDate) : CheckInDecision

    /** 不允许打卡，[reason] 由界面按资源渲染（`:app` 的 `checkInRejectionText`）。 */
    data class Rejected(val date: LocalDate, val reason: CheckInRejection) : CheckInDecision
}

/** 不允许打卡的原因码；展示文案在 `:app`，core 不持有中文文案。 */
enum class CheckInRejection {
    /** 目标日期在未来。 */
    FUTURE_DATE,

    /** 目标日期不是今天（不支持补打卡）。 */
    NOT_TODAY,
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
                CheckInDecision.Rejected(target, CheckInRejection.FUTURE_DATE)

            target.toEpochDays() < today.toEpochDays() ->
                CheckInDecision.Rejected(target, CheckInRejection.NOT_TODAY)

            existingDates.contains(today) -> CheckInDecision.AlreadyCheckedIn(today)

            else -> CheckInDecision.Allowed(today)
        }
    }
}
