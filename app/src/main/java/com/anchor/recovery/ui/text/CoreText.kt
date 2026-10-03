package com.anchor.recovery.ui.text

import androidx.annotation.StringRes
import com.anchor.recovery.R
import com.anchor.recovery.core.export.ImportField
import com.anchor.recovery.core.export.ImportRejection
import com.anchor.recovery.core.export.ImportResult
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

/**
 * 导入被拒的展示文案与格式化参数（资源 id + 参数，界面直接 `stringResource`）。
 *
 * 时间字段解析失败的参数里带的是 [ImportField] 枚举，这里就换成字段专属的整句，
 * 界面不参与拼文案（`:core` 连 `android.*` 都不能依赖，所以拼在 core 里就等于硬编码中文）。
 */
fun importRejectionText(rejected: ImportResult.Rejected): Pair<Int, List<Any>> =
    when (rejected.reason) {
        ImportRejection.EMPTY_FILE -> R.string.import_reject_empty_file to emptyList()
        ImportRejection.NOT_ANCHOR_FILE -> R.string.import_reject_not_anchor_file to emptyList()
        ImportRejection.MISSING_VERSION -> R.string.import_reject_missing_version to emptyList()
        ImportRejection.UNSUPPORTED_VERSION ->
            R.string.import_reject_unsupported_version to rejected.args
        ImportRejection.PARSE_ERROR -> R.string.import_reject_parse_error to rejected.args
        ImportRejection.UNKNOWN_PARSE_ERROR ->
            R.string.import_reject_unknown_parse_error to emptyList()
        ImportRejection.DUPLICATE_DATES -> R.string.import_reject_duplicate_dates to rejected.args
        ImportRejection.INVALID_FIELD -> {
            val (field, raw) = rejected.args
            importFieldMessageRes(field as ImportField) to listOf(raw)
        }
    }

/** 时间字段解析失败的整句文案；占位符是文件里的原始文本（例如 `2024-13-01`）。 */
@StringRes
fun importFieldMessageRes(field: ImportField): Int = when (field) {
    ImportField.CHECK_IN_DATE -> R.string.import_reject_invalid_check_in_date
    ImportField.CHECK_IN_TIME -> R.string.import_reject_invalid_check_in_time
    ImportField.RELAPSE_TIME -> R.string.import_reject_invalid_relapse_time
    ImportField.URGE_STARTED_AT -> R.string.import_reject_invalid_urge_started_at
    ImportField.ASSESSMENT_TAKEN_AT -> R.string.import_reject_invalid_assessment_taken_at
}
