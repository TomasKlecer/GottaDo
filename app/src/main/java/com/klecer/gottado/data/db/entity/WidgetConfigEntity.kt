package com.klecer.gottado.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "widget_config")
data class WidgetConfigEntity(
    @PrimaryKey val widgetId: Int,
    val title: String? = null,
    val subtitle: String? = null,
    val note: String? = null,
    val backgroundColor: Int,
    val backgroundAlpha: Float,
    val categoryFontSizeSp: Float,
    val recordFontSizeSp: Float,
    val defaultTextColor: Int,
    val reorderHandlePosition: ReorderHandlePosition,
    @ColumnInfo(defaultValue = "-1") val titleColor: Int = -1,
    @ColumnInfo(defaultValue = "-1") val subtitleColor: Int = -1,
    @ColumnInfo(defaultValue = "16") val bulletSizeDp: Int = 16,
    @ColumnInfo(defaultValue = "16") val checkboxSizeDp: Int = 16,
    @ColumnInfo(defaultValue = "1") val showTitleOnWidget: Boolean = true,
    @ColumnInfo(defaultValue = "0") val buttonsAtBottom: Boolean = false
)
