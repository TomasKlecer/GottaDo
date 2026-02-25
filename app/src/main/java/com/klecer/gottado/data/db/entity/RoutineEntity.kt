package com.klecer.gottado.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "routine",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId")]
)
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val name: String? = null,
    val frequency: RoutineFrequency,
    val scheduleTimeHour: Int,
    val scheduleTimeMinute: Int,
    val scheduleDayOfWeek: Int? = null,      // Calendar.SUNDAY..SATURDAY for WEEKLY
    val scheduleDayOfMonth: Int? = null,     // 1..31 for MONTHLY
    val scheduleMonth: Int? = null,           // Calendar.JANUARY..DECEMBER for YEARLY
    val scheduleDay: Int? = null,             // day of month for YEARLY
    val visibilityMode: RoutineVisibilityMode = RoutineVisibilityMode.VISIBLE,
    val visibilityFrom: Long? = null,           // type-dependent: time millis or day start millis
    val visibilityTo: Long? = null,
    val incompleteAction: RoutineTaskAction,
    val incompleteMoveToCategoryId: Long? = null,
    val completedAction: RoutineTaskAction,
    val completedMoveToCategoryId: Long? = null
)
