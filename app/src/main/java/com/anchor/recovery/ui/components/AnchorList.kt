package com.anchor.recovery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anchor.recovery.ui.theme.AnchorTheme
import com.anchor.recovery.ui.theme.AnchorType

/*
 * inset grouped 圆角分组列表：设置页、日志筛选、知识库分类的主体样式。
 *
 * 结构固定为「分组表头 → 白卡（内含若干行，行间 0.5dp 细分割线）→ 卡片间距」，
 * 行高下限 44dp（HIG 的最小可点区域），行内水平留白 16dp。
 */

/** 分组表头：13sp 二级灰。iOS 不加大写转写，这里也保持原文。 */
@Composable
fun AnchorSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = AnchorType.footnote,
        color = AnchorTheme.colors.labelSecondary,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 6.dp),
    )
}

/**
 * 分组卡片：白底、10dp 圆角、裁剪内部内容。
 *
 * 行之间的分割线由调用方用 [AnchorHairline] 自己放在「需要分割」的位置
 * （最后一行后面不该有线，交给调用方判断比组件猜更可靠）。
 */
@Composable
fun AnchorListGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(AnchorTheme.colors.cardBackground),
        content = content,
    )
}

/**
 * 细分割线：0.5dp（HIG 的 hairline），默认从行内容起始处（16dp）内缩。
 *
 * @param inset 左侧内缩量；跨整组的线传 `0.dp`
 */
@Composable
fun AnchorHairline(
    modifier: Modifier = Modifier,
    inset: Dp = 16.dp,
    color: Color = AnchorTheme.colors.separator,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = inset)
            .height(Dp.Hairline)
            .background(color),
    )
}

/**
 * 分组列表的一行。
 *
 * @param title 主文案
 * @param subtitle 副文案（15sp 二级灰，iOS 的两行样式）
 * @param value 右侧值（如时间、次数），与标题同一档字号的二级灰
 * @param leadingIcon 左侧图标（可选）
 * @param trailingContent 右侧自定内容（开关、勾选、徽章）
 * @param showChevron 是否显示「>」，点了会进下一页的行才显示
 * @param onClick 为 null 时是纯展示行，不上报点击语义
 */
@Composable
fun AnchorListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    leadingIcon: ImageVector? = null,
    leadingTint: Color = AnchorTheme.colors.tint,
    trailingContent: (@Composable () -> Unit)? = null,
    showChevron: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val colors = AnchorTheme.colors
    val rowContent: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 44.dp)
                .padding(horizontal = 16.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = leadingTint,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = AnchorType.body, color = colors.label)
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = AnchorType.subheadline,
                        color = colors.labelSecondary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            if (value != null) {
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = value, style = AnchorType.body, color = colors.labelSecondary)
            }
            if (trailingContent != null) {
                Spacer(modifier = Modifier.width(12.dp))
                trailingContent()
            }
            if (showChevron) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.labelTertiary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }

    if (onClick == null) {
        Box(modifier = modifier.semantics(mergeDescendants = true) {}) { rowContent() }
    } else {
        AnchorClickableSurface(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = RoundedCornerShape(0.dp),
            role = null,
        ) {
            // 行内自己带 minHeight 与留白；外层容器只管按压反馈与点击。
            rowContent()
        }
    }
}

/**
 * 带开关的分组行：整行可点（点行 = 拨开关），开关本身也能单独拨。
 *
 * @param onCheckedChange 回调里已经带过轻触感反馈（由 [AnchorSwitch] 负责）
 */
@Composable
fun AnchorSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    val colors = AnchorTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 44.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = AnchorType.body,
                color = if (enabled) colors.label else colors.labelTertiary,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = AnchorType.subheadline,
                    color = colors.labelSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        AnchorSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}
