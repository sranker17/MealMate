package com.sranker.mealmate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sranker.mealmate.data.MealRepository
import com.sranker.mealmate.data.MealWithTags
import com.sranker.mealmate.data.MenuRepository
import com.sranker.mealmate.data.TagEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * One-shot UI events emitted by [MealListViewModel].
 */
sealed interface MealListEvent {
    data object AddedToPlan : MealListEvent
    data object RemovedFromPlan : MealListEvent
    data object AlreadyInPlan : MealListEvent
    data object MenuLocked : MealListEvent
}

/**
 * UI state for the meal list screen.
 *
 * @property meals The filtered list of meals (by search query and/or tags).
 * @property searchQuery The current search query text.
 * @property selectedTagIds The set of tag IDs selected as filters.
 * @property allTags All available tags for filter display.
 * @property isLoading Whether the initial load is in progress.
 * @property mealIdsInActivePlan The set of meal IDs currently in the active (non-accepted) menu.
 * @property isActiveMenuLocked Whether the active menu has been accepted (locked).
 */
data class MealListUiState(
    val meals: List<MealWithTags> = emptyList(),
    val searchQuery: String = "",
    val selectedTagIds: Set<Long> = emptySet(),
    val allTags: List<TagEntity> = emptyList(),
    val isLoading: Boolean = false,
    val mealIdsInActivePlan: Set<Long> = emptySet(),
    val isActiveMenuLocked: Boolean = false
)

/**
 * ViewModel for the meal list screen.
 *
 * Manages searching, tag-based filtering, and observing all meals.
 * Exposes reactive [StateFlow]s for the UI layer.
 * Supports adding meals directly to the active plan.
 */
@HiltViewModel
class MealListViewModel @Inject constructor(
    private val mealRepository: MealRepository,
    private val menuRepository: MenuRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTagIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTagIds: StateFlow<Set<Long>> = _selectedTagIds.asStateFlow()

    /** One-shot UI events (snackbar). */
    private val _events = MutableSharedFlow<MealListEvent>()
    val events = _events.asSharedFlow()

    /** All tags available for filter selection. */
    val allTags: StateFlow<List<TagEntity>> = mealRepository.getAllTags()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val uiState: StateFlow<MealListUiState> = combine(
        _searchQuery,
        _selectedTagIds,
        mealRepository.getAllMealsWithTags(),
        menuRepository.getActiveMenuWithMealsFlow()
    ) { query, tagIds, allMeals, activeMenu ->
        val filtered = allMeals.filter { withTags ->
            val matchesSearch = query.isBlank() ||
                    withTags.meal.name.contains(query, ignoreCase = true)
            val matchesTags = tagIds.isEmpty() ||
                    withTags.tags.any { it.id in tagIds }
            matchesSearch && matchesTags
        }
        val mealIdsInActivePlan = if (activeMenu != null && !activeMenu.menu.isCompleted) {
            // We need cross-ref info; use a lookup based on what we know
            activeMenu.meals.map { it.id }.toSet()
        } else {
            emptySet()
        }
        MealListUiState(
            meals = filtered,
            searchQuery = query,
            selectedTagIds = tagIds,
            allTags = allTags.value,
            isLoading = false,
            mealIdsInActivePlan = mealIdsInActivePlan,
            isActiveMenuLocked = activeMenu?.menu?.isAccepted ?: false
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MealListUiState())

    /** Update the search query. */
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    /** Toggle a tag filter on or off. */
    fun onTagFilterToggled(tagId: Long) {
        _selectedTagIds.value = _selectedTagIds.value.let { current ->
            if (tagId in current) current - tagId else current + tagId
        }
    }

    /** Clear all tag filters. */
    fun onClearFilters() {
        _selectedTagIds.value = emptySet()
    }

    /**
     * Add a meal to the active plan (non-accepted menu), or remove if already in plan.
     * Emits a one-shot [MealListEvent] for the UI to show a snackbar.
     */
    fun addToActivePlan(mealId: Long) {
        viewModelScope.launch {
            val state = uiState.value
            if (state.isActiveMenuLocked) {
                _events.emit(MealListEvent.MenuLocked)
            } else if (mealId in state.mealIdsInActivePlan) {
                menuRepository.unpinMealFromActiveMenu(mealId)
                _events.emit(MealListEvent.RemovedFromPlan)
            } else {
                menuRepository.addMealToActiveMenu(mealId)
                _events.emit(MealListEvent.AddedToPlan)
            }
        }
    }
}
