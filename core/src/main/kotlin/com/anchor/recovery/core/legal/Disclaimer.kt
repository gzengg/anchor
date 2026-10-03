package com.anchor.recovery.core.legal

/**
 * 首次启动的整体免责声明（提示词 §5.1）。
 *
 * [VERSION] 是声明的「内容版本」：DataStore 里记录的已同意版本低于当前版本时，
 * 即使 Onboarding 早就走过，也必须重新阅读并同意（声明更新后强制重读）。
 */
object Disclaimer {

    /** 声明内容版本。每次改动 [paragraphs] 的实质含义都要 +1。 */
    const val VERSION: Int = 1

    const val TITLE: String = "使用前请阅读"

    const val ACK_LABEL: String = "我已阅读并理解上述说明"

    /** 隐私说明段落。单列一份给设置页复用，避免同一句话写两遍而走形。 */
    val PRIVACY_PARAGRAPH: String =
        "所有记录只保存在你自己的手机上，不会上传、不会同步到任何服务器。" +
            "数据以明文存储在应用私有目录中，设备被解锁或被 root 后可能被读取。"

    /** 全部段落，按显示顺序。 */
    val paragraphs: List<String> = listOf(
        "磐石是一个自助记录与科普工具，帮你记录打卡、渴求与情绪，并提供离线科普文章。",
        "它不是医疗器械，不提供诊断、治疗或医疗建议。任何自评问卷的结果都只是参考，" +
            "不能作为疾病判断的依据。",
        PRIVACY_PARAGRAPH,
        "如果你处于紧急情况，或情绪持续困扰影响生活，请尽快联系专业医生或当地心理援助热线。",
        "你可以随时在设置页导出全部数据或一键清空，清空后无法恢复。",
    )

    /**
     * 是否需要重新走一遍声明页。
     *
     * @param onboardingDone DataStore 里的 `onboarding_done`
     * @param acknowledgedVersion DataStore 里的 `disclaimer_ack_version`
     */
    fun requiresAcknowledgement(onboardingDone: Boolean, acknowledgedVersion: Int): Boolean =
        !onboardingDone || acknowledgedVersion < VERSION
}
