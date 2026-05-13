package com.sranker.mealmate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sranker.mealmate.data.MealRepository
import com.sranker.mealmate.data.MealWithTags
import com.sranker.mealmate.data.TagEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the meal list screen.
 *
 * @property meals The filtered list of meals (by search query and/or tags).
 * @property searchQuery The current search query text.
 * @property selectedTagIds The set of tag IDs selected as filters.
 * @property allTags All available tags for filter display.
 * @property isLoading Whether the initial load is in progress.
 */
data class MealListUiState(
    val meals: List<MealWithTags> = emptyList(),
    val searchQuery: String = "",
    val selectedTagIds: Set<Long> = emptySet(),
    val allTags: List<TagEntity> = emptyList(),
    val isLoading: Boolean = false
)

/**
 * ViewModel for the meal list screen.
 *
 * Manages searching, tag-based filtering, and observing all meals.
 * Exposes reactive [StateFlow]s for the UI layer.
 */
@HiltViewModel
class MealListViewModel @Inject constructor(
    private val mealRepository: MealRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTagIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTagIds: StateFlow<Set<Long>> = _selectedTagIds.asStateFlow()

    /** All tags available for filter selection. */
    val allTags: StateFlow<List<TagEntity>> = mealRepository.getAllTags()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val uiState: StateFlow<MealListUiState> = combine(
        _searchQuery,
        _selectedTagIds,
        mealRepository.getAllMealsWithTags()
    ) { query, tagIds, allMeals ->
        val filtered = allMeals.filter { withTags ->
            val matchesSearch = query.isBlank() ||
                    withTags.meal.name.contains(query, ignoreCase = true)
            val matchesTags = tagIds.isEmpty() ||
                    withTags.tags.any { it.id in tagIds }
            matchesSearch && matchesTags
        }
        MealListUiState(
            meals = filtered,
            searchQuery = query,
            selectedTagIds = tagIds,
            allTags = allTags.value,
            isLoading = false
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

    /** Delete a meal by its entity. */
    fun deleteMeal(mealWithTags: MealWithTags) {
        viewModelScope.launch {
            mealRepository.deleteMeal(mealWithTags.meal)
        }
    }
}
