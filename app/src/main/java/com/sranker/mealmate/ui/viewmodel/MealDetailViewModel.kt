package com.sranker.mealmate.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sranker.mealmate.data.MealRepository
import com.sranker.mealmate.data.MealWithIngredients
import com.sranker.mealmate.data.MealWithTags
import com.sranker.mealmate.data.MenuRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * One-shot events emitted by [MealDetailViewModel].
 */
sealed interface MealDetailEvent {
    data object MealDeleted : MealDetailEvent
}

/**
 * UI state for the meal detail screen.
 *
 * @property mealWithTags The meal with its associated tags, or null if loading/not found.
 * @property mealWithIngredients The meal with its ingredients, or null if loading/not found.
 * @property isPinned Whether the meal is pinned in the active menu.
 * @property isLoading Whether the meal data is still loading.
 * @property errorMessage An error message if the meal could not be loaded.
 * @property errorMessageResId A string resource ID for the error message, or null.
 */
data class MealDetailUiState(
    val mealWithTags: MealWithTags? = null,
    val mealWithIngredients: MealWithIngredients? = null,
    val isPinned: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val errorMessageResId: Int? = null
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
    private val mealRepository: MealRepository,
    private val menuRepository: MenuRepository
) : ViewModel() {

    private val mealId: Long = savedStateHandle.get<Long>("mealId") ?: -1L

    private val _uiState = MutableStateFlow(MealDetailUiState(isLoading = true))
    val uiState: StateFlow<MealDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<MealDetailEvent>()
    val events: SharedFlow<MealDetailEvent> = _events.asSharedFlow()

    init {
        if (mealId == -1L) {
            _uiState.value = MealDetailUiState(
                errorMessage = null,
                errorMessageResId = com.sranker.mealmate.R.string.meal_detail_not_found
            )
        } else {
            loadMeal()
        }
    }

    private fun loadMeal() {
        viewModelScope.launch {
            _uiState.value = MealDetailUiState(isLoading = true)
            val withTags = mealRepository.getMealWithTags(mealId)
            val withIngredients = mealRepository.getMealWithIngredients(mealId)

            // Check if the meal is pinned in the active menu
            val isPinned = menuRepository.getActiveMenuCrossRefs().any {
                it.mealId == mealId && it.isPinned
            }

            if (withTags == null) {
                _uiState.value = MealDetailUiState(
                    errorMessage = null,
                    errorMessageResId = com.sranker.mealmate.R.string.meal_detail_not_found,
                    isLoading = false
                )
            } else {
                _uiState.value = MealDetailUiState(
                    mealWithTags = withTags,
                    mealWithIngredients = withIngredients,
                    isPinned = isPinned,
                    isLoading = false
                )
            }
        }
    }

    /** Refresh the meal data from the repository. */
    fun refresh() {
        loadMeal()
    }

    /** Delete the meal, emit a repository-wide undo event, and navigate back. */
    fun deleteMeal() {
        val meal = _uiState.value.mealWithTags ?: return
        viewModelScope.launch {
            mealRepository.deleteMeal(meal.meal)
            mealRepository.emitDeleteUndoEvent(meal)
            _events.emit(MealDetailEvent.MealDeleted)
        }
    }
}
