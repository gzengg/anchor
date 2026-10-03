package com.anchor.recovery.ui.text

import androidx.annotation.StringRes
import com.anchor.recovery.R
import com.anchor.recovery.core.export.ImportRejection
import com.anchor.recovery.core.streak.CheckInRejection

/**
 * `:core` 原因码 / 天数 → 展示文案资源的映射。
 *
 * `:core` 不持有中文文案（它连 `android.*` 都不能依赖），只给出可判定的原因码；
 * 文案在 `values/strings_core.xml`。这里的 `when` 不写 `else`：`:core` 新增原因码时
 * 编译期就会失败，避免出现「有状态没文案」。
 */

@StringRes
fun milestoneTitleRes(days: Int): Int = when (days) {
    1 -> R.string.milestone_days_title_1
    7 -> R.string.milestone_days_title_7
    30 -> R.string.milestone_days_title_30
    60 -> R.string.milestone_days_title_60
    90 -> R.string.milestone_days_title_90
    else -> R.string.milestone_days_title_unknown
}

@StringRes
fun checkInRejectionRes(reason: CheckInRejection): Int = when (reason) {
    CheckInRejection.FUTURE_DATE -> R.string.checkin_reject_future_date
    CheckInRejection.NOT_TODAY -> R.string.checkin_reject_not_today
}

@StringRes
fun importRejectionRes(reason: ImportRejection): Int = when (reason) {
    ImportRejection.EMPTY_FILE -> R.string.import_reject_empty_file
    ImportRejection.NOT_ANCHOR_FILE -> R.string.import_reject_not_anchor_file
    ImportRejection.MISSING_VERSION -> R.string.import_reject_missing_version
    ImportRejection.UNSUPPORTED_VERSION -> R.string.import_reject_unsupported_version
    ImportRejection.PARSE_ERROR -> R.string.import_reject_parse_error
    ImportRejection.UNKNOWN_PARSE_ERROR -> R.string.import_reject_unknown_parse_error
    ImportRejection.DUPLICATE_DATES -> R.string.import_reject_duplicate_dates
}
