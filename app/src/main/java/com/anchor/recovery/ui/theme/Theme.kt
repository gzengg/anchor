package com.anchor.recovery.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/*
 * 磐石配色：Android 12+（API 31+）跟随系统动态取色（Material You / Monet），
 * 其余版本回落到固定品牌色板（偏冷的石青 + 暖沙，避免高饱和带来的刺激感）。
 *
 * 动态取色的取舍：
 * 1. 系统启动画面与 windowBackground 都是资源层静态值，在进程起来之前就解析完了，
 *    冷启动首帧无法跟随壁纸，只能选中性色把色差压到最小；进程起来后由 MainActivity
 *    立刻把窗口底色改成当前方案色，首帧之后的窗口底色与页面保持一致。
 * 2. 启动图标底色同理保持品牌石青；想让图标跟随壁纸请走系统的「主题图标」，
 *    我们的自适应图标已提供 monochrome 层。
 * 3. 语义色不参与取色：error/errorContainer 由 M3 固定 error 色板给定（破戒记录用它），
 *    知识库可信度徽章按提示词固定「高=绿/中=黄/低=灰」。
 */

/*
 * 回落色板（API < 31 或取色不可用时使用）：
 * `background` 必须与 res/values/colors.xml 的 `anchor_background`
 * （以及 values-night 的夜间值）一致，否则冷启动会闪一下不同颜色。
 */

// 石青（主色）
private val Teal = Color(0xFF2E6F73)
private val TealDeep = Color(0xFF0B3B3E)
private val TealPale = Color(0xFFCFE9E9)
private val TealLight = Color(0xFF8FD3D6)

// 暖沙（次色）
private val Sand = Color(0xFF8C6D46)
private val SandPale = Color(0xFFF1E4CE)

// 暖白纸与墨色
private val Paper = Color(0xFFFAF6F0)
private val Ink = Color(0xFF1B1C1C)
private val InkSoft = Color(0xFF404A49)

private val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = TealPale,
    onPrimaryContainer = TealDeep,
    secondary = Sand,
    onSecondary = Color.White,
    secondaryContainer = SandPale,
    onSecondaryContainer = Color(0xFF3A2A12),
    tertiary = Color(0xFF4E6350),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD0E8D2),
    onTertiaryContainer = Color(0xFF0B1F12),
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Color(0xFFDCE5E3),
    onSurfaceVariant = InkSoft,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF4EFE8),
    surfaceContainer = Color(0xFFEFEAE2),
    surfaceContainerHigh = Color(0xFFE9E4DC),
    surfaceContainerHighest = Color(0xFFE3DED6),
    outline = Color(0xFF707977),
    outlineVariant = Color(0xFFC0C9C7),
    inverseSurface = Color(0xFF2F3130),
    inverseOnSurface = Color(0xFFF1F1EF),
    inversePrimary = TealLight,
    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
)

private val DarkColors = darkColorScheme(
    primary = TealLight,
    onPrimary = Color(0xFF00363A),
    primaryContainer = Color(0xFF114F53),
    onPrimaryContainer = Color(0xFFAFEFF2),
    secondary = Color(0xFFD9C6A5),
    onSecondary = Color(0xFF3B2C13),
    secondaryContainer = Color(0xFF54422A),
    onSecondaryContainer = Color(0xFFF3E2C4),
    tertiary = Color(0xFFB6CCB8),
    onTertiary = Color(0xFF213527),
    tertiaryContainer = Color(0xFF374B3C),
    onTertiaryContainer = Color(0xFFD2E8D4),
    background = Color(0xFF101413),
    onBackground = Color(0xFFE2E3E1),
    surface = Color(0xFF101413),
    onSurface = Color(0xFFE2E3E1),
    surfaceVariant = Color(0xFF3F4948),
    onSurfaceVariant = Color(0xFFBFC8C7),
    surfaceContainerLowest = Color(0xFF0A0F0E),
    surfaceContainerLow = Color(0xFF181D1C),
    surfaceContainer = Color(0xFF1C2120),
    surfaceContainerHigh = Color(0xFF262B2A),
    surfaceContainerHighest = Color(0xFF313635),
    outline = Color(0xFF899392),
    outlineVariant = Color(0xFF3F4948),
    inverseSurface = Color(0xFFE2E3E1),
    inverseOnSurface = Color(0xFF2F3130),
    inversePrimary = Teal,
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
)

/**
 * 当前生效的配色方案：Android 12+ 跟随壁纸，其余版本用品牌色板。
 *
 * 非 @Composable：启动阶段（Compose 首帧之前）要用同一份逻辑设置窗口底色。
 */
fun anchorColorScheme(context: Context, darkTheme: Boolean): ColorScheme = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

    darkTheme -> DarkColors
    else -> LightColors
}

@Composable
fun AnchorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = anchorColorScheme(LocalContext.current, darkTheme),
        content = content,
    )
}
