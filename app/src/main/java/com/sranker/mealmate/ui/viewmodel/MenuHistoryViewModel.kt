package com.sranker.mealmate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sranker.mealmate.data.MenuRepository
import com.sranker.mealmate.data.MenuWithMeals
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the menu history screen.
 *
 * @property completedMenus The list of completed menus with meals.
 * @property isLoading Whether the menus are still loading.
 */
data class MenuHistoryUiState(
    val completedMenus: List<MenuWithMeals> = emptyList(),
    val isLoading: Boolean = false
)

/**
 * ViewModel for the menu history screen.
 *
 * Loads all completed (archived) menus with their meals.
 */
@HiltViewModel
class MenuHistoryViewModel @Inject constructor(
    private val menuRepository: MenuRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MenuHistoryUiState(isLoading = true))
    val uiState: StateFlow<MenuHistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            menuRepository.getCompletedMenusWithMealsFlow().collect { menus ->
                _uiState.value = MenuHistoryUiState(
                    completedMenus = menus,
                    isLoading = false
                )
            }
        }
    }
}

