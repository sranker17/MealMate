package com.sranker.mealmate

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * The [Application] class for MealMate.
 * Annotated with [HiltAndroidApp] to trigger Hilt's code generation.
 */
@HiltAndroidApp
class MealMateApp : Application()
