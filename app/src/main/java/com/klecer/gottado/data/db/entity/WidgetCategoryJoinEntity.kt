package com.klecer.gottado.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "widget_category_join",
    primaryKeys = ["widgetId", "categoryId"],
    foreignKeys = [
        ForeignKey(
            entity = WidgetConfigEntity::class,
            parentColumns = ["widgetId"],
            childColumns = ["widgetId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("widgetId"), Index("categoryId")]
)
data class WidgetCategoryJoinEntity(
    val widgetId: Int,
    val categoryId: Long,
    val sortOrder: Int,
    val visible: Boolean = true
)
