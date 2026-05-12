package com.sranker.mealmate.ui

import androidx.compose.ui.graphics.Color

// region Dark Theme Base
/** Primary dark background used across all dark themes. */
val MinimalistBackground = Color(0xFF151515)

// endregion

// region Accent Colors (Primary)
/** Teal/ocean accent — default dark theme primary. */
val OceanTeal = Color(0xFF14B8A6)
/** Green/forest accent. */
val ForestGreen = Color(0xFF4ADE80)
/** Pink/sunset accent. */
val SunsetPink = Color(0xFFD34273)
/** Slate/grey accent. */
val SnowSlate = Color(0xFF94A3B8)

// endregion

// region Light Theme Colors
val SkyPrimary = Color(0xFF7DD3FC)
val SkyBackground = Color(0xFFF8FAFC)
val RosePrimary = Color(0xFFFDA4AF)
val RoseBackground = Color(0xFFFFF1F2)
val SandPrimary = Color(0xFFD4D4D8)
val SandBackground = Color(0xFFFAFAFA)

// endregion

/**
 * Sealed class representing the available accent color options.
 * Maps each accent to its dark and light theme primary color.
 */
sealed class AccentColor(
    val darkPrimary: Color,
    val lightPrimary: Color,
    val labelResId: String
) {
    data object Teal : AccentColor(OceanTeal, OceanTeal, "teal")
    data object Green : AccentColor(ForestGreen, ForestGreen, "green")
    data object Pink : AccentColor(SunsetPink, SunsetPink, "pink")
    data object Slate : AccentColor(SnowSlate, SnowSlate, "slate")
    data object Sky : AccentColor(SkyPrimary, SkyPrimary, "sky")
    data object Rose : AccentColor(RosePrimary, RosePrimary, "rose")
    data object Sand : AccentColor(SandPrimary, SandPrimary, "sand")
}

