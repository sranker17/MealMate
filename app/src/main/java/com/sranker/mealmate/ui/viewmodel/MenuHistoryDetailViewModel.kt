package com.sranker.mealmate.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sranker.mealmate.data.MenuRepository
import com.sranker.mealmate.data.MenuWithMeals
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One-shot events emitted by [MenuHistoryDetailViewModel]. */
sealed interface MenuHistoryDetailEvent {
    data class LoadedIntoPlanner(val messageResId: Int) : MenuHistoryDetailEvent
    data class LoadIntoPlannerBlocked(val messageResId: Int) : MenuHistoryDetailEvent
}

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
 * Supports renaming the menu title and loading meals into the planner.
 */
@HiltViewModel
class MenuHistoryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val menuRepository: MenuRepository
) : ViewModel() {

    private val menuId: Long = savedStateHandle.get<Long>("menuId") ?: -1L

    private val _uiState = MutableStateFlow(MenuHistoryDetailUiState(isLoading = true))
    val uiState: StateFlow<MenuHistoryDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<MenuHistoryDetailEvent>()
    val events: SharedFlow<MenuHistoryDetailEvent> = _events.asSharedFlow()

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

    /**
     * Rename the current menu.
     *
     * @param newTitle The new title for the menu.
     */
    fun renameMenu(newTitle: String) {
        viewModelScope.launch {
            menuRepository.renameMenu(menuId, newTitle.trim())
            loadMenu()
        }
    }

    /**
     * Check if loading into planner is allowed, then load.
     * If not allowed, emits a [MenuHistoryDetailEvent.LoadIntoPlannerBlocked] event.
     */
    fun loadIntoPlanner() {
        viewModelScope.launch {
            if (!menuRepository.canLoadIntoPlanner()) {
                _events.emit(
                    MenuHistoryDetailEvent.LoadIntoPlannerBlocked(
                        messageResId = com.sranker.mealmate.R.string.history_load_blocked
                    )
                )
                return@launch
            }
            menuRepository.loadMenuIntoPlanner(menuId)
            _events.emit(
                MenuHistoryDetailEvent.LoadedIntoPlanner(
                    messageResId = com.sranker.mealmate.R.string.history_loaded_into_planner
                )
            )
        }
    }
}
