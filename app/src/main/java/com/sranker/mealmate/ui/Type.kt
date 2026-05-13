package com.sranker.mealmate.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Outfit font family.
 *
 * TO BUNDLE THE OUTFIT FONT:
 * 1. Download the TTF files from https://fonts.google.com/specimen/Outfit
 * 2. Place them in app/src/main/res/font/ as:
 *    - outfit_regular.ttf
 *    - outfit_medium.ttf
 *    - outfit_bold.ttf
 * 3. Uncomment the [outfitFontFamily] definition and update [appFontFamily] to use it.
 * 4. Delete this comment block.
 *
 * Until then, the default system font (Roboto on most devices) is used.
 */
private val appFontFamily = FontFamily.Default

/*
 * Once TTF files are added to res/font/, replace the line above with:
 *
 * private val appFontFamily = FontFamily(
 *     Font(R.font.outfit_regular, weight = FontWeight.Normal),
 *     Font(R.font.outfit_medium, weight = FontWeight.Medium),
 *     Font(R.font.outfit_bold, weight = FontWeight.Bold)
 * )
 */

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
        fontFamily = appFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = (57 * multiplier).sp,
        lineHeight = (64 * multiplier).sp,
        letterSpacing = (-0.25 * multiplier).sp
    ),
    displayMedium = TextStyle(
        fontFamily = appFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = (45 * multiplier).sp,
        lineHeight = (52 * multiplier).sp
    ),
    displaySmall = TextStyle(
        fontFamily = appFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = (36 * multiplier).sp,
        lineHeight = (44 * multiplier).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = appFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = (32 * multiplier).sp,
        lineHeight = (40 * multiplier).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = appFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = (28 * multiplier).sp,
        lineHeight = (36 * multiplier).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = appFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = (24 * multiplier).sp,
        lineHeight = (32 * multiplier).sp
    ),
    titleLarge = TextStyle(
        fontFamily = appFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = (22 * multiplier).sp,
        lineHeight = (28 * multiplier).sp
    ),
    titleMedium = TextStyle(
        fontFamily = appFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = (16 * multiplier).sp,
        lineHeight = (24 * multiplier).sp,
        letterSpacing = (0.15 * multiplier).sp
    ),
    titleSmall = TextStyle(
        fontFamily = appFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = (14 * multiplier).sp,
        lineHeight = (20 * multiplier).sp,
        letterSpacing = (0.1 * multiplier).sp
    ),
    bodyLarge = TextStyle(
        fontFamily = appFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = (16 * multiplier).sp,
        lineHeight = (24 * multiplier).sp,
        letterSpacing = (0.5 * multiplier).sp
    ),
    bodyMedium = TextStyle(
        fontFamily = appFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = (14 * multiplier).sp,
        lineHeight = (20 * multiplier).sp,
        letterSpacing = (0.25 * multiplier).sp
    ),
    bodySmall = TextStyle(
        fontFamily = appFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = (12 * multiplier).sp,
        lineHeight = (16 * multiplier).sp,
        letterSpacing = (0.4 * multiplier).sp
    ),
    labelLarge = TextStyle(
        fontFamily = appFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = (14 * multiplier).sp,
        lineHeight = (20 * multiplier).sp,
        letterSpacing = (0.1 * multiplier).sp
    ),
    labelMedium = TextStyle(
        fontFamily = appFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = (12 * multiplier).sp,
        lineHeight = (16 * multiplier).sp,
        letterSpacing = (0.5 * multiplier).sp
    ),
    labelSmall = TextStyle(
        fontFamily = appFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = (11 * multiplier).sp,
        lineHeight = (16 * multiplier).sp,
        letterSpacing = (0.5 * multiplier).sp
    )
)
