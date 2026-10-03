package com.anchor.recovery.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/*
 * iOS 字阶（HIG Typography：largeTitle 34 / title1 28 / title2 22 / title3 20 /
 * headline·body 17 / callout 16 / subheadline 15 / footnote 13 / caption1 12 / caption2 11）。
 *
 * 为什么不直接改 MaterialTheme.typography：M3 的槽位尺寸（bodyLarge 16、bodyMedium 14、
 * labelLarge 14 …）被现有页面到处引用，整体替换会让所有未改版页面同时长 1–3sp，
 * 在没法逐页过真机的情况下属于盲改。这里作为独立字阶提供，由 P1-2 逐页切入：
 * 改完的页面统一用 AnchorType 内部自洽，没改的页面保持原样，收口过程可分段验证。
 *
 * 字体不打包：San Francisco 有版权，用系统默认字体，只对齐字号/字重/行高/字距。
 */
object AnchorType {
    val largeTitle = TextStyle(
        fontSize = 34.sp,
        lineHeight = 41.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.37.sp,
    )
    val title1 = TextStyle(
        fontSize = 28.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.36.sp,
    )
    val title2 = TextStyle(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.35.sp,
    )
    val title3 = TextStyle(
        fontSize = 20.sp,
        lineHeight = 25.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.38.sp,
    )
    val headline = TextStyle(
        fontSize = 17.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.41).sp,
    )
    val body = TextStyle(
        fontSize = 17.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = (-0.41).sp,
    )
    val callout = TextStyle(
        fontSize = 16.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = (-0.32).sp,
    )
    val subheadline = TextStyle(
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = (-0.24).sp,
    )
    val subheadlineSemibold = subheadline.copy(fontWeight = FontWeight.SemiBold)
    val footnote = TextStyle(
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = (-0.08).sp,
    )
    val footnoteSemibold = footnote.copy(fontWeight = FontWeight.SemiBold)
    val caption1 = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
    )
    val caption2 = TextStyle(
        fontSize = 11.sp,
        lineHeight = 13.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.07.sp,
    )
}
