package com.sranker.mealmate.ui.viewmodel

import com.google.common.truth.Truth.assertThat
import com.sranker.mealmate.data.MealEntity
import com.sranker.mealmate.data.MealRepository
import com.sranker.mealmate.data.MealWithTags
import com.sranker.mealmate.data.MenuMealCrossRef
import com.sranker.mealmate.data.MenuRepository
import com.sranker.mealmate.data.MenuWithMeals
import com.sranker.mealmate.data.SettingsRepository
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
 * Unit tests for [PlannerViewModel].
 *
 * Verifies the recommendation engine, pin/skip/accept/finish lifecycle,
 * and cooldown-aware meal selection.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlannerViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val menuRepository: MenuRepository = mockk()
    private val mealRepository: MealRepository = mockk()
    private val settingsRepository: SettingsRepository = mockk()

    private lateinit var viewModel: PlannerViewModel

    private val soupMeal = MealEntity(id = 1L, name = "Soup", timesCooked = 0, timesSkipped = 0)
    private val soupWithTags = MealWithTags(
        meal = soupMeal,
        tags = listOf(TagEntity(id = 1L, name = "Soup"))
    )
    private val pastaMeal = MealEntity(id = 2L, name = "Pasta", timesCooked = 0, timesSkipped = 0)
    private val pastaWithTags = MealWithTags(
        meal = pastaMeal,
        tags = listOf(TagEntity(id = 2L, name = "Pasta"))
    )
    private val allTags = listOf(
        TagEntity(id = 1L, name = "Soup"),
        TagEntity(id = 2L, name = "Pasta")
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // Default Settings mock
        coEvery { settingsRepository.settings } returns MutableStateFlow(
            com.sranker.mealmate.data.Settings(cooldown = 3)
        )

        // Default menu flow (no active menu)
        coEvery { menuRepository.getActiveMenuWithMealsFlow() } returns MutableStateFlow(null)
        coEvery { menuRepository.getActiveMenuCrossRefs() } returns emptyList()
        coEvery { menuRepository.getCurrentCompletionIndex() } returns 0

        // Default tags
        coEvery { mealRepository.getAllTags() } returns MutableStateFlow(allTags)

        viewModel = PlannerViewModel(menuRepository, mealRepository, settingsRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // region Initial State

    @Test
    fun `initial state has no recommendation and loads tags`() = runTest {
        val state = viewModel.uiState.value
        assertThat(state.recommendedMeal).isNull()
        assertThat(state.allTags).hasSize(2)
        assertThat(state.cooldown).isEqualTo(3)
        assertThat(state.isAccepted).isFalse()
    }

    // endregion

    // region Recommendation Engine

    @Test
    fun `recommendMeal fetches random meal not in cooldown`() = runTest {
        coEvery { mealRepository.getRandomMealNotInCooldown(any(), any(), any()) } returns soupMeal
        coEvery { mealRepository.getMealWithTags(1L) } returns soupWithTags

        viewModel.recommendMeal()

        val state = viewModel.uiState.value
        assertThat(state.recommendedMeal).isEqualTo(soupWithTags)
        assertThat(state.isRecommending).isFalse()
    }

    @Test
    fun `recommendMeal with tag filters uses by-tags query`() = runTest {
        coEvery { mealRepository.getRandomMealNotInCooldownByTags(any(), any(), any(), any()) } returns pastaMeal
        coEvery { mealRepository.getMealWithTags(2L) } returns pastaWithTags

        viewModel.onTagFilterToggled(1L) // Soup tag
        viewModel.recommendMeal()

        coVerify { mealRepository.getRandomMealNotInCooldownByTags(3, 0, listOf(1L), listOf()) }
        val state = viewModel.uiState.value
        assertThat(state.recommendedMeal).isEqualTo(pastaWithTags)
    }

    @Test
    fun `recommendMeal shows error when no meals available`() = runTest {
        coEvery { mealRepository.getRandomMealNotInCooldown(any(), any(), any()) } returns null

        viewModel.recommendMeal()

        val state = viewModel.uiState.value
        assertThat(state.recommendedMeal).isNull()
        assertThat(state.errorMessage).isNull()
    }

    @Test
    fun `recommendMeal increments skip count for previous unpinned recommendation`() = runTest {
        coEvery { mealRepository.getRandomMealNotInCooldown(any(), any(), any()) } returnsMany listOf(soupMeal, pastaMeal)
        coEvery { mealRepository.getMealWithTags(1L) } returns soupWithTags
        coEvery { mealRepository.getMealWithTags(2L) } returns pastaWithTags
        coEvery { mealRepository.recordMealSkipped(1L) } returns Unit

        // First recommendation
        viewModel.recommendMeal()
        assertThat(viewModel.uiState.value.recommendedMeal?.meal?.id).isEqualTo(1L)

        // Second recommendation — first should be auto-skipped
        viewModel.recommendMeal()

        coVerify { mealRepository.recordMealSkipped(1L) }
        assertThat(viewModel.uiState.value.recommendedMeal?.meal?.id).isEqualTo(2L)
    }

    @Test
    fun `recommendMeal respects cooldown from settings`() = runTest {
        // Update settings to cooldown=5
        coEvery { settingsRepository.settings } returns MutableStateFlow(
            com.sranker.mealmate.data.Settings(cooldown = 5)
        )

        // Recreate ViewModel with new settings
        viewModel = PlannerViewModel(menuRepository, mealRepository, settingsRepository)

        coEvery { mealRepository.getRandomMealNotInCooldown(any(), any(), any()) } returns soupMeal
        coEvery { mealRepository.getMealWithTags(1L) } returns soupWithTags

        viewModel.recommendMeal()

        val state = viewModel.uiState.value
        assertThat(state.cooldown).isEqualTo(5)
        coVerify { mealRepository.getRandomMealNotInCooldown(5, any(), any()) }
    }

    // endregion

    // region Pin / Unpin

    @Test
    fun `pinMeal adds recommended meal to active menu and clears recommendation`() = runTest {
        coEvery { mealRepository.getRandomMealNotInCooldown(any(), any(), any()) } returns soupMeal
        coEvery { mealRepository.getMealWithTags(1L) } returns soupWithTags
        coEvery { menuRepository.pinMealToActiveMenu(1L) } returns Unit

        viewModel.recommendMeal()
        assertThat(viewModel.uiState.value.recommendedMeal).isNotNull()

        viewModel.pinMeal()

        coVerify { menuRepository.pinMealToActiveMenu(1L) }
        assertThat(viewModel.uiState.value.recommendedMeal).isNull()
    }

    @Test
    fun `unpinMeal removes meal from active menu`() = runTest {
        // Need a menu with a cross ref showing the meal is pinned for unpinMeal to proceed
        val menuWithMeals = MenuWithMeals(
            menu = com.sranker.mealmate.data.MenuEntity(id = 1L, title = "Test Menu"),
            meals = listOf(soupMeal)
        )
        val crossRefs = listOf(MenuMealCrossRef(menuId = 1L, mealId = 1L, isPinned = true))
        coEvery { menuRepository.getActiveMenuWithMealsFlow() } returns MutableStateFlow(menuWithMeals)
        coEvery { menuRepository.getActiveMenuCrossRefs() } returns crossRefs
        coEvery { menuRepository.unpinMealFromActiveMenu(1L) } returns Unit

        // Recreate viewModel to pick up new menu flow
        viewModel = PlannerViewModel(menuRepository, mealRepository, settingsRepository)

        viewModel.unpinMeal(1L)

        coVerify { menuRepository.unpinMealFromActiveMenu(1L) }
    }

    // endregion

    // region Menu Lifecycle

    @Test
    fun `acceptMenu delegates to repository`() = runTest {
        coEvery { menuRepository.acceptMenu() } returns true

        viewModel.acceptMenu()

        coVerify { menuRepository.acceptMenu() }
        assertThat(viewModel.uiState.value.isAccepted).isTrue()
    }

    @Test
    fun `acceptMenu stays false when no pinned meals`() = runTest {
        coEvery { menuRepository.acceptMenu() } returns false

        viewModel.acceptMenu()

        coVerify { menuRepository.acceptMenu() }
        assertThat(viewModel.uiState.value.isAccepted).isFalse()
    }

    @Test
    fun `onMealCompletedToggled delegates to repository`() = runTest {
        coEvery { menuRepository.markMealCompleted(1L) } returns Unit

        viewModel.onMealCompletedToggled(1L)

        coVerify { menuRepository.markMealCompleted(1L) }
    }

    @Test
    fun `finishMenu delegates to repository and clears state`() = runTest {
        coEvery { menuRepository.finishMenu() } returns 1
        coEvery { mealRepository.getRandomMealNotInCooldown(any(), any(), any()) } returns soupMeal
        coEvery { mealRepository.getMealWithTags(1L) } returns soupWithTags

        viewModel.recommendMeal()
        viewModel.finishMenu()

        coVerify { menuRepository.finishMenu() }
        val state = viewModel.uiState.value
        assertThat(state.recommendedMeal).isNull()
        assertThat(state.isAccepted).isFalse()
    }

    @Test
    fun `finishMenu is no-op when finishMenu returns -1`() = runTest {
        coEvery { menuRepository.finishMenu() } returns -1

        viewModel.finishMenu()

        coVerify { menuRepository.finishMenu() }
    }

    // endregion

    // region Tag Filters

    @Test
    fun `toggling tag filter adds and removes filters`() = runTest {
        viewModel.onTagFilterToggled(1L)
        assertThat(viewModel.uiState.value.selectedTagIds).containsExactly(1L)

        viewModel.onTagFilterToggled(2L)
        assertThat(viewModel.uiState.value.selectedTagIds).containsExactly(1L, 2L)

        viewModel.onTagFilterToggled(1L)
        assertThat(viewModel.uiState.value.selectedTagIds).containsExactly(2L)
    }

    @Test
    fun `clearFilters removes all tag filters`() = runTest {
        viewModel.onTagFilterToggled(1L)
        viewModel.onTagFilterToggled(2L)
        assertThat(viewModel.uiState.value.selectedTagIds).hasSize(2)

        viewModel.onClearFilters()
        assertThat(viewModel.uiState.value.selectedTagIds).isEmpty()
    }

    // endregion
}

