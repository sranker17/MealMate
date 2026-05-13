package com.sranker.mealmate.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for [MealEntity].
 * Provides CRUD operations, tag-filtered queries, and random selection
 * respecting cooldown rules.
 */
@Dao
interface MealDao {

    // region Basic CRUD

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(meal: MealEntity): Long

    @Update
    suspend fun update(meal: MealEntity)

    @Delete
    suspend fun delete(meal: MealEntity)

    @Query("SELECT * FROM meals WHERE id = :id")
    suspend fun getMealById(id: Long): MealEntity?

    @Query("SELECT * FROM meals ORDER BY name ASC")
    fun getAllMealsFlow(): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals ORDER BY name ASC")
    suspend fun getAllMeals(): List<MealEntity>

    @Query("SELECT * FROM meals WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchMeals(query: String): Flow<List<MealEntity>>

    @Query("SELECT COUNT(*) FROM meals WHERE LOWER(name) = LOWER(:name) AND id != :excludeId")
    suspend fun countMealsByName(name: String, excludeId: Long = 0): Int

    // endregion

    // region Meals with Tags

    @Transaction
    @Query("SELECT * FROM meals ORDER BY name ASC")
    fun getAllMealsWithTags(): Flow<List<MealWithTags>>

    @Transaction
    @Query("SELECT * FROM meals WHERE id = :mealId")
    suspend fun getMealWithTags(mealId: Long): MealWithTags?

    @Transaction
    @Query("SELECT * FROM meals WHERE id = :mealId")
    suspend fun getMealWithIngredients(mealId: Long): MealWithIngredients?

    @Transaction
    @Query("SELECT * FROM meals ORDER BY name ASC")
    fun getAllMealsWithIngredients(): Flow<List<MealWithIngredients>>

    // endregion

    // region Random Selection (Cooldown-Aware)

    /**
     * Returns a random meal whose [lastCompletedMenuIndex] indicates it was last
     * completed at least [cooldown] menus ago, or never completed (-1).
     * Excludes meals whose IDs are in [excludeIds].
     *
     * Example: With cooldown = 3, a meal cooked in menu #1 will be blocked until
     * menu #4 is completed (1 + 3 = 4), at which point `currentIndex - lastIndex >= 3`.
     *
     * @param cooldown The minimum number of completed menus that must have passed
     *   since the meal was last cooked.
     * @param currentIndex The current [MenuEntity.completionIndex] (i.e. the total
     *   number of completed menus so far).
     * @param excludeIds List of meal IDs to exclude from the result.
     */
    @Query(
        """
        SELECT * FROM meals
        WHERE (last_completed_menu_index = -1
           OR (:currentIndex - last_completed_menu_index) >= :cooldown)
        AND id NOT IN (:excludeIds)
        ORDER BY RANDOM()
        LIMIT 1
        """
    )
    suspend fun getRandomMealNotInCooldown(
        cooldown: Int,
        currentIndex: Int,
        excludeIds: List<Long> = emptyList()
    ): MealEntity?

    /**
     * Returns a random meal filtered by tag IDs, respecting cooldown rules.
     * Excludes meals whose IDs are in [excludeIds].
     *
     * @param cooldown The minimum number of completed menus between servings.
     * @param currentIndex The current completion index.
     * @param tagIds The list of tag IDs to filter by.
     * @param excludeIds List of meal IDs to exclude from the result.
     */
    @Query(
        """
        SELECT * FROM meals
        WHERE meals.id IN (
            SELECT mealId FROM meal_tag_cross_ref
            WHERE tagId IN (:tagIds)
        )
        AND (last_completed_menu_index = -1
             OR (:currentIndex - last_completed_menu_index) >= :cooldown)
        AND id NOT IN (:excludeIds)
        ORDER BY RANDOM()
        LIMIT 1
        """
    )
    suspend fun getRandomMealNotInCooldownByTags(
        cooldown: Int,
        currentIndex: Int,
        tagIds: List<Long>,
        excludeIds: List<Long> = emptyList()
    ): MealEntity?

    // endregion
}
