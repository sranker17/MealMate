package com.sranker.mealmate.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.sranker.mealmate.data.IngredientEntity
import com.sranker.mealmate.data.MealEntity
import com.sranker.mealmate.data.MealRepository
import com.sranker.mealmate.data.MealWithIngredients
import com.sranker.mealmate.data.MealWithTags
import com.sranker.mealmate.data.TagEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [MealEditViewModel].
 *
 * Verifies form state management, validation, and save logic.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MealEditViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val mealRepository: MealRepository = mockk()
    private val allTags = listOf(
        TagEntity(id = 1L, name = "Soup"),
        TagEntity(id = 2L, name = "Pasta")
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { mealRepository.getAllTags() } returns MutableStateFlow(allTags)
        coEvery { mealRepository.getAllTagsOnce() } returns allTags
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // region New Meal

    @Test
    fun `new meal starts with empty form`() = runTest(testDispatcher) {
        val savedStateHandle = SavedStateHandle()
        val viewModel = MealEditViewModel(savedStateHandle, mealRepository)

        val state = viewModel.uiState.value
        assertThat(state.isEditing).isFalse()
        assertThat(state.name).isEmpty()
        assertThat(state.recipe).isEmpty()
        assertThat(state.ingredients).hasSize(1)
        assertThat(state.ingredients[0].name).isEmpty()
        assertThat(state.selectedTagIds).isEmpty()
        assertThat(state.servingSize).isNull()
        assertThat(state.sourceUrl).isEmpty()
    }

    @Test
    fun `new meal shows all tags`() = runTest(testDispatcher) {
        val savedStateHandle = SavedStateHandle()
        val viewModel = MealEditViewModel(savedStateHandle, mealRepository)

        val state = viewModel.uiState.value
        assertThat(state.isEditing).isFalse()
    }

    @Test
    fun `update name clears error`() = runTest(testDispatcher) {
        val savedStateHandle = SavedStateHandle()
        val viewModel = MealEditViewModel(savedStateHandle, mealRepository)

        viewModel.onNameChanged("")
        viewModel.onNameChanged("New Name")
        val state = viewModel.uiState.value
        assertThat(state.name).isEqualTo("New Name")
    }

    @Test
    fun `adds and removes ingredients dynamically`() = runTest(testDispatcher) {
        val savedStateHandle = SavedStateHandle()
        val viewModel = MealEditViewModel(savedStateHandle, mealRepository)

        viewModel.onAddIngredient()
        assertThat(viewModel.uiState.value.ingredients).hasSize(2)

        viewModel.onAddIngredient()
        assertThat(viewModel.uiState.value.ingredients).hasSize(3)

        viewModel.onRemoveIngredient(1)
        assertThat(viewModel.uiState.value.ingredients).hasSize(2)
    }

    @Test
    fun `cannot remove last ingredient`() = runTest(testDispatcher) {
        val savedStateHandle = SavedStateHandle()
        val viewModel = MealEditViewModel(savedStateHandle, mealRepository)

        assertThat(viewModel.uiState.value.ingredients).hasSize(1)
        viewModel.onRemoveIngredient(0)
        assertThat(viewModel.uiState.value.ingredients).hasSize(1)
    }

    @Test
    fun `toggle tag selection`() = runTest(testDispatcher) {
        val savedStateHandle = SavedStateHandle()
        val viewModel = MealEditViewModel(savedStateHandle, mealRepository)

        viewModel.onTagToggled(1L)
        assertThat(viewModel.uiState.value.selectedTagIds).containsExactly(1L)

        viewModel.onTagToggled(2L)
        assertThat(viewModel.uiState.value.selectedTagIds).containsExactly(1L, 2L)

        viewModel.onTagToggled(1L)
        assertThat(viewModel.uiState.value.selectedTagIds).containsExactly(2L)
    }

    // endregion

    // region Validation

    @Test
    fun `save with empty name shows error`() = runTest {
        val savedStateHandle = SavedStateHandle()
        val viewModel = MealEditViewModel(savedStateHandle, mealRepository)

        viewModel.saveMeal()

        val state = viewModel.uiState.value
        assertThat(state.nameError).isNull()
        assertThat(state.nameErrorResId).isEqualTo(com.sranker.mealmate.R.string.meal_edit_name_required)
        assertThat(state.savedMealId).isNull()
    }

    @Test
    fun `save with valid data calls repository`() = runTest {
        coEvery { mealRepository.saveMeal(any(), any(), any()) } returns 42L
        coEvery { mealRepository.isMealNameTaken(any(), any()) } returns false

        val savedStateHandle = SavedStateHandle()
        val viewModel = MealEditViewModel(savedStateHandle, mealRepository)

        viewModel.onNameChanged("New Meal")
        viewModel.onRecipeChanged("Cook it")
        viewModel.onIngredientChanged(0, "Salt")
        viewModel.onTagToggled(1L)
        viewModel.onServingSizeChanged(4)
        viewModel.onSourceUrlChanged("https://example.com")
        viewModel.saveMeal()

        val state = viewModel.uiState.value
        assertThat(state.savedMealId).isEqualTo(42L)
        coVerify {
            mealRepository.saveMeal(
                withArg { meal ->
                    assertThat(meal.name).isEqualTo("New Meal")
                    assertThat(meal.recipe).isEqualTo("Cook it")
                    assertThat(meal.servingSize).isEqualTo(4)
                    assertThat(meal.sourceUrl).isEqualTo("https://example.com")
                },
                withArg { ingredients ->
                    assertThat(ingredients).hasSize(1)
                    assertThat(ingredients[0].name).isEqualTo("Salt")
                },
                withArg { tagIds ->
                    assertThat(tagIds).containsExactly(1L)
                }
            )
        }
    }

    @Test
    fun `save filters out blank ingredient names`() = runTest {
        coEvery { mealRepository.saveMeal(any(), any(), any()) } returns 1L
        coEvery { mealRepository.isMealNameTaken(any(), any()) } returns false

        val savedStateHandle = SavedStateHandle()
        val viewModel = MealEditViewModel(savedStateHandle, mealRepository)

        viewModel.onNameChanged("Test")
        viewModel.onAddIngredient()
        viewModel.onIngredientChanged(0, "Salt")
        viewModel.onIngredientChanged(1, "")
        viewModel.saveMeal()

        coVerify {
            mealRepository.saveMeal(
                any(),
                withArg { ingredients ->
                    assertThat(ingredients).hasSize(1)
                    assertThat(ingredients[0].name).isEqualTo("Salt")
                },
                any()
            )
        }
    }

    // endregion

    // region Edit Existing Meal

    @Test
    fun `edit mode loads existing meal data`() = runTest(testDispatcher) {
        val existingMeal = MealWithTags(
            meal = MealEntity(id = 5L, name = "Edit Me", recipe = "Original", servingSize = 2),
            tags = listOf(TagEntity(id = 1L, name = "Soup"))
        )
        val existingIngredients = MealWithIngredients(
            meal = MealEntity(id = 5L, name = "Edit Me"),
            ingredients = listOf(
                IngredientEntity(id = 10L, mealId = 5L, name = "Garlic")
            )
        )

        coEvery { mealRepository.getMealWithTags(5L) } returns existingMeal
        coEvery { mealRepository.getMealWithIngredients(5L) } returns existingIngredients

        val savedStateHandle = SavedStateHandle(mapOf("mealId" to 5L))
        val viewModel = MealEditViewModel(savedStateHandle, mealRepository)

        val state = viewModel.uiState.value
        assertThat(state.isEditing).isTrue()
        assertThat(state.mealId).isEqualTo(5L)
        assertThat(state.name).isEqualTo("Edit Me")
        assertThat(state.recipe).isEqualTo("Original")
        assertThat(state.servingSize).isEqualTo(2)
        assertThat(state.ingredients).hasSize(1)
        assertThat(state.ingredients[0].name).isEqualTo("Garlic")
        assertThat(state.selectedTagIds).containsExactly(1L)
    }

    @Test
    fun `edit mode shows error when meal not found`() = runTest {
        coEvery { mealRepository.getMealWithTags(5L) } returns null
        coEvery { mealRepository.getMealWithIngredients(5L) } returns null

        val savedStateHandle = SavedStateHandle(mapOf("mealId" to 5L))
        val viewModel = MealEditViewModel(savedStateHandle, mealRepository)

        val state = viewModel.uiState.value
        assertThat(state.errorMessage).isNull()
        assertThat(state.nameErrorResId).isEqualTo(com.sranker.mealmate.R.string.meal_detail_not_found)
    }

    // endregion
}

