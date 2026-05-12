package com.sranker.mealmate.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Serializable representation of a meal for JSON import/export.
 * This is a flat, self-contained format that can be shared between devices.
 */
@Serializable
data class BackupMeal(
    val name: String,
    val recipe: String = "",
    val ingredients: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val servingSize: Int? = null,
    val sourceUrl: String = ""
)

/**
 * Serializable container for a collection of backup meals.
 */
@Serializable
data class BackupData(
    val version: Int = 1,
    val meals: List<BackupMeal>
)

/**
 * Repository that handles JSON serialization and deserialization
 * of meal data for import/export functionality.
 *
 * Validation and conflict handling strategies:
 * - Duplicate names are skipped during import (case-insensitive).
 * - New tags referenced by imported meals are created automatically.
 * - Existing meals are never overwritten — only new meals are added.
 */
@Singleton
class BackupRepository @Inject constructor(
    private val mealDao: MealDao,
    private val tagDao: TagDao,
    private val ingredientDao: IngredientDao
) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Serializes all meals in the database to a JSON string.
     *
     * @return A pretty-printed JSON string representing all meals,
     *   ingredients, and tags.
     */
    suspend fun exportToJson(): String {
        val mealsSnapshot = mutableListOf<BackupMeal>()
        val meals = mealDao.getAllMeals()
        meals.forEach { meal ->
            val ingredients = ingredientDao.getIngredientsForMealOnce(meal.id)
            val tagIds = tagDao.getTagIdsForMeal(meal.id)
            val tags = tagIds.mapNotNull { tagDao.getTagById(it) }

            mealsSnapshot.add(
                BackupMeal(
                    name = meal.name,
                    recipe = meal.recipe,
                    ingredients = ingredients.map { it.name },
                    tags = tags.map { it.name },
                    servingSize = meal.servingSize,
                    sourceUrl = meal.sourceUrl
                )
            )
        }

        return json.encodeToString(BackupData(meals = mealsSnapshot))
    }

    /**
     * Imports meals from a JSON string.
     *
     * Conflict resolution:
     * - Meals with names that already exist (case-insensitive) are **skipped**.
     * - New tags are created on the fly if they don't already exist.
     *
     * @param jsonString The JSON string produced by [exportToJson].
     * @return The number of meals successfully imported.
     */
    suspend fun importFromJson(jsonString: String): Int {
        val backup = json.decodeFromString<BackupData>(jsonString)
        val existingNames = mealDao.getAllMeals().map { it.name.lowercase() }.toMutableSet()
        var importedCount = 0

        backup.meals.forEach { backupMeal ->
            if (backupMeal.name.lowercase() in existingNames) {
                // Skip duplicate
                return@forEach
            }

            // Resolve tags: create if missing, collect IDs
            val tagIds = backupMeal.tags.map { tagName ->
                val existing = tagDao.getTagByName(tagName)
                if (existing != null) {
                    existing.id
                } else {
                    tagDao.insert(TagEntity(name = tagName))
                }
            }

            // Insert the meal
            val mealId = mealDao.insert(
                MealEntity(
                    name = backupMeal.name,
                    recipe = backupMeal.recipe,
                    servingSize = backupMeal.servingSize,
                    sourceUrl = backupMeal.sourceUrl
                )
            )

            // Insert ingredients
            if (backupMeal.ingredients.isNotEmpty()) {
                ingredientDao.insertAll(
                    backupMeal.ingredients.map { name ->
                        IngredientEntity(mealId = mealId, name = name)
                    }
                )
            }

            // Insert meal-tag associations
            if (tagIds.isNotEmpty()) {
                tagDao.insertMealTagCrossRefs(
                    tagIds.map { MealTagCrossRef(mealId = mealId, tagId = it) }
                )
            }

            importedCount++
            existingNames.add(backupMeal.name.lowercase())
        }

        return importedCount
    }
}
