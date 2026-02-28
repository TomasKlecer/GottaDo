package com.klecer.gottado.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "category")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortOrder: Int,
    val showCheckboxInsteadOfBullet: Boolean = false,
    val tasksWithTimeFirst: Boolean = true,
    val categoryType: String = "normal",
    @ColumnInfo(defaultValue = "0")
    val color: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val syncWithCalendarToday: Boolean = false,
    @ColumnInfo(defaultValue = "1")
    val showCalendarIcon: Boolean = true,
    @ColumnInfo(defaultValue = "0")
    val showDeleteButton: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val notifyOnTime: Boolean = false,
    @ColumnInfo(defaultValue = "15")
    val notifyMinutesBefore: Int = 15,
    @ColumnInfo(defaultValue = "1")
    val autoSortTimedEntries: Boolean = true,
    @ColumnInfo(defaultValue = "1")
    val timedEntriesAscending: Boolean = true,
    @ColumnInfo(defaultValue = "0")
    val defaultBulletColor: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val defaultTextColor: Int = 0
)
