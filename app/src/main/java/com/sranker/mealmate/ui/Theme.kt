package com.sranker.mealmate.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Creates a dark [darkColorScheme] using the given accent primary color.
 * The background/surface use [MinimalistBackground], and the on-colors
 * are derived from the accent with reduced opacity for a minimalist look.
 */
private fun darkColorScheme(primary: Color) = darkColorScheme(
    primary = primary,
    onPrimary = Color.White,
    primaryContainer = primary.copy(alpha = 0.15f),
    onPrimaryContainer = primary,
    secondary = primary.copy(alpha = 0.7f),
    onSecondary = Color.White,
    background = MinimalistBackground,
    onBackground = primary.copy(alpha = 0.9f),
    surface = MinimalistBackground,
    onSurface = primary.copy(alpha = 0.9f),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = primary.copy(alpha = 0.7f),
    outline = primary.copy(alpha = 0.3f),
    outlineVariant = primary.copy(alpha = 0.12f)
)

/**
 * Creates a light [lightColorScheme] using the given accent primary color.
 */
private fun lightColorScheme(primary: Color) = lightColorScheme(
    primary = primary,
    onPrimary = Color.White,
    primaryContainer = primary.copy(alpha = 0.15f),
    onPrimaryContainer = primary,
    secondary = primary.copy(alpha = 0.7f),
    onSecondary = Color.White,
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF1C1C1C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1C1C),
    surfaceVariant = Color(0xFFF0F0F0),
    onSurfaceVariant = Color(0xFF494949),
    outline = Color(0xFFD0D0D0),
    outlineVariant = Color(0xFFE8E8E8)
)

/**
 * MealMate theme composable.
 *
 * Applies the selected [accentColor], [isDarkTheme] mode, and [fontSizeScale]
 * to the entire composable tree via [MaterialTheme].
 *
 * @param accentColor The accent color scheme (defaults to [AccentColor.Teal]).
 * @param isDarkTheme Whether to use the dark or light color scheme.
 * @param fontSizeScale Font size multiplier (1.0 = default).
 * @param content The composable content to wrap with the theme.
 */
@Composable
fun MealMateTheme(
    accentColor: AccentColor = AccentColor.Teal,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    fontSizeScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val colorScheme = if (isDarkTheme) {
        darkColorScheme(primary = accentColor.darkPrimary)
    } else {
        lightColorScheme(primary = accentColor.lightPrimary)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = getTypographyWithMultiplier(fontSizeScale),
        content = content
    )
}
