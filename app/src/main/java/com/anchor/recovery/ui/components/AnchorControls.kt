package com.anchor.recovery.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anchor.recovery.ui.theme.AnchorTheme
import com.anchor.recovery.ui.theme.AnchorType

/*
 * iOS 形状差异太大的三件套：开关、分段控件、按钮。
 * 别的控件继续用 Material3 改样式（改样式不改行为）；这三个如果用 M3 去「压」形状，
 * 要跟组件内部的度量、水波、动画打架，自绘反而更短更稳，语义（selectable/toggleable/clickable）
 * 照旧交给 Compose 的语义修饰符，无障碍行为不变。
 */

/**
 * iOS 开关：51×31dp，滑块 27dp。
 *
 * 开启色用 HIG 的 systemGreen（iOS 开关默认就是绿色），不是品牌色——
 * 开关是「系统控件」，跟品牌色走会让它看着像自定制的 Material 组件。
 */
@Composable
fun AnchorSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = AnchorTheme.colors
    val haptics = rememberAnchorHaptics()
    val thumbOffset by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 900f),
        label = "anchorSwitchThumb",
    )
    val trackColor by animateColorAsState(
        targetValue = if (checked) colors.success else colors.fillStrong,
        animationSpec = tween(durationMillis = 150),
        label = "anchorSwitchTrack",
    )

    Box(
        modifier = modifier
            .size(width = 51.dp, height = 31.dp)
            .clip(CircleShape)
            .alpha(if (enabled) 1f else 0.4f)
            .background(trackColor)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onValueChange = {
                    haptics.light()
                    onCheckedChange(it)
                },
            ),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                // 2dp 边距 + 20dp 行程 = 51 - 27 - 2×2，滑块两端都与轨道对齐。
                .offset(x = (2 + thumbOffset * 20).dp)
                .size(27.dp)
                .shadow(elevation = 2.dp, shape = CircleShape)
                .background(Color.White, CircleShape),
        )
    }
}

/**
 * iOS 分段控件。
 *
 * @param selected 当前选中项（按值匹配，页面不用自己算下标）
 * @param label 选项文案，默认用 `toString()`
 */
@Composable
fun <T> AnchorSegmentedControl(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: (T) -> String = { it.toString() },
) {
    if (options.isEmpty()) return
    val colors = AnchorTheme.colors
    val haptics = rememberAnchorHaptics()
    val selectedIndex = options.indexOf(selected).coerceAtLeast(0)

    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(colors.fill)
            .padding(2.dp)
            .selectableGroup(),
    ) {
        val segmentWidth = maxWidth / options.size
        val indicatorOffset by animateFloatAsState(
            targetValue = selectedIndex.toFloat(),
            animationSpec = spring(dampingRatio = 0.85f, stiffness = 700f),
            label = "anchorSegment",
        )

        // 选中块是「浮」在轨道上的白色小卡（iOS 用阴影而不是描边区分层次）。
        Box(
            modifier = Modifier
                .offset(x = segmentWidth * indicatorOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(7.dp))
                .background(colors.cardBackground, RoundedCornerShape(7.dp)),
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .selectable(
                            selected = isSelected,
                            role = Role.RadioButton,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (!isSelected) {
                                    haptics.selection()
                                    onSelect(option)
                                }
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label(option),
                        style = if (isSelected) AnchorType.subheadlineSemibold else AnchorType.subheadline,
                        color = if (isSelected) colors.label else colors.labelSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
        }
    }
}

/** 按钮形态：实心（主操作）、浅色（次要操作）、纯文字（弱操作）。 */
enum class AnchorButtonStyle { Filled, Tinted, Plain }

/** 导航栏上的图标按钮：44dp 可点区、图标 24dp、主色。 */
@Composable
fun AnchorIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = AnchorTheme.colors.tint,
    enabled: Boolean = true,
) {
    AnchorClickableSurface(
        onClick = onClick,
        modifier = modifier.size(44.dp),
        shape = CircleShape,
        enabled = enabled,
        role = Role.Button,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.align(Alignment.Center).size(24.dp),
        )
    }
}

/**
 * iOS 风格按钮：高 50dp、圆角 12dp、17sp 半粗。
 *
 * 破坏性操作（删除、清空）用同一个组件 + `destructive = true`，
 * 保证「危险动作长什么样」在全应用只有一处定义。
 */
@Composable
fun AnchorButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AnchorButtonStyle = AnchorButtonStyle.Filled,
    enabled: Boolean = true,
    destructive: Boolean = false,
    leadingIcon: ImageVector? = null,
) {
    val colors = AnchorTheme.colors
    val accent = if (destructive) colors.danger else colors.tint
    val container = when (style) {
        AnchorButtonStyle.Filled -> accent
        AnchorButtonStyle.Tinted -> accent.copy(alpha = 0.14f)
        AnchorButtonStyle.Plain -> Color.Transparent
    }
    val contentColor = when (style) {
        AnchorButtonStyle.Filled -> Color.White
        AnchorButtonStyle.Tinted, AnchorButtonStyle.Plain -> accent
    }

    AnchorClickableSurface(
        onClick = onClick,
        modifier = modifier
            .height(50.dp)
            .alpha(if (enabled) 1f else 0.4f),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = container,
        role = Role.Button,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = AnchorType.headline,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
