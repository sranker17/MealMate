package com.sranker.mealmate.data

/**
 * Plain data class combining a [MealEntity] with its menu pivot state and tags.
 * Not processed by Room — used purely at the repository / UI layer.
 */
data class MealWithPivot(
    val meal: MealEntity,
    val isPinned: Boolean = false,
    val isCompleted: Boolean = false,
    val tags: List<TagEntity> = emptyList()
)
