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
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [MealDetailViewModel].
 *
 * Verifies meal loading, missing meal handling, and refresh.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MealDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val mealRepository: MealRepository = mockk()

    private val mealWithTags = MealWithTags(
        meal = MealEntity(id = 1L, name = "Test Meal", recipe = "Recipe text"),
        tags = listOf(TagEntity(id = 1L, name = "Soup"))
    )

    private val mealWithIngredients = MealWithIngredients(
        meal = MealEntity(id = 1L, name = "Test Meal"),
        ingredients = listOf(
            IngredientEntity(id = 1L, mealId = 1L, name = "Salt"),
            IngredientEntity(id = 2L, mealId = 1L, name = "Pepper")
        )
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // region Load Success

    @Test
    fun `loads meal successfully with tags and ingredients`() = runTest(testDispatcher) {
        coEvery { mealRepository.getMealWithTags(1L) } returns mealWithTags
        coEvery { mealRepository.getMealWithIngredients(1L) } returns mealWithIngredients

        val savedStateHandle = SavedStateHandle(mapOf("mealId" to 1L))
        val viewModel = MealDetailViewModel(savedStateHandle, mealRepository)

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.mealWithTags).isEqualTo(mealWithTags)
        assertThat(state.mealWithIngredients).isEqualTo(mealWithIngredients)
        assertThat(state.errorMessage).isNull()
    }

    @Test
    fun `loads meal with no ingredients`() = runTest(testDispatcher) {
        coEvery { mealRepository.getMealWithTags(1L) } returns mealWithTags
        coEvery { mealRepository.getMealWithIngredients(1L) } returns null

        val savedStateHandle = SavedStateHandle(mapOf("mealId" to 1L))
        val viewModel = MealDetailViewModel(savedStateHandle, mealRepository)

        val state = viewModel.uiState.value
        assertThat(state.mealWithTags).isEqualTo(mealWithTags)
        assertThat(state.mealWithIngredients).isNull()
    }

    // endregion

    // region Load Errors

    @Test
    fun `shows error when meal not found`() = runTest(testDispatcher) {
        coEvery { mealRepository.getMealWithTags(1L) } returns null
        coEvery { mealRepository.getMealWithIngredients(1L) } returns null

        val savedStateHandle = SavedStateHandle(mapOf("mealId" to 1L))
        val viewModel = MealDetailViewModel(savedStateHandle, mealRepository)

        val state = viewModel.uiState.value
        assertThat(state.errorMessage).isNull()
        assertThat(state.errorMessageResId).isEqualTo(com.sranker.mealmate.R.string.meal_detail_not_found)
        assertThat(state.mealWithTags).isNull()
    }

    @Test
    fun `shows error when mealId is invalid`() = runTest(testDispatcher) {
        val savedStateHandle = SavedStateHandle(mapOf("mealId" to -1L))
        val viewModel = MealDetailViewModel(savedStateHandle, mealRepository)

        val state = viewModel.uiState.value
        assertThat(state.errorMessage).isNull()
        assertThat(state.errorMessageResId).isEqualTo(com.sranker.mealmate.R.string.meal_detail_not_found)
    }

    // endregion

    // region Refresh

    @Test
    fun `refresh reloads meal data`() = runTest(testDispatcher) {
        coEvery { mealRepository.getMealWithTags(1L) } returns mealWithTags
        coEvery { mealRepository.getMealWithIngredients(1L) } returns mealWithIngredients

        val savedStateHandle = SavedStateHandle(mapOf("mealId" to 1L))
        val viewModel = MealDetailViewModel(savedStateHandle, mealRepository)

        // Simulate updated data
        val updatedTags = mealWithTags.copy(
            meal = mealWithTags.meal.copy(timesCooked = 5)
        )
        coEvery { mealRepository.getMealWithTags(1L) } returns updatedTags
        coEvery { mealRepository.getMealWithIngredients(1L) } returns mealWithIngredients

        viewModel.refresh()

        val state = viewModel.uiState.value
        assertThat(state.mealWithTags?.meal?.timesCooked).isEqualTo(5)
    }

    // endregion
}
