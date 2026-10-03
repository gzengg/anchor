package com.anchor.recovery.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// 磐石主色：偏冷的石青 + 稳重的深灰，避免高饱和色带来的刺激感。
private val AnchorBlue = Color(0xFF2E6F73)
private val AnchorBlueLight = Color(0xFF8FD3D6)
private val AnchorSand = Color(0xFF8C6D46)

private val LightColors = lightColorScheme(
    primary = AnchorBlue,
    secondary = AnchorSand,
)

private val DarkColors = darkColorScheme(
    primary = AnchorBlueLight,
    secondary = AnchorSand,
)

@Composable
fun AnchorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(colorScheme = colorScheme, content = content)
}
