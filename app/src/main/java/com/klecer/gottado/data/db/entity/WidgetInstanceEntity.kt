package com.klecer.gottado.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "widget_instance")
data class WidgetInstanceEntity(
    @PrimaryKey val appWidgetId: Int,
    val presetId: Int
)
