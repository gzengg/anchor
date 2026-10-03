package com.anchor.recovery.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.anchor.recovery.data.content.ContentRepository
import com.anchor.recovery.data.repo.AnchorRepository
import com.anchor.recovery.ui.common.ComingSoonScreen
import com.anchor.recovery.ui.home.CheckInScreen
import com.anchor.recovery.ui.home.HomeScreen
import com.anchor.recovery.ui.journal.TimelineScreen
import com.anchor.recovery.ui.library.ArticleScreen
import com.anchor.recovery.ui.library.LibraryScreen
import com.anchor.recovery.ui.navigation.AnchorRoutes
import com.anchor.recovery.ui.tools.ToolsScreen

/**
 * 单 Activity + Navigation-Compose 的根布局。
 *
 * edge-to-edge 处理：只在这里放一个 Scaffold，TopAppBar/NavigationBar 各自消费系统栏 insets，
 * 页面内容拿到的是已经扣掉系统栏的 padding，避免逐页重复加 insets 导致的双重留白。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnchorApp(repository: AnchorRepository, contentRepository: ContentRepository) {
    val factory = remember(repository) { anchorViewModelFactory(repository) }
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val onTab = currentRoute != null && currentRoute in AnchorRoutes.bottomTabs
    val title = AnchorRoutes.title(currentRoute)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (title != null) {
                TopAppBar(
                    title = { Text(text = title) },
                    navigationIcon = {
                        if (currentRoute != null && !onTab) {
                            IconButton(onClick = { navController.navigateUp() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "返回",
                                )
                            }
                        }
                    },
                )
            }
        },
        bottomBar = {
            if (onTab) {
                NavigationBar {
                    AnchorRoutes.bottomTabs.forEach { route ->
                        NavigationBarItem(
                            selected = currentRoute == route,
                            onClick = { navController.switchTab(route) },
                            icon = {
                                Icon(
                                    imageVector = tabIcon(route),
                                    contentDescription = tabLabel(route),
                                )
                            },
                            label = { Text(text = tabLabel(route)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = AnchorRoutes.HOME,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
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
                )
            }
            composable(AnchorRoutes.LIBRARY) {
                LibraryScreen(
                    content = contentRepository,
                    onOpenArticle = { navController.navigate(AnchorRoutes.article(it)) },
                )
            }
            composable(AnchorRoutes.JOURNAL) {
                TimelineScreen(viewModel = viewModel(factory = factory))
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
                ComingSoonScreen(title = "渴求冲浪")
            }
            composable(AnchorRoutes.DELAY_TOOL) {
                ComingSoonScreen(title = "十分钟延时")
            }
            composable(AnchorRoutes.RELAPSE_EDIT) {
                ComingSoonScreen(title = "记录一次复吸")
            }
            composable(AnchorRoutes.SETTINGS) {
                ComingSoonScreen(title = "设置")
            }
        }
    }
}

private const val ARTICLE_ID_ARG = "articleId"

/** 切换底部 Tab：保留各 Tab 的返回栈与滚动位置。 */
private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun tabIcon(route: String): ImageVector = when (route) {
    AnchorRoutes.TOOLS -> Icons.Outlined.Build
    AnchorRoutes.LIBRARY -> Icons.AutoMirrored.Outlined.MenuBook
    AnchorRoutes.JOURNAL -> Icons.Outlined.EditNote
    else -> Icons.Outlined.Home
}

private fun tabLabel(route: String): String = when (route) {
    AnchorRoutes.TOOLS -> "工具"
    AnchorRoutes.LIBRARY -> "知识库"
    AnchorRoutes.JOURNAL -> "日志"
    else -> "首页"
}
