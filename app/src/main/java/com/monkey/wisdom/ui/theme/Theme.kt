package com.monkey.wisdom.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = BrandPrimaryHover,
    onPrimaryContainer = Color.White,
    secondary = BrandPrimaryHover,
    onSecondary = Color.White,
    error = BrandDanger,
    onError = Color.White,
    background = BgPage,
    onBackground = TextPrimary,
    surface = BgCard,
    onSurface = TextPrimary,
    surfaceVariant = BgPage,
    onSurfaceVariant = TextSecondary,
    outline = BorderLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = BrandPrimaryHover,
    onPrimaryContainer = Color.White,
    secondary = BrandPrimaryHover,
    onSecondary = Color.White,
    error = BrandDanger,
    onError = Color.White,
    background = Color(0xFF111827),
    onBackground = Color(0xFFF9FAFB),
    surface = Color(0xFF1F2937),
    onSurface = Color(0xFFF9FAFB),
    surfaceVariant = Color(0xFF374151),
    onSurfaceVariant = Color(0xFFD1D5DB),
    outline = Color(0xFF4B5563),
)

/**
 * 全局主题：使用固定品牌色（不启用动态取色），保证与 Web 端和设计稿一致。
 */
@Composable
fun WisdomTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
