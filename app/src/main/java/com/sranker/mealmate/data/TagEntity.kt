package com.sranker.mealmate.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a tag or category for filtering meals.
 * Tags are predefined but user-expandable (e.g. "pasta", "stew", "healthy").
 * A meal can have multiple tags via [MealTagCrossRef].
 *
 * @property id Auto-generated primary key.
 * @property name Unique display name of the tag.
 */
@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "name")
    val name: String
)

