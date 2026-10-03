package com.anchor.recovery.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavBackStackEntry

/*
 * iOS 式页面转场：push（二级页右进左出，退出的页面只走 1/3 宽做视差）
 * 与 present（自下而上，给整屏模态/工具页）。
 *
 * 时长没有照抄 iOS 的 350ms：v1 在真机上做过一轮「转场提速」，
 * 原来的 700ms 默认值被判定为拖沓，这里沿用量级的快节奏（200ms 进 / 150ms 退），
 * 只把位移形状换成 iOS 的整屏滑动 + 视差。手感需要微调时改这两个常量即可。
 *
 * 返回手势不额外实现：Android 13+ 的预测性返回由系统提供右滑预览，
 * navigation-compose 会自动用 popEnter/popExit 接管。
 */
object AnchorTransitions {

    /** 进二级页：整屏从左滑入，退出页只滑 1/3（iOS 的视差层级感）。 */
    private const val PUSH_MS = 200

    /** 退二级页：被压住的页面滑回原位比新页面滑出更快，减少两页叠在一起的时间。 */
    private const val POP_MS = 150

    /** present：整屏模态/工具页自下而上。 */
    private const val PRESENT_MS = 260

    /** 切底部 Tab：没有方向感，只做快速淡入淡出。 */
    private const val TAB_MS = 90

    /** iOS 的默认曲线（对应 UIKit 的 ease-in-out 偏快出）。 */
    private val IosEasing: Easing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

    val pushEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideIntoContainer(
            towards = SlideDirection.Left,
            animationSpec = tween(durationMillis = PUSH_MS, easing = IosEasing),
            initialOffset = { it },
        ) + fadeIn(animationSpec = tween(durationMillis = PUSH_MS / 2))
    }

    val pushExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutOfContainer(
            towards = SlideDirection.Left,
            animationSpec = tween(durationMillis = PUSH_MS, easing = IosEasing),
            targetOffset = { it / 3 },
        )
    }

    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideIntoContainer(
            towards = SlideDirection.Right,
            animationSpec = tween(durationMillis = POP_MS, easing = IosEasing),
            initialOffset = { it / 3 },
        )
    }

    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutOfContainer(
            towards = SlideDirection.Right,
            animationSpec = tween(durationMillis = POP_MS, easing = IosEasing),
            targetOffset = { it },
        ) + fadeOut(animationSpec = tween(durationMillis = POP_MS / 2))
    }

    val presentEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInVertically(
            animationSpec = tween(durationMillis = PRESENT_MS, easing = IosEasing),
            initialOffsetY = { it },
        )
    }

    val presentExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutVertically(
            animationSpec = tween(durationMillis = PRESENT_MS, easing = IosEasing),
            targetOffsetY = { it },
        )
    }

    val tabEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn(animationSpec = tween(durationMillis = TAB_MS))
    }

    val tabExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut(animationSpec = tween(durationMillis = TAB_MS))
    }
}
