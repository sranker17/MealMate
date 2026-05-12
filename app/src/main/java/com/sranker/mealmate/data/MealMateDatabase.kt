package com.sranker.mealmate.data

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room database for the MealMate application.
 * Contains all entities and DAOs for meal management, menu planning,
 * and the cooldown system.
 */
@Database(
    entities = [
        MealEntity::class,
        IngredientEntity::class,
        TagEntity::class,
        MealTagCrossRef::class,
        MenuEntity::class,
        MenuMealCrossRef::class
    ],
    version = 1,
    exportSchema = true
)
abstract class MealMateDatabase : RoomDatabase() {

    abstract fun mealDao(): MealDao

    abstract fun ingredientDao(): IngredientDao

    abstract fun tagDao(): TagDao

    abstract fun menuDao(): MenuDao
}

