package com.anchor.recovery.ui.text

import androidx.annotation.StringRes
import com.anchor.recovery.R
import com.anchor.recovery.core.content.Credibility
import com.anchor.recovery.core.legal.DisclaimerParagraph

/**
 * `:core` 的免责段落 id / 可信度等级 → 展示文案资源的映射。
 *
 * `:core` 不持有中文文案（它连 `android.*` 都不能依赖），只给出可判定的 id；
 * 文案在 `values/strings_legal.xml`。这里的 `when` 不写 `else`：`:core` 新增段落或
 * 等级时编译期就会失败，避免出现「有 id 没文案」。
 *
 * 注意 [credibilityRes] 只负责徽章上的展示名；[Credibility.label] 是数据 key，不能拿来做界面文案。
 */

@StringRes
fun disclaimerParagraphRes(paragraph: DisclaimerParagraph): Int = when (paragraph) {
    DisclaimerParagraph.TOOL_SCOPE -> R.string.legal_disclaimer_paragraph_tool_scope
    DisclaimerParagraph.NOT_MEDICAL_DEVICE -> R.string.legal_disclaimer_paragraph_not_medical_device
    DisclaimerParagraph.LOCAL_DATA_PRIVACY -> R.string.legal_disclaimer_paragraph_local_data_privacy
    DisclaimerParagraph.EMERGENCY_HELP -> R.string.legal_disclaimer_paragraph_emergency_help
    DisclaimerParagraph.DATA_CONTROL -> R.string.legal_disclaimer_paragraph_data_control
}

@StringRes
fun credibilityRes(credibility: Credibility): Int = when (credibility) {
    Credibility.HIGH -> R.string.legal_credibility_high
    Credibility.MEDIUM -> R.string.legal_credibility_medium
    Credibility.LOW -> R.string.legal_credibility_low
}
