package com.anchor.recovery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anchor.recovery.ui.theme.AnchorTheme
import com.anchor.recovery.ui.theme.AnchorType

/*
 * iOS 导航栏 = 固定 44dp 的内联栏 + 页面内容里的「大标题」。
 *
 * 为什么大标题由页面自己放在内容顶部，而不是做成会收缩的顶栏：
 * 收缩顶栏会让「滚动位移」和「顶栏变矮」同时对内容生效，同样的手势内容会被推两次，
 * 得再补一层反向位移才能抵消，越写越像 hack；而 iOS 本来就是把大标题放在滚动内容里的，
 * 滚动 1:1 带走它、内联标题交叉淡入，机制简单且手感一致。
 *
 * 两种用法：
 * 1. 页面自己带栏（详情页）：AnchorNavBar(titleAlpha = { 1f })；
 * 2. 全局单栏（当前的 Scaffold 架构）：页面调 PublishAnchorNavBar(...) 交出滚动状态，
 *    全局栏用 anchorNavBarTitleAlpha() 取透明度——读发生在栏的作用域内，滚动只重组栏。
 */

/** 大标题滚过多少距离后，内联标题完全淡入。约等于大标题块的高度。 */
private val LargeTitleCollapseDistance = 44.dp

/**
 * 全局导航栏要用的当前页滚动状态。
 *
 * 只存「引用 + 是否带大标题」，不存透明度：透明度由导航栏在读的时候现算，
 * 这样滚动帧只重组导航栏，不会顺着状态把整页拖着重组。
 */
@Stable
class AnchorNavBarState internal constructor(private val collapseDistancePx: Float) {
    var scrollState: androidx.compose.foundation.ScrollState? by mutableStateOf(null)
        internal set
    var listState: LazyListState? by mutableStateOf(null)
        internal set
    var hasLargeTitle: Boolean by mutableStateOf(true)
        internal set

    /** 内联标题透明度：没交滚动状态、或页面没有大标题时恒为 1。 */
    fun titleAlpha(): Float {
        val scroll = scrollState
        val list = listState
        return when {
            !hasLargeTitle -> 1f
            scroll != null -> (scroll.value / collapseDistancePx).coerceIn(0f, 1f)
            list != null -> {
                val offset = if (list.firstVisibleItemIndex == 0) {
                    list.firstVisibleItemScrollOffset.toFloat()
                } else {
                    collapseDistancePx
                }
                (offset / collapseDistancePx).coerceIn(0f, 1f)
            }

            else -> 1f
        }
    }
}

val LocalAnchorNavBarState = staticCompositionLocalOf<AnchorNavBarState?> { null }

@Composable
fun rememberAnchorNavBarState(): AnchorNavBarState {
    val density = LocalDensity.current
    val collapseDistancePx = remember(density) { with(density) { LargeTitleCollapseDistance.toPx() } }
    return remember(collapseDistancePx) { AnchorNavBarState(collapseDistancePx) }
}

/**
 * 内联导航栏：44dp 高、标题居中、返回键在左、动作用户自己传。
 *
 * @param title 为 null 时只画返回键与动作（整屏页面用）
 * @param titleAlpha 标题透明度，给大标题交叉淡入用；`() -> Float` 是为了把
 *   「读滚动状态」推迟到栏自己的作用域里
 * @param contentWindowInsets 默认吃掉状态栏高度：这个栏是全屏页面的第一层
 */
@Composable
fun AnchorNavBar(
    title: String? = null,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    titleAlpha: () -> Float = { 1f },
    actions: @Composable RowScope.() -> Unit = {},
    showHairline: Boolean = true,
    contentWindowInsets: WindowInsets = WindowInsets.statusBars,
) {
    val colors = AnchorTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.cardBackground)
            .windowInsetsPadding(contentWindowInsets),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(44.dp)) {
            if (onBack != null) {
                AnchorClickableSurface(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 4.dp)
                        .size(44.dp),
                    shape = CircleShape,
                    role = Role.Button,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "返回",
                        tint = colors.tint,
                        modifier = Modifier.align(Alignment.Center).size(28.dp),
                    )
                }
            }
            if (title != null) {
                Text(
                    text = title,
                    style = AnchorType.headline,
                    color = colors.label,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 56.dp)
                        .alpha(titleAlpha()),
                )
            }
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
        if (showHairline) {
            AnchorHairline(inset = 0.dp)
        }
    }
}

/**
 * 页面内容顶部的大标题（HIG largeTitle：34sp 粗体）。
 *
 * 放在滚动容器的第一个元素里；超过两行会截断，避免大字体设置下把首屏挤没。
 */
@Composable
fun AnchorLargeTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = AnchorType.largeTitle,
        color = AnchorTheme.colors.label,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
    )
}

/** 页面自带导航栏时用：把滚动状态换算成内联标题透明度。 */
@Composable
fun rememberAnchorTitleAlpha(
    scrollState: androidx.compose.foundation.ScrollState,
    collapseDistance: Dp = LargeTitleCollapseDistance,
): () -> Float {
    val density = LocalDensity.current
    val distance = remember(density, collapseDistance) { with(density) { collapseDistance.toPx() } }
    return remember(scrollState, distance) { { (scrollState.value / distance).coerceIn(0f, 1f) } }
}

/** 同上，给用 LazyColumn/LazyList 的页面。 */
@Composable
fun rememberAnchorTitleAlpha(
    listState: LazyListState,
    collapseDistance: Dp = LargeTitleCollapseDistance,
): () -> Float {
    val density = LocalDensity.current
    val distance = remember(density, collapseDistance) { with(density) { collapseDistance.toPx() } }
    return remember(listState, distance) {
        {
            val offset = if (listState.firstVisibleItemIndex == 0) {
                listState.firstVisibleItemScrollOffset.toFloat()
            } else {
                distance
            }
            (offset / distance).coerceIn(0f, 1f)
        }
    }
}

/** 页面把自己交给全局导航栏（单 Scaffold 架构）。在页面根组件里调一次即可。 */
@Composable
fun PublishAnchorNavBar(
    scrollState: androidx.compose.foundation.ScrollState,
    hasLargeTitle: Boolean = true,
) {
    val state = LocalAnchorNavBarState.current ?: return
    DisposableEffect(state, scrollState, hasLargeTitle) {
        state.scrollState = scrollState
        state.listState = null
        state.hasLargeTitle = hasLargeTitle
        onDispose {
            if (state.scrollState === scrollState) state.scrollState = null
        }
    }
}

/** 同上，给 LazyColumn 页面。 */
@Composable
fun PublishAnchorNavBar(
    listState: LazyListState,
    hasLargeTitle: Boolean = true,
) {
    val state = LocalAnchorNavBarState.current ?: return
    DisposableEffect(state, listState, hasLargeTitle) {
        state.listState = listState
        state.scrollState = null
        state.hasLargeTitle = hasLargeTitle
        onDispose {
            if (state.listState === listState) state.listState = null
        }
    }
}


