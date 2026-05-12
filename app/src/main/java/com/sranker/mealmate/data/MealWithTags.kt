package com.sranker.mealmate.data

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/**
 * Data class representing a [MealEntity] with its associated [TagEntity] list.
 * Used by Room's relational query support.
 */
data class MealWithTags(
    @Embedded
    val meal: MealEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = MealTagCrossRef::class,
            parentColumn = "mealId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity>
)

