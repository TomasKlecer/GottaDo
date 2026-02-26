package com.klecer.gottado.domain.repository

import com.klecer.gottado.data.db.entity.TrashEntryEntity

/**
 * Extension point: The trash table doubles as a journal/diary.
 * For "Trash as journal" extension, add filtering/sorting methods and richer day view queries here.
 * No schema changes needed — only UI and optional export functionality.
 */
interface TrashRepository {
    suspend fun getAllOrderedByDeletedAt(): List<TrashEntryEntity>
    suspend fun getById(id: Long): TrashEntryEntity?
    suspend fun insert(entity: TrashEntryEntity): Long
    suspend fun updateContent(id: Long, contentHtml: String)
    suspend fun deleteById(id: Long)
    suspend fun deleteAll()
}
