package com.sranker.mealmate.data

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/**
 * Data class representing a [MenuEntity] with its associated [MealEntity] list.
 * Used by Room's relational query support.
 * Pivot state (isPinned, isCompleted) is accessed separately via [MenuDao].
 */
data class MenuWithMeals(
    @Embedded
    val menu: MenuEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = MenuMealCrossRef::class,
            parentColumn = "menuId",
            entityColumn = "mealId"
        )
    )
    val meals: List<MealEntity>
)
