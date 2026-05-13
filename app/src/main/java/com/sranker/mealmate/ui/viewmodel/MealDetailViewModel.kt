package com.sranker.mealmate.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sranker.mealmate.data.MealRepository
import com.sranker.mealmate.data.MealWithIngredients
import com.sranker.mealmate.data.MealWithTags
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the meal detail screen.
 *
 * @property mealWithTags The meal with its associated tags, or null if loading/not found.
 * @property mealWithIngredients The meal with its ingredients, or null if loading/not found.
 * @property isLoading Whether the meal data is still loading.
 * @property errorMessage An error message if the meal could not be loaded.
 */
data class MealDetailUiState(
    val mealWithTags: MealWithTags? = null,
    val mealWithIngredients: MealWithIngredients? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ViewModel for the meal detail screen.
 *
 * Loads a meal by its ID from [SavedStateHandle] and exposes its
 * full data (with tags and ingredients) via [StateFlow].
 */
@HiltViewModel
class MealDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mealRepository: MealRepository
) : ViewModel() {

    private val mealId: Long = savedStateHandle.get<Long>("mealId") ?: -1L

    private val _uiState = MutableStateFlow(MealDetailUiState(isLoading = true))
    val uiState: StateFlow<MealDetailUiState> = _uiState.asStateFlow()

    init {
        if (mealId == -1L) {
            _uiState.value = MealDetailUiState(errorMessage = "Étel nem található")
        } else {
            loadMeal()
        }
    }

    private fun loadMeal() {
        viewModelScope.launch {
            _uiState.value = MealDetailUiState(isLoading = true)
            val withTags = mealRepository.getMealWithTags(mealId)
            val withIngredients = mealRepository.getMealWithIngredients(mealId)

            if (withTags == null) {
                _uiState.value = MealDetailUiState(
                    errorMessage = "Étel nem található",
                    isLoading = false
                )
            } else {
                _uiState.value = MealDetailUiState(
                    mealWithTags = withTags,
                    mealWithIngredients = withIngredients,
                    isLoading = false
                )
            }
        }
    }

    /** Refresh the meal data from the repository. */
    fun refresh() {
        loadMeal()
    }
}

