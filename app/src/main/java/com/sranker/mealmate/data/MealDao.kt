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
     * completed more than [cooldown] menus ago, or never completed (-1).
     *
     * @param cooldown The minimum number of completed menus that must have passed
     *   since the meal was last cooked.
     * @param currentIndex The current [MenuEntity.completionIndex] (i.e. the total
     *   number of completed menus so far).
     */
    @Query(
        """
        SELECT * FROM meals
        WHERE last_completed_menu_index = -1
           OR (:currentIndex - last_completed_menu_index) > :cooldown
        ORDER BY RANDOM()
        LIMIT 1
        """
    )
    suspend fun getRandomMealNotInCooldown(cooldown: Int, currentIndex: Int): MealEntity?

    /**
     * Returns a random meal filtered by tag IDs, respecting cooldown rules.
     *
     * @param cooldown The minimum number of completed menus between servings.
     * @param currentIndex The current completion index.
     * @param tagIds The list of tag IDs to filter by.
     */
    @Query(
        """
        SELECT * FROM meals
        WHERE meals.id IN (
            SELECT mealId FROM meal_tag_cross_ref
            WHERE tagId IN (:tagIds)
        )
        AND (last_completed_menu_index = -1
             OR (:currentIndex - last_completed_menu_index) > :cooldown)
        ORDER BY RANDOM()
        LIMIT 1
        """
    )
    suspend fun getRandomMealNotInCooldownByTags(
        cooldown: Int,
        currentIndex: Int,
        tagIds: List<Long>
    ): MealEntity?

    // endregion
}

