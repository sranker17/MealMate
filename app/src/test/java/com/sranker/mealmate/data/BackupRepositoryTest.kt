package com.sranker.mealmate.data

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [BackupRepository].
 *
 * Verifies JSON export/import logic including:
 * - Full meal serialization with ingredients and tags.
 * - Duplicate name detection (case-insensitive).
 * - Auto-creation of missing tags during import.
 * - Graceful handling of empty databases.
 */
class BackupRepositoryTest {

    private val mealRepository: MealRepository = mockk()
    private val mealDao: MealDao = mockk()
    private val tagDao: TagDao = mockk()
    private val ingredientDao: IngredientDao = mockk()

    private lateinit var repository: BackupRepository

    @Before
    fun setUp() {
        repository = BackupRepository(mealRepository, mealDao, tagDao, ingredientDao)
    }

    // region Export

    @Test
    fun `exportToJson produces valid JSON for meal with ingredients and tags`() = runTest {
        val meal = MealEntity(id = 1L, name = "Test Meal", recipe = "Cook it", servingSize = 2)
        val ingredients = listOf(IngredientEntity(id = 10L, mealId = 1L, name = "Salt"))
        val tags = listOf(TagEntity(id = 100L, name = "Savory"))

        coEvery { mealDao.getAllMeals() } returns listOf(meal)
        coEvery { ingredientDao.getIngredientsForMealOnce(1L) } returns ingredients
        coEvery { tagDao.getTagIdsForMeal(1L) } returns listOf(100L)
        coEvery { tagDao.getTagById(100L) } returns tags[0]
        coEvery { tagDao.getAllTagsOnce() } returns tags

        val json = repository.exportToJson()

        assertThat(json).contains("Test Meal")
        assertThat(json).contains("Cook it")
        assertThat(json).contains("Salt")
        assertThat(json).contains("Savory")
        assertThat(json).contains("\"servingSize\": 2")
    }

    @Test
    fun `exportToJson handles empty database`() = runTest {
        coEvery { mealDao.getAllMeals() } returns emptyList()
        coEvery { tagDao.getAllTagsOnce() } returns emptyList()

        val json = repository.exportToJson()

        assertThat(json).contains("\"meals\": []")
    }

    @Test
    fun `exportToJson handles meal with no ingredients or tags`() = runTest {
        val meal = MealEntity(id = 1L, name = "Minimal Meal")

        coEvery { mealDao.getAllMeals() } returns listOf(meal)
        coEvery { ingredientDao.getIngredientsForMealOnce(1L) } returns emptyList()
        coEvery { tagDao.getTagIdsForMeal(1L) } returns emptyList()
        coEvery { tagDao.getAllTagsOnce() } returns emptyList()

        val json = repository.exportToJson()

        assertThat(json).contains("Minimal Meal")
        assertThat(json).contains("\"ingredients\": []")
        assertThat(json).contains("\"tags\": []")
    }

    @Test
    fun `exportToJson includes top-level tags`() = runTest {
        val meal = MealEntity(id = 1L, name = "Soup")
        val tag1 = TagEntity(id = 1L, name = "Savory")
        val tag2 = TagEntity(id = 2L, name = "Quick")

        coEvery { mealDao.getAllMeals() } returns listOf(meal)
        coEvery { ingredientDao.getIngredientsForMealOnce(1L) } returns emptyList()
        coEvery { tagDao.getTagIdsForMeal(1L) } returns listOf(1L)
        coEvery { tagDao.getTagById(1L) } returns tag1
        coEvery { tagDao.getAllTagsOnce() } returns listOf(tag1, tag2)

        val json = repository.exportToJson()

        // Top-level tags array should contain both tags
        assertThat(json).contains("\"tags\": [")
        assertThat(json).contains("Savory")
        assertThat(json).contains("Quick")
        // "Savory" also appears in the meal's per-meal tags — that's fine
    }

    // endregion

    // region Import

