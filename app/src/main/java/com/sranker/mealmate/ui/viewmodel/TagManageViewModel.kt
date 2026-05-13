package com.sranker.mealmate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sranker.mealmate.data.MealRepository
import com.sranker.mealmate.data.TagEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the tag management screen.
 *
 * @property tags All available tags.
 * @property newTagName The text in the "add new tag" field.
 * @property errorMessage An error message for invalid input or duplicates.
 * @property isLoading Whether the tags are still loading.
 */
data class TagManageUiState(
    val tags: List<TagEntity> = emptyList(),
    val newTagName: String = "",
    val errorMessage: String? = null,
    val isLoading: Boolean = false
)

/**
 * ViewModel for the tag management screen.
 *
 * Provides functionality to view, add, and delete tags.
 * Tags are used for categorizing and filtering meals.
 */
@HiltViewModel
class TagManageViewModel @Inject constructor(
    private val mealRepository: MealRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TagManageUiState(isLoading = true))
    val uiState: StateFlow<TagManageUiState> = _uiState.asStateFlow()

    /** All tags as a reactive flow (used by other ViewModels if needed). */
    val allTags: StateFlow<List<TagEntity>> = mealRepository.getAllTags()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            mealRepository.getAllTags().collect { tags ->
                _uiState.value = _uiState.value.copy(
                    tags = tags,
                    isLoading = false
                )
            }
        }
    }

    /** Update the new tag name text field. */
    fun onNewTagNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(
            newTagName = name,
            errorMessage = null
        )
    }

    /**
     * Add a new tag.
     *
     * Validates that the name is not blank and does not duplicate an existing tag.
     */
    fun addTag() {
        val name = _uiState.value.newTagName.trim()

        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "A címke neve nem lehet üres"
            )
            return
        }

        if (_uiState.value.tags.any { it.name.equals(name, ignoreCase = true) }) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Már létezik ilyen nevű címke"
            )
            return
        }

        viewModelScope.launch {
            mealRepository.createTag(name)
            _uiState.value = _uiState.value.copy(newTagName = "")
        }
    }

    /** Delete a tag. */
    fun deleteTag(tag: TagEntity) {
        viewModelScope.launch {
            mealRepository.deleteTag(tag)
        }
    }
}
