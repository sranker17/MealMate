package com.sranker.mealmate.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data class holding all user-configurable settings values.
 *
 * @property cooldown The number of completed menus that must pass before a meal
 *   can be recommended again. Default is 3.
 * @property isDarkTheme Whether the dark theme is enabled.
 * @property fontSizeScale Scale factor for font sizing (1.0 = default).
 */
data class Settings(
    val cooldown: Int = 3,
    val isDarkTheme: Boolean = true,
    val fontSizeScale: Float = 1.0f
)

/**
 * Repository that manages app settings via DataStore.
 *
 * Provides reactive [Flow] observations and suspend functions for atomic updates.
 */
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    companion object {
        private val KEY_COOLDOWN = intPreferencesKey("cooldown")
        private val KEY_DARK_THEME = booleanPreferencesKey("dark_theme")
        private val KEY_FONT_SIZE_SCALE = stringPreferencesKey("font_size_scale")
    }

    /** Observe all settings as a single [Settings] data object. */
    val settings: Flow<Settings> = dataStore.data.map { prefs ->
        Settings(
            cooldown = prefs[KEY_COOLDOWN] ?: 3,
            isDarkTheme = prefs[KEY_DARK_THEME] ?: true,
            fontSizeScale = (prefs[KEY_FONT_SIZE_SCALE]?.toFloatOrNull()) ?: 1.0f
        )
    }

    /** Update the cooldown parameter. */
    suspend fun setCooldown(value: Int) {
        dataStore.edit { prefs -> prefs[KEY_COOLDOWN] = value.coerceAtLeast(1) }
    }

    /** Update the dark theme preference. */
    suspend fun setDarkTheme(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_DARK_THEME] = enabled }
    }

    /** Update the font size scale factor. */
    suspend fun setFontSizeScale(scale: Float) {
        dataStore.edit { prefs ->
            prefs[KEY_FONT_SIZE_SCALE] = scale.coerceIn(0.5f, 2.0f).toString()
        }
    }
}
