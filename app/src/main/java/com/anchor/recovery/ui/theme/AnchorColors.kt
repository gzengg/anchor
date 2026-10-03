package com.anchor.recovery.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/*
 * iOS 语义色：Material3 的 ColorScheme 表达不了 HIG 里的这几档
 * （分组背景 / 卡片背景 / 细分割线 / 二级三级文字 / 填充灰 / 系统强调色），
 * 所以单开一份 AnchorColors，由 AnchorTheme 通过 CompositionLocal 下发。
 *
 * 取值原则：
 * 1. 结构性颜色直接照抄 HIG 的 system* 系列（systemGroupedBackground、
 *    secondarySystemGroupedBackground、separator、systemFill、systemRed/Green/Orange…），
 *    这些是「像不像 iOS」的主要来源。
 * 2. 品牌石青（锚色）继续当主色/交互色，不换成 systemBlue：v1 定的冷色低饱和调性
 *    有明确的产品理由（避免刺激感），HIG 对齐的是结构与尺寸，不是把品牌色也交出去。
 * 3. 文字灰比 HIG 原值深一档：iOS 的 secondaryLabel（60% 黑）在白底只有约 3.5:1，
 *    落到 17sp 正文上不达标；这里取 4.6:1 以上的灰，视觉档位保留、对比度不降。
 */

// 品牌色：石青（主色）与暖沙（次色），沿用 v1，保证品牌连续性。
internal val AnchorTeal = Color(0xFF2E6F73)
internal val AnchorTealDeep = Color(0xFF0B3B3E)
internal val AnchorTealPale = Color(0xFFCFE9E9)
internal val AnchorTealLight = Color(0xFF8FD3D6)

internal val AnchorSand = Color(0xFF8C6D46)
internal val AnchorSandPale = Color(0xFFF1E4CE)

// HIG System Colors（浅色 / 深色）
private val SystemRedLight = Color(0xFFFF3B30)
private val SystemRedDark = Color(0xFFFF453A)
private val SystemOrangeLight = Color(0xFFFF9500)
private val SystemOrangeDark = Color(0xFFFF9F0A)
private val SystemGreenLight = Color(0xFF34C759)
private val SystemGreenDark = Color(0xFF30D158)

/**
 * iOS 语义色集合。
 *
 * @property groupedBackground 分组列表页底色（HIG systemGroupedBackground）
 * @property cardBackground 分组卡片底色（HIG secondarySystemGroupedBackground）
 * @property separator 细分割线（0.5dp；HIG separator）
 * @property opaqueSeparator 不透明分割线，给需要与背景叠加的边框用
 * @property label 一级文字
 * @property labelSecondary 二级文字（副标题、说明）
 * @property labelTertiary 三级文字（占位符、极弱提示）
 * @property fill 填充灰（分段控件轨道、输入框底）
 * @property fillStrong 更重一档的填充灰（按压态、分组表头）
 * @property tint 交互色（= 品牌主色）
 * @property success 成功/已达成
 * @property warning 提醒/注意
 * @property danger 危险/破坏性操作
 * @property isLight 当前是浅色方案
 */
@Immutable
data class AnchorColors(
    val groupedBackground: Color,
    val cardBackground: Color,
    val separator: Color,
    val opaqueSeparator: Color,
    val label: Color,
    val labelSecondary: Color,
    val labelTertiary: Color,
    val fill: Color,
    val fillStrong: Color,
    val tint: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val isLight: Boolean,
)

internal val LightAnchorColors = AnchorColors(
    groupedBackground = Color(0xFFF5F2EE),
    cardBackground = Color(0xFFFFFFFF),
    separator = Color(0x4A3C3C43),
    opaqueSeparator = Color(0xFFC6C6C8),
    label = Color(0xFF000000),
    labelSecondary = Color(0xFF6C6C70),
    labelTertiary = Color(0xFF8E8E93),
    fill = Color(0x29787880),
    fillStrong = Color(0x33787880),
    tint = AnchorTeal,
    success = SystemGreenLight,
    warning = SystemOrangeLight,
    danger = SystemRedLight,
    isLight = true,
)

internal val DarkAnchorColors = AnchorColors(
    groupedBackground = Color(0xFF000000),
    cardBackground = Color(0xFF1C1C1E),
    separator = Color(0xA6545458),
    opaqueSeparator = Color(0xFF38383A),
    label = Color(0xFFFFFFFF),
    labelSecondary = Color(0xFF98989D),
    labelTertiary = Color(0xFF636366),
    fill = Color(0x52787880),
    fillStrong = Color(0x5C787880),
    tint = AnchorTealLight,
    success = SystemGreenDark,
    warning = SystemOrangeDark,
    danger = SystemRedDark,
    isLight = false,
)

/** 当前生效的 iOS 语义色。默认值只是兜底，实际永远由 [AnchorTheme] 覆盖。 */
internal val LocalAnchorColors = staticCompositionLocalOf { LightAnchorColors }
