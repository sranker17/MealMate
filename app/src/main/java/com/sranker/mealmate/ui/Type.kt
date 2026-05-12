package com.sranker.mealmate.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Default system font family.
 * Replace with Outfit bundled fonts once [R.font.outfit_regular] et al. are added to res/font/.
 */
private val defaultFontFamily = FontFamily.Default

/** Default typography at 1.0x scale. */
val defaultTypography = getTypographyWithMultiplier(1.0f)

/**
 * Returns a [Typography] with all type scales scaled by the given [multiplier].
 *
 * @param multiplier Font size scale factor (e.g. 1.0 = default, 1.15 = large, 1.3 = extra large).
 * @return A [Typography] instance with scaled font sizes.
 */
fun getTypographyWithMultiplier(multiplier: Float): Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = defaultFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = (57 * multiplier).sp,
        lineHeight = (64 * multiplier).sp,
        letterSpacing = (-0.25 * multiplier).sp
    ),
    displayMedium = TextStyle(
        fontFamily = defaultFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = (45 * multiplier).sp,
        lineHeight = (52 * multiplier).sp
    ),
    displaySmall = TextStyle(
        fontFamily = defaultFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = (36 * multiplier).sp,
        lineHeight = (44 * multiplier).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = defaultFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = (32 * multiplier).sp,
        lineHeight = (40 * multiplier).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = defaultFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = (28 * multiplier).sp,
        lineHeight = (36 * multiplier).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = defaultFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = (24 * multiplier).sp,
        lineHeight = (32 * multiplier).sp
    ),
    titleLarge = TextStyle(
        fontFamily = defaultFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = (22 * multiplier).sp,
        lineHeight = (28 * multiplier).sp
    ),
    titleMedium = TextStyle(
        fontFamily = defaultFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = (16 * multiplier).sp,
        lineHeight = (24 * multiplier).sp,
        letterSpacing = (0.15 * multiplier).sp
    ),
    titleSmall = TextStyle(
        fontFamily = defaultFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = (14 * multiplier).sp,
        lineHeight = (20 * multiplier).sp,
        letterSpacing = (0.1 * multiplier).sp
    ),
    bodyLarge = TextStyle(
        fontFamily = defaultFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = (16 * multiplier).sp,
        lineHeight = (24 * multiplier).sp,
        letterSpacing = (0.5 * multiplier).sp
    ),
    bodyMedium = TextStyle(
        fontFamily = defaultFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = (14 * multiplier).sp,
        lineHeight = (20 * multiplier).sp,
        letterSpacing = (0.25 * multiplier).sp
    ),
    bodySmall = TextStyle(
        fontFamily = defaultFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = (12 * multiplier).sp,
        lineHeight = (16 * multiplier).sp,
        letterSpacing = (0.4 * multiplier).sp
    ),
    labelLarge = TextStyle(
        fontFamily = defaultFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = (14 * multiplier).sp,
        lineHeight = (20 * multiplier).sp,
        letterSpacing = (0.1 * multiplier).sp
    ),
    labelMedium = TextStyle(
        fontFamily = defaultFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = (12 * multiplier).sp,
        lineHeight = (16 * multiplier).sp,
        letterSpacing = (0.5 * multiplier).sp
    ),
    labelSmall = TextStyle(
        fontFamily = defaultFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = (11 * multiplier).sp,
        lineHeight = (16 * multiplier).sp,
        letterSpacing = (0.5 * multiplier).sp
    )
)
