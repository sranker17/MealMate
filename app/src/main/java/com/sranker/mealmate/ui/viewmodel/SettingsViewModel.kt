package com.sranker.mealmate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sranker.mealmate.data.BackupRepository
import com.sranker.mealmate.data.MealRepository
import com.sranker.mealmate.data.Settings
import com.sranker.mealmate.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the settings screen.
 *
 * @property settings The current settings values.
 * @property importResultResId String resource ID for the import result message, or null.
 * @property exportResultResId String resource ID for the export result message, or null.
 * @property importResultArg Optional integer argument for the import result string (e.g. meal count).
 */
data class SettingsUiState(
    val settings: Settings = Settings(),
    val importResult: String? = null,
    val exportResult: String? = null,
    val importResultResId: Int? = null,
    val exportResultResId: Int? = null,
    val importResultArg: Int? = null
)

/**
 * ViewModel for the settings screen.
 *
 * Manages reading and updating all app settings,
 * as well as import/export of meal data.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val mealRepository: MealRepository,
    private val backupRepository: BackupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.value = _uiState.value.copy(settings = settings)
            }
        }
    }

    fun setCooldown(value: Int) {
        viewModelScope.launch { settingsRepository.setCooldown(value) }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDarkTheme(enabled) }
    }

    fun setFontSizeScale(scale: Float) {
        viewModelScope.launch { settingsRepository.setFontSizeScale(scale) }
    }

    fun setAccentColor(name: String) {
        viewModelScope.launch { settingsRepository.setAccentColor(name) }
    }

    /** Update the language preference. */
    fun setLanguage(lang: String) {
        viewModelScope.launch { settingsRepository.setLanguage(lang) }
    }

    fun resetCooldowns() {
        viewModelScope.launch { mealRepository.resetCooldowns() }
    }

    /**
     * Exports all meals as a JSON string.
     */
    fun exportToJson(callback: (String) -> Unit) {
        viewModelScope.launch {
            val json = backupRepository.exportToJson()
            callback(json)
            _uiState.value = _uiState.value.copy(
                exportResult = null,
                exportResultResId = com.sranker.mealmate.R.string.settings_export_success
            )
        }
    }

    /**
     * Imports meals from a JSON string.
     */
    fun importFromJson(content: String) {
        viewModelScope.launch {
            val count = backupRepository.importFromJson(content)
            _uiState.value = _uiState.value.copy(
                importResult = null,
                importResultResId = com.sranker.mealmate.R.string.settings_import_success,
                importResultArg = count
            )
        }
    }

    /** Clear import/export result messages. */
    fun clearResults() {
        _uiState.value = _uiState.value.copy(
            importResult = null,
            exportResult = null,
            importResultResId = null,
            exportResultResId = null,
            importResultArg = null
        )
    }
}
