package com.sranker.mealmate.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
    version = 3,
    exportSchema = true
)
abstract class MealMateDatabase : RoomDatabase() {

    abstract fun mealDao(): MealDao

    abstract fun ingredientDao(): IngredientDao

    abstract fun tagDao(): TagDao

    abstract fun menuDao(): MenuDao

    companion object {
        /**
         * Migration from version 1 to 2:
         * Adds the `is_accepted` column to the `menus` table.
         */
        val MIGRATION_1_2 = Migration(1, 2) { db ->
            db.execSQL("ALTER TABLE menus ADD COLUMN is_accepted INTEGER NOT NULL DEFAULT 0")
        }

        /**
         * Migration from version 2 to 3:
         * Adds the `created_at` column to the `meals` table for sorting by newest first.
         */
        val MIGRATION_2_3 = Migration(2, 3) { db ->
            db.execSQL("ALTER TABLE meals ADD COLUMN created_at INTEGER NOT NULL DEFAULT 0")
        }
    }
}
