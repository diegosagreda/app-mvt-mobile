package com.example.mvt.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    secondary = AccentRed,
    onSecondary = Color.White,
    background = DarkMvtColors.background,
    onBackground = DarkMvtColors.textPrimary,
    surface = DarkMvtColors.surface,
    onSurface = DarkMvtColors.textPrimary,
    surfaceVariant = DarkMvtColors.surfaceAlt,
    onSurfaceVariant = DarkMvtColors.textSecondary,
    outline = DarkMvtColors.border,
    error = AppError,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    secondary = AccentRed,
    onSecondary = Color.White,
    background = LightMvtColors.background,
    onBackground = LightMvtColors.textPrimary,
    surface = LightMvtColors.surface,
    onSurface = LightMvtColors.textPrimary,
    surfaceVariant = LightMvtColors.surfaceAlt,
    onSurfaceVariant = LightMvtColors.textSecondary,
    outline = LightMvtColors.border,
    error = AppError,
    onError = Color.White
)

enum class AppThemeMode(val storageKey: String) {
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromStorageKey(value: String?): AppThemeMode {
            return entries.firstOrNull { it.storageKey == value } ?: DARK
        }
    }
}

@Composable
fun MVTTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    darkTheme: Boolean = themeMode == AppThemeMode.DARK,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val appColors = if (darkTheme) DarkMvtColors else LightMvtColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalMvtColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
