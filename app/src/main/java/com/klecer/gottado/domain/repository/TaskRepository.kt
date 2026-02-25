package com.klecer.gottado.domain.repository

import com.klecer.gottado.data.db.entity.TaskEntity

interface TaskRepository {
    suspend fun getById(id: Long): TaskEntity?
    /** All tasks in category in natural order (time ASC, sortOrder ASC). Apply tasksWithTimeFirst in use case. */
    suspend fun getByCategory(categoryId: Long): List<TaskEntity>
    suspend fun insert(entity: TaskEntity): Long
    suspend fun update(entity: TaskEntity)
    suspend fun updateCategoryAndOrder(id: Long, categoryId: Long, sortOrder: Int, updatedAtMillis: Long)
    suspend fun updateOrder(id: Long, sortOrder: Int, updatedAtMillis: Long)
    suspend fun updateCompleted(id: Long, completed: Boolean, updatedAtMillis: Long)
    suspend fun deleteById(id: Long)
}
