package com.klecer.gottado.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "trash_entry",
    indices = [Index("deletedAtMillis"), Index("originalCategoryId")]
)
data class TrashEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originalCategoryId: Long,
    val contentHtml: String,
    val completed: Boolean,
    val bulletColor: Int,
    val scheduledTimeMillis: Long?,
    val sortOrder: Int,
    val deletedAtMillis: Long,
    val categoryName: String
)
