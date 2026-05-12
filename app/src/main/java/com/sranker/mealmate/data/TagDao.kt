package com.sranker.mealmate.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data access object for [TagEntity].
 * Provides CRUD operations and queries for meal-tag associations.
 */
@Dao
interface TagDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: TagEntity): Long

    @Update
    suspend fun update(tag: TagEntity)

    @Delete
    suspend fun delete(tag: TagEntity)

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags ORDER BY name ASC")
    suspend fun getAllTagsOnce(): List<TagEntity>

    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun getTagByName(name: String): TagEntity?

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getTagById(id: Long): TagEntity?

    // region Meal-Tag Relationships

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMealTagCrossRef(crossRef: MealTagCrossRef)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMealTagCrossRefs(crossRefs: List<MealTagCrossRef>)

    @Query("DELETE FROM meal_tag_cross_ref WHERE mealId = :mealId")
    suspend fun deleteAllTagsForMeal(mealId: Long)

    @Query("SELECT tagId FROM meal_tag_cross_ref WHERE mealId = :mealId")
    suspend fun getTagIdsForMeal(mealId: Long): List<Long>

    /**
     * Returns all meals that have at least one of the given tags.
     */
    @Query(
        """
        SELECT DISTINCT mealId FROM meal_tag_cross_ref
        WHERE tagId IN (:tagIds)
        """
    )
    suspend fun getMealIdsWithTags(tagIds: List<Long>): List<Long>

    // endregion
}

