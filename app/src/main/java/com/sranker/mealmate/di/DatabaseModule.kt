package com.sranker.mealmate.di

import android.content.Context
import androidx.room.Room
import com.sranker.mealmate.data.IngredientDao
import com.sranker.mealmate.data.MealDao
import com.sranker.mealmate.data.MealMateDatabase
import com.sranker.mealmate.data.MenuDao
import com.sranker.mealmate.data.TagDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing the Room database and all DAO instances.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MealMateDatabase =
        Room.databaseBuilder(
            context,
            MealMateDatabase::class.java,
            "meal_mate_database"
        ).addMigrations(MealMateDatabase.MIGRATION_1_2)
            .build()

    @Provides
    fun provideMealDao(database: MealMateDatabase): MealDao = database.mealDao()

    @Provides
    fun provideIngredientDao(database: MealMateDatabase): IngredientDao = database.ingredientDao()

    @Provides
    fun provideTagDao(database: MealMateDatabase): TagDao = database.tagDao()

    @Provides
    fun provideMenuDao(database: MealMateDatabase): MenuDao = database.menuDao()
}
