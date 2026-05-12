package com.sranker.mealmate.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module providing Room database and DAO dependencies.
 * Concrete bindings will be added once entities and DAOs are defined.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule
