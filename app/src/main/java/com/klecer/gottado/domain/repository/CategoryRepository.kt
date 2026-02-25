package com.klecer.gottado.domain.repository

import com.klecer.gottado.data.db.entity.CategoryEntity

/**
 * Extension point: categoryType field supports "today", "tomorrow", weekday types.
 * Calendar sync will query categories by type to determine which calendar events to sync.
 * SyncCalendarUseCase will call getAll(), filter by type, then create/update tasks.
 */
interface CategoryRepository {
    suspend fun getAll(): List<CategoryEntity>
    suspend fun getById(id: Long): CategoryEntity?
    suspend fun insert(entity: CategoryEntity): Long
    suspend fun update(entity: CategoryEntity)
    suspend fun deleteById(id: Long)
}
