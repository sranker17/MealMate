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
 * Data access object for [MenuEntity].
 * Provides CRUD operations, active menu management, and history queries.
 */
@Dao
interface MenuDao {

    // region Basic CRUD

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(menu: MenuEntity): Long

    @Update
    suspend fun update(menu: MenuEntity)

    @Delete
    suspend fun delete(menu: MenuEntity)

    @Query("SELECT * FROM menus WHERE id = :id")
    suspend fun getMenuById(id: Long): MenuEntity?

    // endregion

    // region Active Menu

    /**
     * Returns the one menu that is currently not completed (the active planning menu).
     * Since only one active menu exists at a time, this returns at most one result.
     */
    @Query("SELECT * FROM menus WHERE is_completed = 0 LIMIT 1")
    suspend fun getActiveMenu(): MenuEntity?

    @Query("SELECT * FROM menus WHERE is_completed = 0 LIMIT 1")
    fun getActiveMenuFlow(): Flow<MenuEntity?>

    /**
     * Marks the active menu as completed with the given [completionIndex].
     */
    @Query("UPDATE menus SET is_completed = 1, completion_index = :completionIndex WHERE is_completed = 0")
    suspend fun completeActiveMenu(completionIndex: Int)

    // endregion

    // region Menu History

    @Query("SELECT * FROM menus WHERE is_completed = 1 ORDER BY completion_index DESC")
    fun getCompletedMenusFlow(): Flow<List<MenuEntity>>

    @Query("SELECT * FROM menus WHERE is_completed = 1 ORDER BY completion_index DESC")
    suspend fun getCompletedMenusOnce(): List<MenuEntity>

    /**
     * Returns the highest completion index among completed menus, or null if none exist.
     */
    @Query("SELECT MAX(completion_index) FROM menus WHERE is_completed = 1")
    suspend fun getMaxCompletionIndex(): Int?

    // endregion

    // region Menu-Meal Relationships

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMenuMealCrossRef(crossRef: MenuMealCrossRef)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMenuMealCrossRefs(crossRefs: List<MenuMealCrossRef>)

    @Update
    suspend fun updateMenuMealCrossRef(crossRef: MenuMealCrossRef)

    @Query("SELECT * FROM menu_meal_cross_ref WHERE menuId = :menuId AND mealId = :mealId")
    suspend fun getMenuMealCrossRef(menuId: Long, mealId: Long): MenuMealCrossRef?

    @Query("SELECT * FROM menu_meal_cross_ref WHERE menuId = :menuId")
    suspend fun getMenuMealCrossRefsForMenu(menuId: Long): List<MenuMealCrossRef>

    @Query("DELETE FROM menu_meal_cross_ref WHERE menuId = :menuId AND mealId = :mealId")
    suspend fun removeMealFromMenu(menuId: Long, mealId: Long)

    @Query("DELETE FROM menu_meal_cross_ref WHERE menuId = :menuId")
    suspend fun clearMenu(menuId: Long)

    /**
     * Marks a meal as completed in the given menu.
     */
    @Query(
        """
        UPDATE menu_meal_cross_ref
        SET is_completed = 1
        WHERE menuId = :menuId AND mealId = :mealId
        """
    )
    suspend fun markMealCompleted(menuId: Long, mealId: Long)

    // endregion

    // region Menu with Meals (Relations)

    @Transaction
    @Query("SELECT * FROM menus WHERE id = :menuId")
    suspend fun getMenuWithMeals(menuId: Long): MenuWithMeals?

    @Transaction
    @Query("SELECT * FROM menus WHERE is_completed = 0 LIMIT 1")
    suspend fun getActiveMenuWithMeals(): MenuWithMeals?

    @Transaction
    @Query("SELECT * FROM menus WHERE is_completed = 0 LIMIT 1")
    fun getActiveMenuWithMealsFlow(): Flow<MenuWithMeals?>

    @Transaction
    @Query("SELECT * FROM menus WHERE is_completed = 1 ORDER BY completion_index DESC")
    fun getCompletedMenusWithMealsFlow(): Flow<List<MenuWithMeals>>

    // endregion
}

