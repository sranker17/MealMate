package com.sranker.mealmate.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sranker.mealmate.data.IngredientEntity
import com.sranker.mealmate.data.MealEntity
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
 * Represents a single ingredient in the edit form.
 *
 * @property id A temporary local ID for Compose keying (not the database ID).
 * @property name The ingredient name text.
 */
data class IngredientItem(
    val id: Long = 0,
    val name: String = ""
)

/**
 * UI state for the meal edit/create screen.
 *
 * @property isEditing Whether we are editing an existing meal (true) or creating a new one.
 * @property mealId The database ID of the meal being edited, or 0 for new meals.
 * @property name The meal name.
 * @property recipe The meal recipe text.
 * @property ingredients The list of ingredient items in the form.
 * @property selectedTagIds The set of tag IDs currently assigned to the meal.
 * @property allTags All available tags for selection.
 * @property servingSize The serving size, or null if not set.
 * @property sourceUrl The source/link for the recipe.
 * @property nameError Error message for the name field, or null.
 * @property isSaving Whether the save operation is in progress.
 * @property savedMealId The ID of the saved meal (non-null after successful save).
 * @property isLoading Whether existing meal data is still loading (edit mode only).
 * @property errorMessage A general error message.
 */
data class MealEditUiState(
    val isEditing: Boolean = false,
    val mealId: Long = 0,
    val name: String = "",
    val recipe: String = "",
    val ingredients: List<IngredientItem> = listOf(IngredientItem(id = 0)),
    val selectedTagIds: Set<Long> = emptySet(),
    val allTags: List<TagEntity> = emptyList(),
    val servingSize: Int? = null,
    val sourceUrl: String = "",
    val nameError: String? = null,
    val isSaving: Boolean = false,
    val savedMealId: Long? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ViewModel for the meal create/edit screen.
 *
 * Manages form state, validation, and persists meal data via [MealRepository].
 * In edit mode, loads the existing meal's data (name, recipe, ingredients, tags).
 */
@HiltViewModel
class MealEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mealRepository: MealRepository
) : ViewModel() {

    private val mealId: Long = savedStateHandle.get<Long>("mealId") ?: 0L

    private val _uiState = MutableStateFlow(MealEditUiState())
    val uiState: StateFlow<MealEditUiState> = _uiState.asStateFlow()

    /** All available tags for tag selection. */
    val allTags: StateFlow<List<TagEntity>> = mealRepository.getAllTags()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        if (mealId > 0L) {
            loadExistingMeal()
        } else {
            _uiState.value = MealEditUiState(
                allTags = allTags.value,
                ingredients = listOf(IngredientItem(id = 0))
            )
        }
    }

    private fun loadExistingMeal() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val withTags = mealRepository.getMealWithTags(mealId)

            if (withTags == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Étel nem található"
                )
                return@launch
            }

            val withIngredients = mealRepository.getMealWithIngredients(mealId)

            val ingredients = withIngredients?.ingredients?.mapIndexed { index, entity ->
                IngredientItem(id = index.toLong(), name = entity.name)
            } ?: listOf(IngredientItem(id = 0))

            val tagIds = withTags.tags.map { it.id }.toSet()

            _uiState.value = MealEditUiState(
                isEditing = true,
                mealId = mealId,
                name = withTags.meal.name,
                recipe = withTags.meal.recipe,
                ingredients = ingredients.ifEmpty { listOf(IngredientItem(id = 0)) },
                selectedTagIds = tagIds,
                allTags = allTags.value,
                servingSize = withTags.meal.servingSize,
                sourceUrl = withTags.meal.sourceUrl,
                isLoading = false
            )
        }
    }

    // region Form field updates

    /** Update the meal name. Clears the name error if present. */
    fun onNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(
            name = name,
            nameError = if (name.isNotBlank()) null else _uiState.value.nameError
        )
    }

    /** Update the recipe text. */
    fun onRecipeChanged(recipe: String) {
        _uiState.value = _uiState.value.copy(recipe = recipe)
    }

    /** Update the serving size. Pass null to clear. */
    fun onServingSizeChanged(size: Int?) {
        _uiState.value = _uiState.value.copy(servingSize = size)
    }

    /** Update the source URL. */
    fun onSourceUrlChanged(url: String) {
        _uiState.value = _uiState.value.copy(sourceUrl = url)
    }

    /** Update an ingredient's name by its local index. */
    fun onIngredientChanged(index: Int, name: String) {
        _uiState.value = _uiState.value.copy(
            ingredients = _uiState.value.ingredients.mapIndexed { i, item ->
                if (i == index) item.copy(name = name) else item
            }
        )
    }

    /** Add a new empty ingredient row to the form. */
    fun onAddIngredient() {
        val nextId = (_uiState.value.ingredients.maxOfOrNull { it.id } ?: 0) + 1
        _uiState.value = _uiState.value.copy(
            ingredients = _uiState.value.ingredients + IngredientItem(id = nextId)
        )
    }

    /** Remove an ingredient row by its index. */
    fun onRemoveIngredient(index: Int) {
        val current = _uiState.value.ingredients
        if (current.size > 1) {
            _uiState.value = _uiState.value.copy(
                ingredients = current.filterIndexed { i, _ -> i != index }
            )
        }
    }

    /** Toggle a tag's selection state. */
    fun onTagToggled(tagId: Long) {
        _uiState.value = _uiState.value.copy(
            selectedTagIds = _uiState.value.selectedTagIds.let { current ->
                if (tagId in current) current - tagId else current + tagId
            }
        )
    }

    // endregion

    /**
     * Validate the current form state.
     *
     * @return true if the form is valid and can be saved.
     */
    private fun validate(): Boolean {
        val name = _uiState.value.name.trim()
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(nameError = "A név megadása kötelező")
            return false
        }
        return true
    }

    /**
     * Save the meal to the repository.
     *
     * Validates the form first. Checks for duplicate meal names (case-insensitive).
     * On success, updates [MealEditUiState.savedMealId].
     */
    fun saveMeal() {
        if (!validate()) return

        val state = _uiState.value

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true)

            // Check for duplicate meal name
            val trimmedName = state.name.trim()
            if (mealRepository.isMealNameTaken(trimmedName, state.mealId)) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    nameError = "Már létezik ilyen nevű étel"
                )
                return@launch
            }

            val meal = MealEntity(
                id = if (state.isEditing) state.mealId else 0,
                name = trimmedName,
                recipe = state.recipe.trim(),
                servingSize = state.servingSize,
                sourceUrl = state.sourceUrl.trim()
            )

            val ingredients = state.ingredients
                .map { it.name.trim() }
                .filter { it.isNotBlank() }
                .map { IngredientEntity(mealId = 0, name = it) }

            val mealId = mealRepository.saveMeal(
                meal = meal,
                ingredients = ingredients,
                tagIds = state.selectedTagIds.toList()
            )

            _uiState.value = _uiState.value.copy(
                isSaving = false,
                savedMealId = mealId
            )
        }
    }
}
