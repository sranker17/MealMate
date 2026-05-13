package com.sranker.mealmate.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a weekly menu plan.
 * A menu can be in-progress (accepting pinned meals), accepted (locked),
 * or completed (archived).
 *
 * @property id Auto-generated primary key.
 * @property title Human-readable title (defaults to a date-based string).
 * @property isAccepted Whether this menu has been accepted (locked) by the user.
 * @property isCompleted Whether this menu has been finished and archived.
 * @property completionIndex Sequential index assigned when the menu is completed,
 *   used for cooldown calculations. Null when not yet completed.
 */
@Entity(tableName = "menus")
data class MenuEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    @ColumnInfo(name = "is_accepted")
    val isAccepted: Boolean = false,
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,
    @ColumnInfo(name = "completion_index")
    val completionIndex: Int? = null
)
