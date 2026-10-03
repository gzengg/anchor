package com.anchor.recovery.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/*
 * 磐石主题（v2：iOS 风格）。
 *
 * 取舍一：不再跟随系统动态取色（Material You / Monet）。
 * 1. HIG 的观感来自固定语义色（分组底、卡片白、细分割线），壁纸取色会把品牌主色与
 *    中性色打散，改出来的东西既不像 iOS 也不像原来的磐石；
 * 2. P1 之所以敢整体改版，靠的是 P1-0/P1-3 的截图基线逐页对比；取色会让每台机器的
 *    基线都不同，等于把安全网撤了；
 * 3. 顺带消掉一类历史问题：windowBackground 是资源层静态值，取色时冷启动首帧必然与
 *    页面底色不一致，只能靠 MainActivity 起来后补一刀。现在 values/colors.xml 的
 *    anchor_background 与 LightColors.background 恒等，首帧就是对的。
 * 代价：用户失去「跟随壁纸」的个性化，记在交付说明的已知限制里。
 *
 * 取舍二：底层组件仍是 Material3（改样式不改行为），只有按钮/开关/分段控件这类
 * HIG 形状差异太大的自绘，避免重写手势、焦点与无障碍行为。
 */

/** iOS 圆角：卡片 10–16dp、大标题页的分组卡 10dp、弹窗 14dp、底部面板 24dp（顶部两角）。 */
private val AnchorShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

private val LightColors = lightColorScheme(
    primary = AnchorTeal,
    onPrimary = Color.White,
    primaryContainer = AnchorTealPale,
    onPrimaryContainer = AnchorTealDeep,
    inversePrimary = AnchorTealLight,
    secondary = AnchorSand,
    onSecondary = Color.White,
    secondaryContainer = AnchorSandPale,
    onSecondaryContainer = Color(0xFF3A2A12),
    tertiary = Color(0xFF4E6350),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD0E8D2),
    onTertiaryContainer = Color(0xFF0B1F12),
    background = LightAnchorColors.groupedBackground,
    onBackground = LightAnchorColors.label,
    surface = LightAnchorColors.cardBackground,
    onSurface = LightAnchorColors.label,
    surfaceVariant = Color(0xFFEFEDE9),
    onSurfaceVariant = Color(0xFF53535A),
    // iOS 卡片是平的，不要 M3 的高程染色（否则白卡会被主色染上一层青）。
    surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFF1C1C1E),
    inverseOnSurface = Color.White,
    error = LightAnchorColors.danger,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF7A0B05),
    outline = LightAnchorColors.opaqueSeparator,
    outlineVariant = Color(0xFFE5E5EA),
    scrim = Color(0xFF000000),
    surfaceBright = Color.White,
    surfaceDim = Color(0xFFE7E4E0),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = LightAnchorColors.groupedBackground,
    surfaceContainerHighest = Color(0xFFEAE8E4),
)

private val DarkColors = darkColorScheme(
    primary = AnchorTealLight,
    onPrimary = Color(0xFF00363A),
    primaryContainer = Color(0xFF114F53),
    onPrimaryContainer = Color(0xFFAFEFF2),
    inversePrimary = AnchorTeal,
    secondary = Color(0xFFD9C6A5),
    onSecondary = Color(0xFF3B2C13),
    secondaryContainer = Color(0xFF54422A),
    onSecondaryContainer = Color(0xFFF3E2C4),
    tertiary = Color(0xFFB6CCB8),
    onTertiary = Color(0xFF213527),
    tertiaryContainer = Color(0xFF374B3C),
    onTertiaryContainer = Color(0xFFD2E8D4),
    background = DarkAnchorColors.groupedBackground,
    onBackground = DarkAnchorColors.label,
    surface = DarkAnchorColors.cardBackground,
    onSurface = DarkAnchorColors.label,
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFFB8B8BE),
    surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFFF2F2F7),
    inverseOnSurface = Color(0xFF1C1C1E),
    error = DarkAnchorColors.danger,
    onError = Color.White,
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF545458),
    outlineVariant = Color(0xFF38383A),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF2C2C2E),
    surfaceDim = Color(0xFF0A0A0A),
    surfaceContainerLowest = Color(0xFF000000),
    surfaceContainerLow = Color(0xFF1C1C1E),
    surfaceContainer = Color(0xFF1C1C1E),
    surfaceContainerHigh = Color(0xFF2C2C2E),
    surfaceContainerHighest = Color(0xFF38383A),
)

/**
 * 当前生效的配色方案。
 *
 * 非 @Composable：启动阶段（Compose 首帧之前）MainActivity 要用同一份逻辑设置窗口底色，
 * 保证「windowBackground → 首帧」不闪色。浅色值必须与 `values/colors.xml` 的
 * `anchor_background` 一致，深色值与其 `values-night` 版本一致，改了这里就要同步改那边。
 */
fun anchorColorScheme(darkTheme: Boolean): ColorScheme = if (darkTheme) DarkColors else LightColors

@Composable
fun AnchorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalAnchorColors provides if (darkTheme) DarkAnchorColors else LightAnchorColors,
    ) {
        MaterialTheme(
            colorScheme = anchorColorScheme(darkTheme),
            shapes = AnchorShapes,
            content = content,
        )
    }
}

/** 取 iOS 语义色与字阶：`AnchorTheme.colors.labelSecondary`、`AnchorTheme.type.body`。 */
object AnchorTheme {
    val colors: AnchorColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAnchorColors.current

    val type: AnchorType
        @Composable
        @ReadOnlyComposable
        get() = AnchorType
}