    @Test
    fun `importFromJson imports new meals`() = runTest {
        val json = """
            {
                "version": 1,
                "meals": [
                    {
                        "name": "New Meal",
                        "recipe": "Boil water",
                        "ingredients": ["Water", "Salt"],
                        "tags": ["Basic"]
                    }
                ]
            }
        """.trimIndent()

        coEvery { mealDao.getAllMeals() } returns emptyList()
        coEvery { tagDao.getTagByNameIgnoreCase("Basic") } returns null
        coEvery { tagDao.insert(TagEntity(name = "Basic")) } returns 1L
        coEvery { mealDao.insert(any()) } returns 42L
        coEvery { ingredientDao.insertAll(any()) } returns Unit
        coEvery { tagDao.insertMealTagCrossRefs(any()) } returns Unit

        val count = repository.importFromJson(json)

        assertThat(count).isEqualTo(1)
        coVerify { mealDao.insert(withArg { it.name == "New Meal" }) }
        coVerify { ingredientDao.insertAll(any()) }
        coVerify { tagDao.insertMealTagCrossRefs(any()) }
    }

    @Test
    fun `importFromJson skips duplicate names case-insensitively`() = runTest {
        val json = """
            {
                "version": 1,
                "meals": [
                    { "name": "Chicken Soup", "ingredients": [], "tags": [] },
                    { "name": "chicken soup", "ingredients": [], "tags": [] },
                    { "name": "Beef Stew", "ingredients": [], "tags": [] }
                ]
            }
        """.trimIndent()

        coEvery { mealDao.getAllMeals() } returns emptyList()
        coEvery { mealDao.insert(any()) } returnsMany listOf(1L, 2L)
        coEvery { ingredientDao.insertAll(any()) } returns Unit
        coEvery { tagDao.insertMealTagCrossRefs(any()) } returns Unit

        val count = repository.importFromJson(json)

        // "Chicken Soup" and "chicken soup" are duplicates, only 2 unique names
        assertThat(count).isEqualTo(2)
        coVerify(exactly = 2) { mealDao.insert(any()) }
    }

    @Test
    fun `importFromJson skips meals that already exist in database`() = runTest {
        val json = """
            {
                "version": 1,
                "meals": [
                    { "name": "Pasta", "ingredients": [], "tags": [] },
                    { "name": "Soup", "ingredients": [], "tags": [] }
                ]
            }
        """.trimIndent()

        coEvery { mealDao.getAllMeals() } returns listOf(
            MealEntity(id = 1L, name = "Pasta")
        )
        coEvery { mealDao.insert(any()) } returns 2L
        coEvery { ingredientDao.insertAll(any()) } returns Unit
        coEvery { tagDao.insertMealTagCrossRefs(any()) } returns Unit

        val count = repository.importFromJson(json)

        assertThat(count).isEqualTo(1) // Only "Soup" is new
    }

    @Test
    fun `importFromJson creates missing tags automatically`() = runTest {
        val json = """
            {
                "version": 1,
                "meals": [
                    {
                        "name": "Spicy Noodles",
                        "ingredients": [],
                        "tags": ["Spicy", "Noodles"]
                    }
                ]
            }
        """.trimIndent()

        coEvery { mealDao.getAllMeals() } returns emptyList()
        coEvery { tagDao.getTagByNameIgnoreCase("Spicy") } returns null
        coEvery { tagDao.getTagByNameIgnoreCase("Noodles") } returns null
        coEvery { tagDao.insert(TagEntity(name = "Spicy")) } returns 10L
        coEvery { tagDao.insert(TagEntity(name = "Noodles")) } returns 11L
        coEvery { mealDao.insert(any()) } returns 1L
        coEvery { ingredientDao.insertAll(any()) } returns Unit
        coEvery { tagDao.insertMealTagCrossRefs(any()) } returns Unit

        val count = repository.importFromJson(json)

        assertThat(count).isEqualTo(1)
        coVerify(exactly = 2) { tagDao.insert(any()) }
        coVerify {
            tagDao.insertMealTagCrossRefs(
                withArg { refs ->
                    assertThat(refs).hasSize(2)
                }
            )
        }
    }

    @Test
    fun `importFromJson uses existing tags by name`() = runTest {
        val json = """
            {
                "version": 1,
                "meals": [
                    {
                        "name": "Tomato Soup",
                        "ingredients": [],
                        "tags": ["Soup"]
                    }
                ]
            }
        """.trimIndent()

        coEvery { mealDao.getAllMeals() } returns emptyList()
        coEvery { tagDao.getTagByNameIgnoreCase("Soup") } returns TagEntity(id = 5L, name = "Soup")
        coEvery { mealDao.insert(any()) } returns 1L
        coEvery { ingredientDao.insertAll(any()) } returns Unit
        coEvery { tagDao.insertMealTagCrossRefs(any()) } returns Unit

        val count = repository.importFromJson(json)

        assertThat(count).isEqualTo(1)
        coVerify(inverse = true) { tagDao.insert(any()) }
        coVerify {
            tagDao.insertMealTagCrossRefs(
                withArg { refs ->
                    assertThat(refs.first().tagId).isEqualTo(5L)
                }
            )
        }
    }

