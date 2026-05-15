package com.sranker.mealmate.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository that mediates between the data layer (Room DAOs) and the rest
 * of the application for all meal-related operations.
 *
 * Handles CRUD for meals with their nested ingredients and tag associations,
 * as well as cooldown-aware random selection and usage statistics.
 */
@Singleton
class MealRepository @Inject constructor(
    private val mealDao: MealDao,
    private val ingredientDao: IngredientDao,
    private val tagDao: TagDao
) {

    /** Observe all meals with their associated tags. */
    fun getAllMealsWithTags(): Flow<List<MealWithTags>> = mealDao.getAllMealsWithTags()

    /** Observe all meals with their ingredients. */
    fun getAllMealsWithIngredients(): Flow<List<MealWithIngredients>> =
        mealDao.getAllMealsWithIngredients()

    /** Search meals by name (case-insensitive substring match). */
    fun searchMeals(query: String): Flow<List<MealEntity>> = mealDao.searchMeals(query)

    /** Get a single meal with its tags by ID, or null if not found. */
    suspend fun getMealWithTags(mealId: Long): MealWithTags? = mealDao.getMealWithTags(mealId)

    /** Get a single meal with its ingredients by ID, or null if not found. */
    suspend fun getMealWithIngredients(mealId: Long): MealWithIngredients? =
        mealDao.getMealWithIngredients(mealId)

    /**
     * Save a meal along with its ingredients and tag associations.
     * If the meal already has an [id] > 0, it will be updated; otherwise inserted.
     *
     * @return The ID of the saved meal.
     */
    suspend fun saveMeal(
        meal: MealEntity,
        ingredients: List<IngredientEntity> = emptyList(),
        tagIds: List<Long> = emptyList()
    ): Long {
        val mealId = if (meal.id > 0L) {
            mealDao.update(meal)
            meal.id
        } else {
            mealDao.insert(meal)
        }

        // Replace ingredients: delete old ones, then insert new ones
        ingredientDao.deleteAllForMeal(mealId)
        if (ingredients.isNotEmpty()) {
            ingredientDao.insertAll(ingredients.map { it.copy(mealId = mealId) })
        }

        // Replace tag associations: delete old ones, then insert new ones
        tagDao.deleteAllTagsForMeal(mealId)
        if (tagIds.isNotEmpty()) {
            tagDao.insertMealTagCrossRefs(tagIds.map {
                MealTagCrossRef(
                    mealId = mealId,
                    tagId = it
                )
            })
        }

        return mealId
    }

    /** Delete a meal and its associated data (cascaded by Room). */
    suspend fun deleteMeal(meal: MealEntity) = mealDao.delete(meal)

    /**
     * Returns one random meal that is not in cooldown.
     * See [MealDao.getRandomMealNotInCooldown].
     */
    suspend fun getRandomMealNotInCooldown(
        cooldown: Int,
        currentIndex: Int,
        excludeIds: List<Long> = emptyList()
    ): MealEntity? = mealDao.getRandomMealNotInCooldown(cooldown, currentIndex, excludeIds)

    /**
     * Returns one random meal matching the given [tagIds] and not in cooldown.
     * See [MealDao.getRandomMealNotInCooldownByTags].
     */
    suspend fun getRandomMealNotInCooldownByTags(
        cooldown: Int,
        currentIndex: Int,
        tagIds: List<Long>,
        excludeIds: List<Long> = emptyList()
    ): MealEntity? =
        mealDao.getRandomMealNotInCooldownByTags(cooldown, currentIndex, tagIds, excludeIds)

    /** Increment the [timesCooked] counter and update [lastCompletedMenuIndex]. */
    suspend fun recordMealCooked(mealId: Long, menuCompletionIndex: Int) {
        val meal = mealDao.getMealById(mealId) ?: return
        mealDao.update(
            meal.copy(
                timesCooked = meal.timesCooked + 1,
                lastCompletedMenuIndex = menuCompletionIndex
            )
        )
    }

    /** Increment the [timesSkipped] counter. */
    suspend fun recordMealSkipped(mealId: Long) {
        val meal = mealDao.getMealById(mealId) ?: return
        mealDao.update(meal.copy(timesSkipped = meal.timesSkipped + 1))
    }

    /** Reset [lastCompletedMenuIndex] to -1 for all meals (cooldown reset). */
    suspend fun resetCooldowns() {
        mealDao.getAllMeals().forEach { meal ->
            if (meal.lastCompletedMenuIndex != -1) {
                mealDao.update(meal.copy(lastCompletedMenuIndex = -1))
            }
        }
    }

    /** Get all tags. */
    fun getAllTags(): Flow<List<TagEntity>> = tagDao.getAllTags()

    /** Get all tags once (non-flow). */
    suspend fun getAllTagsOnce(): List<TagEntity> = tagDao.getAllTagsOnce()

    /** Create a new tag. Returns the generated ID. */
    suspend fun createTag(name: String): Long = tagDao.insert(TagEntity(name = name))

    /** Delete a tag. */
    suspend fun deleteTag(tag: TagEntity) = tagDao.delete(tag)

    /**
     * Check if a meal with the given name already exists (case-insensitive).
     *
     * @param name The meal name to check.
     * @param excludeId Optional ID to exclude from the check (for editing).
     * @return true if a meal with the same name already exists.
     */
    suspend fun isMealNameTaken(name: String, excludeId: Long = 0): Boolean =
        mealDao.countMealsByName(name, excludeId) > 0

    /** SharedFlow for delete-undo events, emitted when a meal is deleted from a detail screen. */
    private val _deleteUndoEvents = MutableSharedFlow<MealWithTags>()
    val deleteUndoEvents: SharedFlow<MealWithTags> = _deleteUndoEvents.asSharedFlow()

    /** Emit a deleted meal with its tags for undo handling on other screens. */
    suspend fun emitDeleteUndoEvent(mealWithTags: MealWithTags) {
        _deleteUndoEvents.emit(mealWithTags)
    }
}
