package com.anchor.recovery.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.anchor.recovery.AppInfo
import com.anchor.recovery.core.legal.Disclaimer
import com.anchor.recovery.data.content.ContentRepository
import com.anchor.recovery.data.repo.AnchorRepository
import com.anchor.recovery.data.settings.AnchorSettings
import com.anchor.recovery.ui.assessment.AssessmentHubScreen
import com.anchor.recovery.ui.assessment.AssessmentHubViewModel
import com.anchor.recovery.ui.assessment.CsbdIntroScreen
import com.anchor.recovery.ui.assessment.CsbdQuizViewModel
import com.anchor.recovery.ui.assessment.CsbdResultScreen
import com.anchor.recovery.ui.assessment.CsbdResultViewModel
import com.anchor.recovery.ui.assessment.MoralIntroScreen
import com.anchor.recovery.ui.assessment.MoralQuizViewModel
import com.anchor.recovery.ui.assessment.MoralResultScreen
import com.anchor.recovery.ui.assessment.MoralResultViewModel
import com.anchor.recovery.ui.assessment.QuizScreen
import com.anchor.recovery.ui.home.CheckInScreen
import com.anchor.recovery.ui.home.HomeScreen
import com.anchor.recovery.ui.journal.TimelineScreen
import com.anchor.recovery.ui.library.ArticleScreen
import com.anchor.recovery.ui.library.LibraryScreen
import com.anchor.recovery.ui.milestones.MilestoneWallScreen
import com.anchor.recovery.ui.components.AnchorIconButton
import com.anchor.recovery.ui.components.AnchorNavBar
import com.anchor.recovery.ui.components.AnchorTab
import com.anchor.recovery.ui.components.AnchorTabBar
import com.anchor.recovery.ui.components.LocalAnchorNavBarState
import com.anchor.recovery.ui.components.rememberAnchorNavBarState
import com.anchor.recovery.ui.navigation.AnchorRoutes
import com.anchor.recovery.ui.navigation.AnchorTransitions
import com.anchor.recovery.ui.onboarding.OnboardingScreen
import com.anchor.recovery.ui.settings.SettingsScreen
import com.anchor.recovery.ui.settings.SettingsViewModel
import com.anchor.recovery.ui.tools.DelayToolScreen
import com.anchor.recovery.ui.tools.RelapseEditScreen
import com.anchor.recovery.ui.tools.ToolsScreen
import com.anchor.recovery.ui.tools.UrgeSurfingScreen
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * 单 Activity + Navigation-Compose 的根布局。
 *
 * edge-to-edge 处理：只在这里放一个 Scaffold，AnchorNavBar/AnchorTabBar 各自消费系统栏 insets，
 * 页面内容拿到的是已经扣掉系统栏的 padding，避免逐页重复加 insets 导致的双重留白。
 *
 * 导航栏只有一份（不逐页复制）：页面根组件调 PublishAnchorNavBar 把自己交给它，
 * 大标题由页面放在自己的内容顶部，滚过 44dp 后内联标题淡入。
 */
