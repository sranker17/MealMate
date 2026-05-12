package com.sranker.mealmate.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Cross-reference entity for the many-to-many relationship between [MenuEntity] and [MealEntity].
 * Tracks pinning and completion state for each meal within a menu.
 *
 * @property menuId Foreign key referencing [MenuEntity.id].
 * @property mealId Foreign key referencing [MealEntity.id].
 * @property isPinned Whether this meal has been pinned (accepted into the planned menu).
 * @property isCompleted Whether this meal has been marked as completed (cooked).
 */
@Entity(
    tableName = "menu_meal_cross_ref",
    primaryKeys = ["menuId", "mealId"],
    foreignKeys = [
        ForeignKey(
            entity = MenuEntity::class,
            parentColumns = ["id"],
            childColumns = ["menuId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MealEntity::class,
            parentColumns = ["id"],
            childColumns = ["mealId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("mealId")]
)
data class MenuMealCrossRef(
    val menuId: Long,
    val mealId: Long,
    @ColumnInfo(name = "is_pinned")
    val isPinned: Boolean = false,
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false
)

