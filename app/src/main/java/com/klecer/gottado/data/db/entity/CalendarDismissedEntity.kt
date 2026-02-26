package com.klecer.gottado.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "calendar_dismissed",
    indices = [Index(value = ["categoryId", "eventTitle"], unique = true)]
)
data class CalendarDismissedEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val eventTitle: String
)
