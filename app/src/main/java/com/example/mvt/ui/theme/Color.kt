package com.example.mvt.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val DarkSidebar = Color(0xFF1E2633)
val BackgroundGreyDark = Color(0xFF283243)
val BorderDark = Color(0xFF35435A)
val PrimaryBlue = Color(0xFF0066CC)
val DarkText = Color(0xFFA2A5B9)
val AccentRed = Color(0xFFFF42B4)
val AppError = Color(0xFFE5484D)

val AppSuccess = Color(0xFF32D296)

@Immutable
data class MvtColors(
    val background: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val surfaceAlt: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val iconMuted: Color,
    val primarySoft: Color,
    val athleteNavigation: Color,
    val goalsSummary: Color
)

val DarkMvtColors = MvtColors(
    background = DarkSidebar,
    surface = BackgroundGreyDark,
    surfaceMuted = Color(0xFF222C3A),
    surfaceAlt = Color(0xFF2D394B),
    border = BorderDark,
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = DarkText,
    iconMuted = Color(0xFF7F8AA3),
    primarySoft = Color(0x332C7BE5),
    athleteNavigation = Color(0xFF63C7FF),
    goalsSummary = Color(0xFF293B9B)
)

val LightMvtColors = MvtColors(
    background = Color(0xFFF5F7FB),
    surface = Color(0xFFFFFFFF),
    surfaceMuted = Color(0xFFF0F4FA),
    surfaceAlt = Color(0xFFEAF0F8),
    border = Color(0xFFD8E0EC),
    textPrimary = Color(0xFF172033),
    textSecondary = Color(0xFF65718A),
    iconMuted = Color(0xFF7C879A),
    primarySoft = Color(0x1F0066CC),
    athleteNavigation = Color(0xFF168AB5),
    goalsSummary = Color(0xFF394FE3)
)

val LocalMvtColors = staticCompositionLocalOf { DarkMvtColors }

val AthleteNavigationBlue: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalMvtColors.current.athleteNavigation

val AppBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalMvtColors.current.background

val AppSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalMvtColors.current.surface

val AppSurfaceMuted: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalMvtColors.current.surfaceMuted

val AppSurfaceAlt: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalMvtColors.current.surfaceAlt

val AppBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalMvtColors.current.border

val AppTextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalMvtColors.current.textPrimary

val AppTextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalMvtColors.current.textSecondary

val AppIconMuted: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalMvtColors.current.iconMuted

val AppPrimarySoft: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalMvtColors.current.primarySoft

val GoalsSummaryBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalMvtColors.current.goalsSummary
