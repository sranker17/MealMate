package com.sranker.mealmate.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
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
 * UI state for the menu history detail screen.
 *
 * @property menuWithMeals The menu with its meals, or null if loading/not found.
 * @property isLoading Whether the menu data is still loading.
 */
data class MenuHistoryDetailUiState(
    val menuWithMeals: MenuWithMeals? = null,
    val isLoading: Boolean = false
)

/**
 * ViewModel for the menu history detail screen.
 *
 * Loads a completed menu by its ID from [SavedStateHandle].
 */
@HiltViewModel
class MenuHistoryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val menuRepository: MenuRepository
) : ViewModel() {

    private val menuId: Long = savedStateHandle.get<Long>("menuId") ?: -1L

    private val _uiState = MutableStateFlow(MenuHistoryDetailUiState(isLoading = true))
    val uiState: StateFlow<MenuHistoryDetailUiState> = _uiState.asStateFlow()

    init {
        if (menuId > 0L) {
            loadMenu()
        } else {
            _uiState.value = MenuHistoryDetailUiState(isLoading = false)
        }
    }

    private fun loadMenu() {
        viewModelScope.launch {
            val menu = menuRepository.getMenuWithMeals(menuId)
            _uiState.value = MenuHistoryDetailUiState(
                menuWithMeals = menu,
                isLoading = false
            )
        }
    }
}

