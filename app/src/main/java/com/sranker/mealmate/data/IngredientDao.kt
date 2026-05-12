package com.sranker.mealmate.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for [IngredientEntity].
 * Manages individual ingredients scoped to a specific meal.
 */
@Dao
interface IngredientDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(ingredient: IngredientEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ingredients: List<IngredientEntity>)

    @Update
    suspend fun update(ingredient: IngredientEntity)

    @Delete
    suspend fun delete(ingredient: IngredientEntity)

    @Query("SELECT * FROM ingredients WHERE mealId = :mealId ORDER BY id ASC")
    fun getIngredientsForMeal(mealId: Long): Flow<List<IngredientEntity>>

    @Query("SELECT * FROM ingredients WHERE mealId = :mealId ORDER BY id ASC")
    suspend fun getIngredientsForMealOnce(mealId: Long): List<IngredientEntity>

    @Query("DELETE FROM ingredients WHERE mealId = :mealId")
    suspend fun deleteAllForMeal(mealId: Long)
}

