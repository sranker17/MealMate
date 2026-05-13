package com.sranker.mealmate.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Formats a [Date] into a Hungarian date string suitable for default menu titles.
 *
 * @return A string like "2025. 03. 05." using the device locale.
 */
fun formatDateTitle(date: Date = Date()): String =
    SimpleDateFormat("yyyy. MM. dd.", Locale.getDefault()).format(date)

