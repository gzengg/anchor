package com.anchor.recovery.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import com.anchor.recovery.ui.theme.AnchorTheme

/**
 * 可点击容器：iOS 的按压反馈是「整块轻微变暗」，不是 Material 的涟漪扩散。
 *
 * 全应用的点击反馈都收口到这里（不用 ripple，也不动 `LocalIndication`，
 * 免得影响还没改版的页面）。放在这一层还有两个好处：
 * 1. 分割线、圆角、按压态一次写对，各页面不用重复；
 * 2. 长按与单击走同一个 interactionSource，反馈时序一致。
 *
 * @param color 底色，默认透明（由外层卡片提供）
 * @param role 默认按按钮上报；纯展示型卡片传 `null`
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun AnchorClickableSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RectangleShape,
    color: Color = Color.Transparent,
    role: Role? = Role.Button,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
    contentPadding: PaddingValues = PaddingValues(),
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = AnchorTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    // 按压变暗用短过渡（80ms）：iOS 是「跟手立刻变暗」，太慢会感觉点不动。
    val overlayAlpha by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.12f else 0f,
        animationSpec = tween(durationMillis = 80),
        label = "anchorPressOverlay",
    )
    val overlayColor = if (colors.isLight) Color.Black else Color.White

    Box(
        modifier = modifier
            .clip(shape)
            .background(color)
            .drawBehind {
                if (overlayAlpha > 0f) {
                    drawRect(color = overlayColor.copy(alpha = overlayAlpha))
                }
            }
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = enabled,
                        role = role,
                        onLongClickLabel = onLongClickLabel,
                        onLongClick = onLongClick,
                        onClick = onClick,
                    )
                } else {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = enabled,
                        role = role,
                        onClick = onClick,
                    )
                },
            )
            .padding(contentPadding),
        content = content,
    )
}
