package com.musicprayer.vibematch.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

data class AppThemeOption(
    val name: String, val dark: Boolean, val background: Color, val surface: Color,
    val primary: Color, val secondary: Color, val onBackground: Color,
)

val AppThemes = listOf(
    AppThemeOption("EVA-01", true, Color(0xFF09090B), Color(0xFF18151F), Color(0xFF8B5CF6), Color(0xFF7CFF4F), Color.White),
    AppThemeOption("Pipeline Punch", false, Color(0xFFFFF3F8), Color.White, Color(0xFFFF4F9A), Color(0xFF5C173B), Color(0xFF321023)),
    AppThemeOption("Ocean Drive", true, Color(0xFF07141B), Color(0xFF102631), Color(0xFF29D3E2), Color(0xFF8BE9FD), Color.White),
    AppThemeOption("Solar Flare", true, Color(0xFF160D08), Color(0xFF2B1A11), Color(0xFFFF7A1A), Color(0xFFFFD166), Color.White),
)

@Composable fun MusicPrayerTheme(themeName: String, content: @Composable () -> Unit) {
    val theme = AppThemes.firstOrNull { it.name == themeName } ?: AppThemes.first()
    val colors = if (theme.dark) darkColorScheme(
        primary = theme.primary, secondary = theme.secondary, background = theme.background,
        surface = theme.surface, onBackground = theme.onBackground, onSurface = theme.onBackground,
    ) else lightColorScheme(
        primary = theme.primary, secondary = theme.secondary, background = theme.background,
        surface = theme.surface, onBackground = theme.onBackground, onSurface = theme.onBackground,
    )
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        window.statusBarColor = theme.background.toArgb(); window.navigationBarColor = theme.background.toArgb()
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !theme.dark
            isAppearanceLightNavigationBars = !theme.dark
        }
    }
    MaterialTheme(colorScheme = colors, content = content)
}
