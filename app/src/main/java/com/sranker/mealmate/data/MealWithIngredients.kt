package com.sranker.mealmate.data

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Data class representing a [MealEntity] with its list of [IngredientEntity].
 * Used by Room relational queries.
 */
data class MealWithIngredients(
    @Embedded
    val meal: MealEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "mealId"
    )
    val ingredients: List<IngredientEntity>
)

