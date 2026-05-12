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
 * Unit tests for [MealRepository].
 *
 * Uses mocked DAOs to verify repository delegation,
 * cooldown-aware random selection, and usage stat recording.
 */
class MealRepositoryTest {

    private val mealDao: MealDao = mockk()
    private val ingredientDao: IngredientDao = mockk()
    private val tagDao: TagDao = mockk()

    private lateinit var repository: MealRepository

    @Before
    fun setUp() {
        repository = MealRepository(mealDao, ingredientDao, tagDao)
    }

    // region Basic Query Delegation

    @Test
    fun `getAllMealsWithTags delegates to mealDao`() = runTest {
        val expected = listOf<MealWithTags>()
        coEvery { mealDao.getAllMealsWithTags() } returns MutableStateFlow(expected)

        val result = repository.getAllMealsWithTags().first()

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `getAllMealsWithIngredients delegates to mealDao`() = runTest {
        val expected = listOf<MealWithIngredients>()
        coEvery { mealDao.getAllMealsWithIngredients() } returns MutableStateFlow(expected)

        val result = repository.getAllMealsWithIngredients().first()

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `searchMeals delegates to mealDao`() = runTest {
        val expected = listOf<MealEntity>()
        coEvery { mealDao.searchMeals("chicken") } returns MutableStateFlow(expected)

        val result = repository.searchMeals("chicken").first()

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `getMealWithTags delegates to mealDao`() = runTest {
        val expected = mockk<MealWithTags>()
        coEvery { mealDao.getMealWithTags(1L) } returns expected

        val result = repository.getMealWithTags(1L)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `getMealWithIngredients delegates to mealDao`() = runTest {
        val expected = mockk<MealWithIngredients>()
        coEvery { mealDao.getMealWithIngredients(1L) } returns expected

        val result = repository.getMealWithIngredients(1L)

        assertThat(result).isEqualTo(expected)
    }

    // endregion

    // region Save Meal

    @Test
    fun `saveMeal inserts meal, replaces ingredients and tags`() = runTest {
        val meal = MealEntity(name = "Test Meal")
        val mealId = 42L
        val ingredients = listOf(IngredientEntity(mealId = 0, name = "Salt"))
        val tagIds = listOf(1L, 2L)

        coEvery { mealDao.insert(meal) } returns mealId
        coEvery { ingredientDao.deleteAllForMeal(mealId) } returns Unit
        coEvery { ingredientDao.insertAll(any()) } returns Unit
        coEvery { tagDao.deleteAllTagsForMeal(mealId) } returns Unit
        coEvery { tagDao.insertMealTagCrossRefs(any()) } returns Unit

        val result = repository.saveMeal(meal, ingredients, tagIds)

        assertThat(result).isEqualTo(mealId)
        coVerify { mealDao.insert(meal) }
        coVerify { ingredientDao.deleteAllForMeal(mealId) }
        coVerify { ingredientDao.insertAll(ingredients.map { it.copy(mealId = mealId) }) }
        coVerify { tagDao.deleteAllTagsForMeal(mealId) }
        coVerify { tagDao.insertMealTagCrossRefs(tagIds.map { MealTagCrossRef(mealId = mealId, tagId = it) }) }
    }

    @Test
    fun `saveMeal with empty ingredients does not call insertAll`() = runTest {
        val meal = MealEntity(name = "Minimal Meal")
        val mealId = 7L

        coEvery { mealDao.insert(meal) } returns mealId
        coEvery { ingredientDao.deleteAllForMeal(mealId) } returns Unit
        coEvery { tagDao.deleteAllTagsForMeal(mealId) } returns Unit

        repository.saveMeal(meal, ingredients = emptyList(), tagIds = emptyList())

        coVerify(inverse = true) { ingredientDao.insertAll(any()) }
        coVerify(inverse = true) { tagDao.insertMealTagCrossRefs(any()) }
    }

    // endregion

    // region Delete

    @Test
    fun `deleteMeal delegates to mealDao`() = runTest {
        val meal = MealEntity(id = 1L, name = "Delete Me")
        coEvery { mealDao.delete(meal) } returns Unit

        repository.deleteMeal(meal)

        coVerify { mealDao.delete(meal) }
    }

    // endregion

    // region Cooldown-Aware Random Selection

    @Test
    fun `getRandomMealNotInCooldown delegates to mealDao`() = runTest {
        val expected = MealEntity(name = "Random Meal")
        coEvery { mealDao.getRandomMealNotInCooldown(3, 5) } returns expected

        val result = repository.getRandomMealNotInCooldown(3, 5)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `getRandomMealNotInCooldownByTags delegates to mealDao`() = runTest {
        val expected = MealEntity(name = "Filtered Meal")
        val tagIds = listOf(1L, 2L)
        coEvery { mealDao.getRandomMealNotInCooldownByTags(3, 5, tagIds) } returns expected

        val result = repository.getRandomMealNotInCooldownByTags(3, 5, tagIds)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `getRandomMealNotInCooldown returns null when no meal available`() = runTest {
        coEvery { mealDao.getRandomMealNotInCooldown(3, 5) } returns null

        val result = repository.getRandomMealNotInCooldown(3, 5)

        assertThat(result).isNull()
    }

    // endregion

    // region Usage Stats

    @Test
    fun `recordMealCooked increments timesCooked and sets lastCompletedMenuIndex`() = runTest {
        val meal = MealEntity(id = 1L, name = "Soup", timesCooked = 5, lastCompletedMenuIndex = 2)
        coEvery { mealDao.getMealById(1L) } returns meal
        coEvery { mealDao.update(any()) } returns Unit

        repository.recordMealCooked(1L, 10)

        coVerify {
            mealDao.update(
                withArg { updated ->
                    assertThat(updated.timesCooked).isEqualTo(6)
                    assertThat(updated.lastCompletedMenuIndex).isEqualTo(10)
                }
            )
        }
    }

    @Test
    fun `recordMealCooked is no-op when meal not found`() = runTest {
        coEvery { mealDao.getMealById(999L) } returns null

        repository.recordMealCooked(999L, 5)

        coVerify(inverse = true) { mealDao.update(any()) }
    }

    @Test
    fun `recordMealSkipped increments timesSkipped`() = runTest {
        val meal = MealEntity(id = 1L, name = "Skip Me", timesSkipped = 2)
        coEvery { mealDao.getMealById(1L) } returns meal
        coEvery { mealDao.update(any()) } returns Unit

        repository.recordMealSkipped(1L)

        coVerify {
            mealDao.update(
                withArg { updated ->
                    assertThat(updated.timesSkipped).isEqualTo(3)
                }
            )
        }
    }

    @Test
    fun `recordMealSkipped is no-op when meal not found`() = runTest {
        coEvery { mealDao.getMealById(999L) } returns null

        repository.recordMealSkipped(999L)

        coVerify(inverse = true) { mealDao.update(any()) }
    }

    // endregion

    // region Cooldown Reset

    @Test
    fun `resetCooldowns resets lastCompletedMenuIndex for all meals`() = runTest {
        val meals = listOf(
            MealEntity(id = 1L, name = "A", lastCompletedMenuIndex = 5),
            MealEntity(id = 2L, name = "B", lastCompletedMenuIndex = -1),
            MealEntity(id = 3L, name = "C", lastCompletedMenuIndex = 3)
        )
        coEvery { mealDao.getAllMeals() } returns meals
        coEvery { mealDao.update(any()) } returns Unit

        repository.resetCooldowns()

        coVerify(exactly = 2) { mealDao.update(any()) }
        coVerify {
            mealDao.update(withArg { updated ->
                assertThat(updated.id).isEqualTo(1L)
                assertThat(updated.lastCompletedMenuIndex).isEqualTo(-1)
            })
        }
        coVerify {
            mealDao.update(withArg { updated ->
                assertThat(updated.id).isEqualTo(3L)
                assertThat(updated.lastCompletedMenuIndex).isEqualTo(-1)
            })
        }
    }

    // endregion

    // region Tags

    @Test
    fun `getAllTags delegates to tagDao`() = runTest {
        val expected = listOf<TagEntity>()
        coEvery { tagDao.getAllTags() } returns MutableStateFlow(expected)

        val result = repository.getAllTags().first()

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `getAllTagsOnce delegates to tagDao`() = runTest {
        val expected = listOf(TagEntity(name = "Pasta"))
        coEvery { tagDao.getAllTagsOnce() } returns expected

        val result = repository.getAllTagsOnce()

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `createTag delegates to tagDao`() = runTest {
        coEvery { tagDao.insert(TagEntity(name = "New Tag")) } returns 5L

        val result = repository.createTag("New Tag")

        assertThat(result).isEqualTo(5L)
    }

    @Test
    fun `deleteTag delegates to tagDao`() = runTest {
        val tag = TagEntity(id = 1L, name = "Delete Me")
        coEvery { tagDao.delete(tag) } returns Unit

        repository.deleteTag(tag)

        coVerify { tagDao.delete(tag) }
    }

    // endregion
}