@Composable
fun AnchorApp(
    repository: AnchorRepository,
    contentRepository: ContentRepository,
    settings: AnchorSettings,
) {
    val factory = remember(repository, settings) { anchorViewModelFactory(repository, settings) }
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    val gate by remember(settings) {
        settings.snapshot.map { snapshot ->
            val required = Disclaimer.requiresAcknowledgement(
                onboardingDone = snapshot.onboardingDone,
                acknowledgedVersion = snapshot.disclaimerAckVersion,
            )
            if (required) DisclaimerGate.REQUIRED else DisclaimerGate.PASSED
        }
    }.collectAsState(initial = DisclaimerGate.LOADING)

    when (gate) {
        // DataStore 首帧还没读到：留白一瞬，避免给已同意的用户闪一下声明页。
        DisclaimerGate.LOADING -> {
            Box(modifier = Modifier.fillMaxSize())
            return
        }

        DisclaimerGate.REQUIRED -> {
            OnboardingScreen(
                onAccept = {
                    coroutineScope.launch {
                        settings.setOnboardingDone(true)
                        settings.setDisclaimerAckVersion(Disclaimer.VERSION)
                    }
                },
            )
            return
        }

        DisclaimerGate.PASSED -> Unit
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val onTab = currentRoute != null && currentRoute in AnchorRoutes.bottomTabs
    val title = AnchorRoutes.title(currentRoute)

    // 导航栏的唯一状态源：页面把滚动状态挂进来，栏自己读透明度。
    val navBarState = rememberAnchorNavBarState()
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (title != null) {
                AnchorNavBar(
                    title = title,
                    onBack = if (currentRoute != null && !onTab) {
                        { navController.navigateUp() }
                    } else {
                        null
                    },
                    titleAlpha = { navBarState.titleAlpha() },
                    actions = {
                        if (onTab) {
                            AnchorIconButton(
                                icon = Icons.Outlined.Settings,
                                contentDescription = "设置",
                                onClick = {
                                    navController.navigate(AnchorRoutes.SETTINGS) {
                                        launchSingleTop = true
                                    }
                                },
                            )
                        }
                    },
                )
            }
        },
        bottomBar = {
            if (onTab) {
                AnchorTabBar(
                    tabs = tabBarItems(),
                    selectedIndex = AnchorRoutes.bottomTabs.indexOf(currentRoute).coerceAtLeast(0),
                    onSelect = { index -> navController.switchTab(AnchorRoutes.bottomTabs[index]) },
                )
            }
        },
    ) { padding ->
        // 页面通过这个状态把滚动进度交给顶栏（大标题滚走、内联标题交叉淡入）。
        CompositionLocalProvider(LocalAnchorNavBarState provides navBarState) {
            NavHost(
                navController = navController,
                startDestination = AnchorRoutes.HOME,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    // 系统栏 insets 已在 Scaffold 里消费完；键盘弹出时再补一段，
                    // 让底部输入框（打卡备注、破戒表单、提示语）不被输入法遮住。
                    .consumeWindowInsets(padding)
                    .imePadding(),
                // 转场参数集中在 AnchorTransitions：切 Tab 无方向感，二级页给一点横向位移提示层级。
                enterTransition = {
                    if (isTabSwitch()) {
                        AnchorTransitions.tabEnter.invoke(this)
                    } else {
                        AnchorTransitions.pushEnter.invoke(this)
                    }
                },
                exitTransition = {
                    if (isTabSwitch()) {
                        AnchorTransitions.tabExit.invoke(this)
                    } else {
                        AnchorTransitions.pushExit.invoke(this)
                    }
                },
                popEnterTransition = { AnchorTransitions.popEnter.invoke(this) },
                popExitTransition = { AnchorTransitions.popExit.invoke(this) },
            ) {
                composable(AnchorRoutes.HOME) {
                    HomeScreen(
                        viewModel = viewModel(factory = factory),
                        onOpenCheckIn = { navController.navigate(AnchorRoutes.CHECK_IN) },
                        onOpenUrgeSurfing = { navController.navigate(AnchorRoutes.URGE_SURFING) },
                        onOpenDelayTool = { navController.navigate(AnchorRoutes.DELAY_TOOL) },
                        onOpenArticle = { navController.navigate(AnchorRoutes.article(it)) },
                        onOpenLibrary = { navController.navigate(AnchorRoutes.LIBRARY) },
                    )
                }
                composable(AnchorRoutes.CHECK_IN) {
                    CheckInScreen(viewModel = viewModel(factory = factory))
                }
                composable(AnchorRoutes.TOOLS) {
                    ToolsScreen(
                        onOpenUrgeSurfing = { navController.navigate(AnchorRoutes.URGE_SURFING) },
                        onOpenDelayTool = { navController.navigate(AnchorRoutes.DELAY_TOOL) },
                        onOpenRelapseEdit = { navController.navigate(AnchorRoutes.RELAPSE_EDIT) },
                        onOpenAssessmentHub = { navController.navigate(AnchorRoutes.ASSESSMENT_HUB) },
                        onOpenMilestones = { navController.navigate(AnchorRoutes.MILESTONES) },
                    )
                }
                composable(AnchorRoutes.LIBRARY) {
                    LibraryScreen(
                        content = contentRepository,
                        onOpenArticle = { navController.navigate(AnchorRoutes.article(it)) },
                    )
                }
                composable(AnchorRoutes.JOURNAL) {
                    TimelineScreen(
                        viewModel = viewModel(factory = factory),
                        onOpenRelapseEdit = { navController.navigate(AnchorRoutes.RELAPSE_EDIT) },
                    )
                }
                composable(
                    route = AnchorRoutes.ARTICLE,
                    arguments = listOf(navArgument(ARTICLE_ID_ARG) { type = NavType.StringType }),
                ) { entry ->
                    ArticleScreen(
                        articleId = entry.arguments?.getString(ARTICLE_ID_ARG).orEmpty(),
                        content = contentRepository,
                    )
                }
                composable(AnchorRoutes.URGE_SURFING) {
                    UrgeSurfingScreen(
                        viewModel = viewModel(factory = factory),
                        onDone = { navController.popBackStack() },
                    )
                }
                composable(AnchorRoutes.DELAY_TOOL) {
                    DelayToolScreen(
                        viewModel = viewModel(factory = factory),
                        onDone = { navController.popBackStack() },
                    )
                }
                composable(AnchorRoutes.RELAPSE_EDIT) {
                    RelapseEditScreen(
                        viewModel = viewModel(factory = factory),
                        onSaved = {
                            navController.popBackStack()
                            navController.navigate(AnchorRoutes.JOURNAL) {
                                launchSingleTop = true
                            }
                        },
                    )
                }
                composable(AnchorRoutes.SETTINGS) {
                    val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
                    SettingsScreen(viewModel = settingsViewModel)
                }
                composable(AnchorRoutes.MILESTONES) {
                    MilestoneWallScreen(viewModel = viewModel(factory = factory))
                }
                composable(AnchorRoutes.ASSESSMENT_HUB) {
                    val hubViewModel: AssessmentHubViewModel = viewModel(factory = factory)
                    val hubState by hubViewModel.state.collectAsState()
                    AssessmentHubScreen(
                        state = hubState,
                        onOpenCsbd = { navController.navigate(AnchorRoutes.CSBD_INTRO) },
                        onOpenMoral = { navController.navigate(AnchorRoutes.MORAL_INTRO) },
                        onOpenLastCsbd = { navController.navigate(AnchorRoutes.CSBD_RESULT) },
                        onOpenLastMoral = { navController.navigate(AnchorRoutes.MORAL_RESULT) },
                    )
                }
                composable(AnchorRoutes.CSBD_INTRO) {
                    CsbdIntroScreen(
                        onStart = { navController.navigate(AnchorRoutes.CSBD_QUIZ) },
                    )
                }
                composable(AnchorRoutes.CSBD_QUIZ) {
                    val quizViewModel: CsbdQuizViewModel = viewModel(factory = factory)
                    val quizState by quizViewModel.state.collectAsState()
                    QuizScreen(
                        state = quizState,
                        onSelect = quizViewModel::select,
                        onPrevious = quizViewModel::previous,
                        onNext = quizViewModel::next,
                        onSubmit = quizViewModel::submit,
                        onFinished = {
                            navController.navigate(AnchorRoutes.CSBD_RESULT) {
                                popUpTo(AnchorRoutes.CSBD_INTRO) { inclusive = true }
                            }
                        },
                    )
                }
                composable(AnchorRoutes.CSBD_RESULT) {
                    val resultViewModel: CsbdResultViewModel = viewModel(factory = factory)
                    val resultState by resultViewModel.state.collectAsState()
                    CsbdResultScreen(state = resultState)
                }
                composable(AnchorRoutes.MORAL_INTRO) {
                    MoralIntroScreen(
                        onStart = { navController.navigate(AnchorRoutes.MORAL_QUIZ) },
                    )
                }
                composable(AnchorRoutes.MORAL_QUIZ) {
                    val quizViewModel: MoralQuizViewModel = viewModel(factory = factory)
                    val quizState by quizViewModel.state.collectAsState()
                    QuizScreen(
                        state = quizState,
                        onSelect = quizViewModel::select,
                        onPrevious = quizViewModel::previous,
                        onNext = quizViewModel::next,
                        onSubmit = quizViewModel::submit,
                        onFinished = {
                            navController.navigate(AnchorRoutes.MORAL_RESULT) {
                                popUpTo(AnchorRoutes.MORAL_INTRO) { inclusive = true }
                            }
                        },
                    )
                }
                composable(AnchorRoutes.MORAL_RESULT) {
                    val resultViewModel: MoralResultViewModel = viewModel(factory = factory)
                    val resultState by resultViewModel.state.collectAsState()
                    MoralResultScreen(state = resultState)
                }
            }
        }
    }
}

private const val ARTICLE_ID_ARG = "articleId"

/** 初始与目标路由都在底部 Tab 里，就是切 Tab（不需要方向感）。 */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTabSwitch(): Boolean {
    val from = initialState.destination.route
    val to = targetState.destination.route
    return from != null && from in AnchorRoutes.bottomTabs && to != null && to in AnchorRoutes.bottomTabs
}

/** 首启门禁的三种状态。用三态枚举而不是可空布尔，已同意的用户不会看到声明页闪现。 */
private enum class DisclaimerGate { LOADING, REQUIRED, PASSED }

/** 切换底部 Tab：保留各 Tab 的返回栈与滚动位置。 */
private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** 四个 Tab 的图标与文案：选中用实心、未选中用描边（iOS 的选中态）。 */
private fun tabBarItems(): List<AnchorTab> = listOf(
    AnchorTab("首页", Icons.Outlined.Home, Icons.Filled.Home),
    AnchorTab("工具", Icons.Outlined.Build, Icons.Filled.Build),
    AnchorTab("知识库", Icons.AutoMirrored.Outlined.MenuBook, Icons.AutoMirrored.Filled.MenuBook),
    AnchorTab("日志", Icons.Outlined.EditNote, Icons.Filled.EditNote),
)
