package com.sranker.mealmate.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a meal in the database.
 *
 * @property id Auto-generated primary key.
 * @property name Display name of the meal.
 * @property recipe Preparation instructions (optional).
 * @property timesCooked Number of times this meal has been cooked.
 * @property timesSkipped Number of times this meal has been skipped.
 * @property servingSize Default serving size (optional).
 * @property sourceUrl Link to the original recipe source (optional).
 * @property lastCompletedMenuIndex The [MenuEntity.completionIndex] of the last menu
 *   in which this meal was completed. Used for cooldown calculation. -1 means never cooked.
 * @property createdAt Timestamp (millis) when this meal was created, for sorting by newest first.
 */
@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val recipe: String = "",
    val timesCooked: Int = 0,
    val timesSkipped: Int = 0,
    val servingSize: Int? = null,
    @ColumnInfo(name = "source_url")
    val sourceUrl: String = "",
    @ColumnInfo(name = "last_completed_menu_index")
    val lastCompletedMenuIndex: Int = -1,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
