package com.sranker.mealmate.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Cross-reference entity for the many-to-many relationship between [MealEntity] and [TagEntity].
 *
 * @property mealId Foreign key referencing [MealEntity.id].
 * @property tagId Foreign key referencing [TagEntity.id].
 */
@Entity(
    tableName = "meal_tag_cross_ref",
    primaryKeys = ["mealId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = MealEntity::class,
            parentColumns = ["id"],
            childColumns = ["mealId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tagId")]
)
data class MealTagCrossRef(
    val mealId: Long,
    val tagId: Long
)