    @Test
    fun `importFromJson handles empty JSON`() = runTest {
        val json = """{"version": 1, "meals": []}""".trimIndent()

        coEvery { mealDao.getAllMeals() } returns emptyList()

        val count = repository.importFromJson(json)

        assertThat(count).isEqualTo(0)
    }

    @Test
    fun `importFromJson creates standalone tags from top-level tags list`() = runTest {
        val json = """
            {
                "version": 1,
                "tags": ["Savory", "Quick"],
                "meals": []
            }
        """.trimIndent()

        coEvery { mealDao.getAllMeals() } returns emptyList()
        coEvery { tagDao.getTagByNameIgnoreCase(any()) } returns null
        coEvery { tagDao.insert(any()) } returnsMany listOf(1L, 2L)

        val count = repository.importFromJson(json)

        assertThat(count).isEqualTo(0) // No meals imported
        coVerify(exactly = 2) { tagDao.insert(any()) }
        coVerify { tagDao.insert(TagEntity(name = "Savory")) }
        coVerify { tagDao.insert(TagEntity(name = "Quick")) }
    }

    @Test
    fun `importFromJson skips duplicate standalone tags`() = runTest {
        val json = """
            {
                "version": 1,
                "tags": ["Savory", "savory", "Quick"],
                "meals": []
            }
        """.trimIndent()

        coEvery { mealDao.getAllMeals() } returns emptyList()
        coEvery { tagDao.getTagByNameIgnoreCase("Savory") } returns null
        coEvery { tagDao.getTagByNameIgnoreCase("savory") } returns null
        coEvery { tagDao.getTagByNameIgnoreCase("Quick") } returns null
        coEvery { tagDao.insert(any()) } returnsMany listOf(1L, 2L)

        val count = repository.importFromJson(json)

        assertThat(count).isEqualTo(0)
        // Only 2 unique tags: "Savory" (or "savory") and "Quick"
        coVerify(exactly = 2) { tagDao.insert(any()) }
    }

    @Test
    fun `importFromJson does not re-create standalone tags that already exist`() = runTest {
        val json = """
            {
                "version": 1,
                "tags": ["Savory"],
                "meals": []
            }
        """.trimIndent()

        coEvery { mealDao.getAllMeals() } returns emptyList()
        coEvery { tagDao.getTagByNameIgnoreCase("Savory") } returns TagEntity(id = 5L, name = "Savory")

        val count = repository.importFromJson(json)

        assertThat(count).isEqualTo(0)
        coVerify(inverse = true) { tagDao.insert(any()) }
    }

    @Test
    fun `importFromJson populates tagCache from standalone tags for meal tag resolution`() = runTest {
        val json = """
            {
                "version": 1,
                "tags": ["Savory"],
                "meals": [
                    {
                        "name": "Spicy Noodles",
                        "ingredients": [],
                        "tags": ["Savory", "Spicy"]
                    }
                ]
            }
        """.trimIndent()

        coEvery { mealDao.getAllMeals() } returns emptyList()
        // "Savory" already exists in DB (standalone tags pass finds it)
        coEvery { tagDao.getTagByNameIgnoreCase("Savory") } returns TagEntity(id = 5L, name = "Savory")
        // "Spicy" does not exist — will be created during meal tag resolution
        coEvery { tagDao.getTagByNameIgnoreCase("Spicy") } returns null
        coEvery { tagDao.insert(any()) } returns 10L
        coEvery { mealDao.insert(any()) } returns 1L
        coEvery { ingredientDao.insertAll(any()) } returns Unit
        coEvery { tagDao.insertMealTagCrossRefs(any()) } returns Unit

        val count = repository.importFromJson(json)

        assertThat(count).isEqualTo(1) // 1 meal imported
        // "Savory" was already in DB, "Spicy" was created — only 1 new tag insert
        coVerify(exactly = 1) { tagDao.insert(any()) }
        coVerify {
            tagDao.insertMealTagCrossRefs(
                withArg { refs ->
                    assertThat(refs).hasSize(2)
                    assertThat(refs.map { it.tagId }).containsExactly(5L, 10L)
                }
            )
        }
    }
}
