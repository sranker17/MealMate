package com.sranker.mealmate.data

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [MenuRepository].
 *
 * Verifies active menu lifecycle: creation, pinning, unpinning,
 * acceptance, completion, and meal stat recording delegation.
 */
class MenuRepositoryTest {

    private val menuDao: MenuDao = mockk()
    private val mealRepository: MealRepository = mockk()

    private lateinit var repository: MenuRepository

    @Before
    fun setUp() {
        repository = MenuRepository(menuDao, mealRepository)
    }

    // region Active Menu

    @Test
    fun `getOrCreateActiveMenu returns existing active menu when present`() = runTest {
        val existing = MenuEntity(id = 1L, title = "Existing Menu")
        coEvery { menuDao.getActiveMenu() } returns existing

        val result = repository.getOrCreateActiveMenu()

        assertThat(result).isEqualTo(existing)
        coVerify(inverse = true) { menuDao.insert(any()) }
    }

    @Test
    fun `getOrCreateActiveMenu creates new menu when none exists`() = runTest {
        coEvery { menuDao.getActiveMenu() } returns null
        coEvery { menuDao.insert(any()) } returns 1L

        val result = repository.getOrCreateActiveMenu()

        assertThat(result.id).isEqualTo(1L)
        assertThat(result.isCompleted).isFalse()
        coVerify { menuDao.insert(any()) }
    }

    // endregion

    // region Pin / Unpin

    @Test
    fun `pinMealToActiveMenu creates menu and inserts cross ref when none exists`() = runTest {
        val menu = MenuEntity(id = 1L, title = "Menu")
        coEvery { menuDao.getActiveMenu() } returns null
        coEvery { menuDao.insert(any()) } returns 1L
        coEvery { menuDao.getMenuMealCrossRef(1L, 42L) } returns null
        coEvery { menuDao.insertMenuMealCrossRef(any()) } returns Unit

        repository.pinMealToActiveMenu(42L)

        coVerify {
            menuDao.insertMenuMealCrossRef(
                withArg { ref ->
                    assertThat(ref.menuId).isEqualTo(1L)
                    assertThat(ref.mealId).isEqualTo(42L)
                    assertThat(ref.isPinned).isTrue()
                }
            )
        }
    }

    @Test
    fun `pinMealToActiveMenu updates existing cross ref if not pinned`() = runTest {
        val menu = MenuEntity(id = 1L, title = "Menu")
        val existing = MenuMealCrossRef(menuId = 1L, mealId = 42L, isPinned = false)
        coEvery { menuDao.getActiveMenu() } returns menu
        coEvery { menuDao.getMenuMealCrossRef(1L, 42L) } returns existing
        coEvery { menuDao.updateMenuMealCrossRef(any()) } returns Unit

        repository.pinMealToActiveMenu(42L)

        coVerify {
            menuDao.updateMenuMealCrossRef(
                withArg { ref ->
                    assertThat(ref.isPinned).isTrue()
                }
            )
        }
    }

    @Test
    fun `unpinMealFromActiveMenu removes cross ref`() = runTest {
        val menu = MenuEntity(id = 1L, title = "Menu")
        coEvery { menuDao.getActiveMenu() } returns menu
        coEvery { menuDao.removeMealFromMenu(1L, 42L) } returns Unit

        repository.unpinMealFromActiveMenu(42L)

        coVerify { menuDao.removeMealFromMenu(1L, 42L) }
    }

    @Test
    fun `unpinMealFromActiveMenu is no-op when no active menu`() = runTest {
        coEvery { menuDao.getActiveMenu() } returns null

        repository.unpinMealFromActiveMenu(42L)

        coVerify(inverse = true) { menuDao.removeMealFromMenu(any(), any()) }
    }

    // endregion

    // region Accept Menu

    @Test
    fun `acceptMenu returns true when active menu has pinned meals`() = runTest {
        val menu = MenuEntity(id = 1L, title = "Menu")
        val crossRefs = listOf(MenuMealCrossRef(menuId = 1L, mealId = 42L, isPinned = true))
        coEvery { menuDao.getActiveMenu() } returns menu
        coEvery { menuDao.getMenuMealCrossRefsForMenu(1L) } returns crossRefs
        coEvery { menuDao.acceptActiveMenu() } returns Unit

        val result = repository.acceptMenu()

        assertThat(result).isTrue()
    }

    @Test
    fun `acceptMenu returns false when no active menu`() = runTest {
        coEvery { menuDao.getActiveMenu() } returns null

        val result = repository.acceptMenu()

        assertThat(result).isFalse()
    }

    @Test
    fun `acceptMenu returns false when no meals are pinned`() = runTest {
        val menu = MenuEntity(id = 1L, title = "Menu")
        coEvery { menuDao.getActiveMenu() } returns menu
        coEvery { menuDao.getMenuMealCrossRefsForMenu(1L) } returns emptyList()

        val result = repository.acceptMenu()

        assertThat(result).isFalse()
    }

    // endregion

    // region Finish Menu

