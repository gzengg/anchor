package com.anchor.recovery.ui.navigation

import androidx.annotation.StringRes
import com.anchor.recovery.R

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

    /** 顶部标题栏文案的资源 id；返回 null 表示该路由自行渲染标题。 */
    @StringRes
    fun title(route: String?): Int? = when (route) {
        HOME -> R.string.route_title_home
        TOOLS -> R.string.route_title_tools
        LIBRARY -> R.string.route_title_library
        JOURNAL -> R.string.route_title_journal
        CHECK_IN -> R.string.route_title_check_in
        URGE_SURFING -> R.string.route_title_urge_surfing
        DELAY_TOOL -> R.string.route_title_delay_tool
        RELAPSE_EDIT -> R.string.route_title_relapse_edit
        ASSESSMENT_HUB -> R.string.route_title_assessment_hub
        CSBD_INTRO, CSBD_QUIZ, CSBD_RESULT -> R.string.route_title_csbd
        MORAL_INTRO, MORAL_QUIZ, MORAL_RESULT -> R.string.route_title_moral
        ARTICLE -> R.string.route_title_article
        SETTINGS -> R.string.route_title_settings
        MILESTONES -> R.string.route_title_milestones
        else -> null
    }
}
