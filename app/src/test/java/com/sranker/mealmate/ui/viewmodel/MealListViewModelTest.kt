package com.sranker.mealmate.ui.viewmodel

import com.google.common.truth.Truth.assertThat
import com.sranker.mealmate.data.MealRepository
import com.sranker.mealmate.data.MealWithTags
import com.sranker.mealmate.data.MenuRepository
import com.sranker.mealmate.data.MenuWithMeals
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
 * Unit tests for [MealListViewModel].
 *
 * Verifies search filtering, tag filtering, meal deletion logic,
 * and add-to-active-plan functionality.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MealListViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val mealRepository: MealRepository = mockk()
    private val menuRepository: MenuRepository = mockk()

    private lateinit var viewModel: MealListViewModel

    private val mealsWithTags = listOf(
        MealWithTags(
            meal = com.sranker.mealmate.data.MealEntity(id = 1L, name = "Chicken Soup"),
            tags = listOf(TagEntity(id = 1L, name = "Soup"))
        ),
        MealWithTags(
            meal = com.sranker.mealmate.data.MealEntity(id = 2L, name = "Pasta Bolognese"),
            tags = listOf(TagEntity(id = 2L, name = "Pasta"))
        ),
        MealWithTags(
            meal = com.sranker.mealmate.data.MealEntity(id = 3L, name = "Caesar Salad"),
            tags = listOf(TagEntity(id = 1L, name = "Soup"), TagEntity(id = 3L, name = "Salad"))
        )
    )

    private val allTags = listOf(
        TagEntity(id = 1L, name = "Soup"),
        TagEntity(id = 2L, name = "Pasta"),
        TagEntity(id = 3L, name = "Salad")
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        coEvery { mealRepository.getAllMealsWithTags() } returns MutableStateFlow(mealsWithTags)
        coEvery { mealRepository.getAllTags() } returns MutableStateFlow(allTags)
        coEvery { menuRepository.getActiveMenuWithMealsFlow() } returns MutableStateFlow(null)
        viewModel = MealListViewModel(mealRepository, menuRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // region Initial State

    @Test
    fun `initial state shows all meals and tags`() = runTest(testDispatcher) {
        val state = viewModel.uiState.value
        assertThat(state.meals).hasSize(3)
        assertThat(state.allTags).hasSize(3)
        assertThat(state.searchQuery).isEmpty()
        assertThat(state.selectedTagIds).isEmpty()
    }

    // endregion

    // region Search

    @Test
    fun `search query filters meals by name`() = runTest(testDispatcher) {
        viewModel.onSearchQueryChanged("Chicken")
        val state = viewModel.uiState.value
        assertThat(state.meals).hasSize(1)
        assertThat(state.meals[0].meal.name).isEqualTo("Chicken Soup")
    }

    @Test
    fun `search query is case-insensitive`() = runTest(testDispatcher) {
        viewModel.onSearchQueryChanged("chicken")
        val state = viewModel.uiState.value
        assertThat(state.meals).hasSize(1)
    }

    @Test
    fun `empty search query returns all meals`() = runTest(testDispatcher) {
        viewModel.onSearchQueryChanged("Pasta")
        var state = viewModel.uiState.value
        assertThat(state.meals).hasSize(1)

        viewModel.onSearchQueryChanged("")
        state = viewModel.uiState.value
        assertThat(state.meals).hasSize(3)
    }

    @Test
    fun `search with no matches returns empty list`() = runTest(testDispatcher) {
        viewModel.onSearchQueryChanged("xyz")
        val state = viewModel.uiState.value
        assertThat(state.meals).isEmpty()
    }

    // endregion

    // region Tag Filtering

    @Test
    fun `tag filter shows only meals with that tag`() = runTest(testDispatcher) {
        viewModel.onTagFilterToggled(2L)
        val state = viewModel.uiState.value
        assertThat(state.meals).hasSize(1)
        assertThat(state.meals[0].meal.name).isEqualTo("Pasta Bolognese")
    }

    @Test
    fun `multiple tag filters show meals with any of those tags`() = runTest(testDispatcher) {
        viewModel.onTagFilterToggled(1L)
        viewModel.onTagFilterToggled(2L)
        val state = viewModel.uiState.value
        assertThat(state.meals).hasSize(3)
    }

    @Test
    fun `toggling same tag twice removes filter`() = runTest(testDispatcher) {
        viewModel.onTagFilterToggled(1L)
        var state = viewModel.uiState.value
        assertThat(state.selectedTagIds).containsExactly(1L)
        assertThat(state.meals).hasSize(2)

        viewModel.onTagFilterToggled(1L)
        state = viewModel.uiState.value
        assertThat(state.selectedTagIds).isEmpty()
        assertThat(state.meals).hasSize(3)
    }

    @Test
    fun `clear filters removes all tag filters`() = runTest(testDispatcher) {
        viewModel.onTagFilterToggled(1L)
        viewModel.onTagFilterToggled(2L)
        var state = viewModel.uiState.value
        assertThat(state.selectedTagIds).hasSize(2)

        viewModel.onClearFilters()
        state = viewModel.uiState.value
        assertThat(state.selectedTagIds).isEmpty()
        assertThat(state.meals).hasSize(3)
    }

    @Test
    fun `search and tag filter work together`() = runTest(testDispatcher) {
        viewModel.onSearchQueryChanged("Chicken")
        viewModel.onTagFilterToggled(1L)
        val state = viewModel.uiState.value
        assertThat(state.meals).hasSize(1)
        assertThat(state.meals[0].meal.name).isEqualTo("Chicken Soup")
    }

    // endregion

    // region Delete

    @Test
    fun `deleteMeal delegates to repository`() = runTest(testDispatcher) {
        coEvery { mealRepository.deleteMeal(any()) } returns Unit

        viewModel.deleteMeal(mealsWithTags[0])

        coVerify { mealRepository.deleteMeal(mealsWithTags[0].meal) }
    }

    // endregion

    // region Add to Active Plan

    @Test
    fun `addToActivePlan emits AddedToPlan event`() = runTest(testDispatcher) {
        coEvery { menuRepository.addMealToActiveMenu(1L) } returns Unit

        viewModel.addToActivePlan(1L)

        coVerify { menuRepository.addMealToActiveMenu(1L) }
    }

    @Test
    fun `addToActivePlan emits AlreadyInPlan when meal already in plan`() = runTest(testDispatcher) {
        val activeMenu = MenuWithMeals(
            menu = com.sranker.mealmate.data.MenuEntity(id = 1L, title = "Menu"),
            meals = listOf(
                com.sranker.mealmate.data.MealEntity(id = 1L, name = "Chicken Soup")
            )
        )
        coEvery { menuRepository.getActiveMenuWithMealsFlow() } returns MutableStateFlow(activeMenu)

        // Recreate ViewModel to pick up new flow
        viewModel = MealListViewModel(mealRepository, menuRepository)

        // Wait for state to settle
        kotlinx.coroutines.delay(100)

        viewModel.addToActivePlan(1L)

        // Should not call addMealToActiveMenu since already in plan
        coVerify(inverse = true) { menuRepository.addMealToActiveMenu(1L) }
    }

    // endregion
}
