package com.anchor.recovery.core.legal

/**
 * 首次启动的整体免责声明（提示词 §5.1）。
 *
 * [VERSION] 是声明的「内容版本」：DataStore 里记录的已同意版本低于当前版本时，
 * 即使 Onboarding 早就走过，也必须重新阅读并同意（声明更新后强制重读）。
 *
 * 正文不在这里：`:core` 不能依赖 `android.*`，所以本模块只保留段落的稳定 id
 * （[DisclaimerParagraph]）与显示顺序；文案在 `:app` 的 `values/strings_legal.xml`，
 * 由 `LegalContentText.disclaimerParagraphRes` 按 id 映射。
 */
object Disclaimer {

    /** 声明内容版本。每次改动正文的实质含义都要 +1。 */
    const val VERSION: Int = 4

    /** 隐私说明段落。单列一份给设置页复用，避免同一句话写两遍而走形。 */
    val PRIVACY_PARAGRAPH: DisclaimerParagraph = DisclaimerParagraph.LOCAL_DATA_PRIVACY

    /** 全部段落，按显示顺序。每条只说一件事，避免长句堆叠。 */
    val paragraphs: List<DisclaimerParagraph> = DisclaimerParagraph.entries.toList()

    /**
     * 是否需要重新走一遍声明页。
     *
     * @param onboardingDone DataStore 里的 `onboarding_done`
     * @param acknowledgedVersion DataStore 里的 `disclaimer_ack_version`
     */
    fun requiresAcknowledgement(onboardingDone: Boolean, acknowledgedVersion: Int): Boolean =
        !onboardingDone || acknowledgedVersion < VERSION
}

/**
 * 免责声明的每一段；`:core` 不持有中文文案。
 *
 * 声明顺序 = 枚举声明顺序（[Disclaimer.paragraphs] 依赖它），改顺序等于改合规文案的展示顺序。
 */
enum class DisclaimerParagraph {
    /** 工具定位：自助记录与科普，另有离线科普文章。 */
    TOOL_SCOPE,

    /** 不是医疗器械，不提供诊断、治疗或医疗建议；自评结果只是参考。 */
    NOT_MEDICAL_DEVICE,

    /** 数据只存本机、加密方式，以及设备被解锁或 root 后的风险。 */
    LOCAL_DATA_PRIVACY,

    /** 紧急情况或情绪持续困扰时的求助指引。 */
    EMERGENCY_HELP,

    /** 导出 / 用导出文件恢复 / 一键清空的说明。 */
    DATA_CONTROL,
}
