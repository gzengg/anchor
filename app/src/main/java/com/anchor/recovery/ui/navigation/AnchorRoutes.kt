package com.anchor.recovery.ui.navigation

/**
 * 全部路由集中定义，避免字符串散落在各处。
 */
object AnchorRoutes {
    // 底部 4 个 Tab
    const val HOME = "home"
    const val TOOLS = "tools"
    const val LIBRARY = "library"
    const val JOURNAL = "journal"

    // 二级页面
    const val CHECK_IN = "checkin"
    const val URGE_SURFING = "urge_surfing"
    const val DELAY_TOOL = "delay_tool"
    const val RELAPSE_EDIT = "relapse_edit"
    const val ASSESSMENT_HUB = "assessment_hub"
    const val CSBD_INTRO = "csbd_intro"
    const val CSBD_QUIZ = "csbd_quiz"
    const val CSBD_RESULT = "csbd_result"
    const val MORAL_INTRO = "moral_intro"
    const val MORAL_QUIZ = "moral_quiz"
    const val MORAL_RESULT = "moral_result"
    const val ARTICLE = "article/{articleId}"
    const val SETTINGS = "settings"
    const val MILESTONES = "milestones"

    val bottomTabs = listOf(HOME, TOOLS, LIBRARY, JOURNAL)

    fun article(articleId: String): String = "article/$articleId"

    /** 顶部标题栏文案；返回 null 表示该路由自行渲染标题。 */
    fun title(route: String?): String? = when (route) {
        HOME -> "磐石"
        TOOLS -> "工具"
        LIBRARY -> "知识库"
        JOURNAL -> "日志"
        CHECK_IN -> "今日打卡"
        URGE_SURFING -> "渴求冲浪"
        DELAY_TOOL -> "十分钟延时"
        RELAPSE_EDIT -> "记录一次破戒"
        ASSESSMENT_HUB -> "自评问卷"
        CSBD_INTRO, CSBD_QUIZ, CSBD_RESULT -> "成瘾倾向自评"
        MORAL_INTRO, MORAL_QUIZ, MORAL_RESULT -> "道德冲突评估"
        ARTICLE -> "文章"
        SETTINGS -> "设置"
        MILESTONES -> "里程碑"
        else -> null
    }
}