    @Test
    fun `finishMenu records cooked meals and skipped meals`() = runTest {
        val menu = MenuEntity(id = 1L, title = "Menu")
        val crossRefs = listOf(
            MenuMealCrossRef(menuId = 1L, mealId = 10L, isPinned = true, isCompleted = true),
            MenuMealCrossRef(menuId = 1L, mealId = 20L, isPinned = true, isCompleted = false)
        )
        coEvery { menuDao.getActiveMenu() } returns menu
        coEvery { menuDao.getMenuMealCrossRefsForMenu(1L) } returns crossRefs
        coEvery { menuDao.getMaxCompletionIndex() } returns 5
        coEvery { menuDao.completeActiveMenu(6) } returns Unit
        coEvery { mealRepository.recordMealCooked(10L, 6) } returns Unit
        coEvery { mealRepository.recordMealSkipped(20L) } returns Unit

        val result = repository.finishMenu()

        assertThat(result).isEqualTo(6)
        coVerify { mealRepository.recordMealCooked(10L, 6) }
        coVerify { mealRepository.recordMealSkipped(20L) }
        coVerify { menuDao.completeActiveMenu(6) }
    }

    @Test
    fun `finishMenu returns -1 when no active menu`() = runTest {
        coEvery { menuDao.getActiveMenu() } returns null

        val result = repository.finishMenu()

        assertThat(result).isEqualTo(-1)
    }

    // endregion

    // region Meal Completion

    @Test
    fun `markMealCompleted delegates to menuDao`() = runTest {
        val menu = MenuEntity(id = 1L, title = "Menu")
        coEvery { menuDao.getActiveMenu() } returns menu
        coEvery { menuDao.markMealCompleted(1L, 42L) } returns Unit

        repository.markMealCompleted(42L)

        coVerify { menuDao.markMealCompleted(1L, 42L) }
    }

    @Test
    fun `markMealCompleted is no-op when no active menu`() = runTest {
        coEvery { menuDao.getActiveMenu() } returns null

        repository.markMealCompleted(42L)

        coVerify(inverse = true) { menuDao.markMealCompleted(any(), any()) }
    }

    // endregion

    // region History Queries

    @Test
    fun `getActiveMenuWithMealsFlow delegates to menuDao`() = runTest {
        val expected: MenuWithMeals? = null
        coEvery { menuDao.getActiveMenuWithMealsFlow() } returns MutableStateFlow(expected)

        val result = repository.getActiveMenuWithMealsFlow().first()

        assertThat(result).isNull()
    }

    @Test
    fun `getCompletedMenusWithMealsFlow delegates to menuDao`() = runTest {
        val expected = listOf<MenuWithMeals>()
        coEvery { menuDao.getCompletedMenusWithMealsFlow() } returns MutableStateFlow(expected)

        val result = repository.getCompletedMenusWithMealsFlow().first()

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `getCompletedMenusOnce delegates to menuDao`() = runTest {
        val expected = listOf(MenuEntity(id = 1L, title = "Past", isCompleted = true))
        coEvery { menuDao.getCompletedMenusOnce() } returns expected

        val result = repository.getCompletedMenusOnce()

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `getMenuWithMeals delegates to menuDao`() = runTest {
        val expected: MenuWithMeals? = null
        coEvery { menuDao.getMenuWithMeals(1L) } returns expected

        val result = repository.getMenuWithMeals(1L)

        assertThat(result).isNull()
    }

    // endregion

    // region New Methods for Task 6

    @Test
    fun `getActiveMenuCrossRefs returns cross refs when active menu exists`() = runTest {
        val menu = MenuEntity(id = 1L, title = "Menu")
        val crossRefs = listOf(
            MenuMealCrossRef(menuId = 1L, mealId = 10L, isPinned = true)
        )
        coEvery { menuDao.getActiveMenu() } returns menu
        coEvery { menuDao.getMenuMealCrossRefsForMenu(1L) } returns crossRefs

        val result = repository.getActiveMenuCrossRefs()

        assertThat(result).isEqualTo(crossRefs)
    }

    @Test
    fun `getActiveMenuCrossRefs returns empty when no active menu`() = runTest {
        coEvery { menuDao.getActiveMenu() } returns null

        val result = repository.getActiveMenuCrossRefs()

        assertThat(result).isEmpty()
    }

    @Test
    fun `getCurrentCompletionIndex returns max index from dao`() = runTest {
        coEvery { menuDao.getMaxCompletionIndex() } returns 10

        val result = repository.getCurrentCompletionIndex()

        assertThat(result).isEqualTo(10)
    }

    @Test
    fun `getCurrentCompletionIndex returns 0 when no completed menus`() = runTest {
        coEvery { menuDao.getMaxCompletionIndex() } returns null

        val result = repository.getCurrentCompletionIndex()

        assertThat(result).isEqualTo(0)
    }

    @Test
    fun `addMealToActiveMenu creates menu and inserts cross ref`() = runTest {
        coEvery { menuDao.getActiveMenu() } returns null
        coEvery { menuDao.insert(any()) } returns 1L
        coEvery { menuDao.getMenuMealCrossRef(1L, 42L) } returns null
        coEvery { menuDao.insertMenuMealCrossRef(any()) } returns Unit

        repository.addMealToActiveMenu(42L)

        coVerify {
            menuDao.insertMenuMealCrossRef(
                withArg { ref ->
                    assertThat(ref.menuId).isEqualTo(1L)
                    assertThat(ref.mealId).isEqualTo(42L)
                    assertThat(ref.isPinned).isTrue()
                }
            )
        }
    }

    @Test
    fun `addMealToActiveMenu does not insert duplicate`() = runTest {
        val menu = MenuEntity(id = 1L, title = "Menu")
        val existing = MenuMealCrossRef(menuId = 1L, mealId = 42L, isPinned = true)
        coEvery { menuDao.getActiveMenu() } returns menu
        coEvery { menuDao.getMenuMealCrossRef(1L, 42L) } returns existing

        repository.addMealToActiveMenu(42L)

        coVerify(inverse = true) { menuDao.insertMenuMealCrossRef(any()) }
    }

    // endregion
}
