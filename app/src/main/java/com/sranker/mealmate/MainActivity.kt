package com.sranker.mealmate

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.sranker.mealmate.data.SettingsRepository
import com.sranker.mealmate.navigation.AppNavGraph
import com.sranker.mealmate.navigation.MainScaffold
import com.sranker.mealmate.ui.AccentColor
import com.sranker.mealmate.ui.MealMateTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun attachBaseContext(newBase: Context) {
        val language = pendingLanguage
        val context = if (language != null && language != "system") {
            val locale = Locale.forLanguageTag(language)
            Locale.setDefault(locale)
            val config = Configuration(newBase.resources.configuration).apply {
                setLocale(locale)
            }
            newBase.createConfigurationContext(config)
        } else {
            newBase
        }
        super.attachBaseContext(context)
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val settings by settingsRepository.settings.collectAsState(initial = null)

            // Apply language setting
            settings?.language?.let { lang ->
                applyLanguage(lang)
            }

            val accentColor = when (settings?.accentColorName) {
                "green" -> AccentColor.Green
                "pink" -> AccentColor.Pink
                "slate" -> AccentColor.Slate
                "sky" -> AccentColor.Sky
                "rose" -> AccentColor.Rose
                "sand" -> AccentColor.Sand
                else -> AccentColor.Teal
            }

            MealMateTheme(
                accentColor = accentColor,
                isDarkTheme = settings?.isDarkTheme ?: true,
                fontSizeScale = settings?.fontSizeScale ?: 1.0f
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScaffold(
                        windowWidthSizeClass = windowSizeClass.widthSizeClass
                    ) { navController, windowWidthSizeClass ->
                        AppNavGraph(
                            navController = navController,
                            windowWidthSizeClass = windowWidthSizeClass
                        )
                    }
                }
            }
        }
    }

    private fun applyLanguage(lang: String) {
        if (lang != pendingLanguage) {
            pendingLanguage = lang
            recreate()
        }
    }

    companion object {
        private var pendingLanguage: String? = null
    }
}
