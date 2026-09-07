package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlueLight,
    onPrimary = Color(0xFF08111F),
    primaryContainer = Color(0xFF162544),
    onPrimaryContainer = TextPrimaryDark,
    secondary = GrowthGreenDarkTheme,
    onSecondary = Color.Black,
    error = AlertRedDarkTheme,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = Color(0xFF15223D),
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderSubtleDark,
    outlineVariant = BorderSubtleDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEBF2FF),
    onPrimaryContainer = FinoraNavy,
    secondary = GrowthGreen,
    onSecondary = Color.White,
    secondaryContainer = GrowthGreenLight,
    onSecondaryContainer = GrowthGreenDark,
    error = AlertRed,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFF1F5FA),
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderSubtle,
    outlineVariant = BorderSubtle
)

@Composable
fun FinoraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Use FinoraNavy for a seamless top bar in light mode, BackgroundDark in dark mode
            window.statusBarColor = if (darkTheme) BackgroundDark.toArgb() else FinoraNavy.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

