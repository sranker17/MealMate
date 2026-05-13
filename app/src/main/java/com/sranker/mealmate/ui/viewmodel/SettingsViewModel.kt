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
 * @property importResult Message to show after an import operation, or null.
 * @property exportResult Message to show after an export operation, or null.
 */
data class SettingsUiState(
    val settings: Settings = Settings(),
    val importResult: String? = null,
    val exportResult: String? = null
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
            _uiState.value = _uiState.value.copy(exportResult = "Exportálás sikeres")
        }
    }

    /**
     * Imports meals from a JSON string.
     */
    fun importFromJson(content: String) {
        viewModelScope.launch {
            val count = backupRepository.importFromJson(content)
            _uiState.value = _uiState.value.copy(
                importResult = "Sikeresen importálva: $count étel"
            )
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(importResult = null, exportResult = null)
    }
}
