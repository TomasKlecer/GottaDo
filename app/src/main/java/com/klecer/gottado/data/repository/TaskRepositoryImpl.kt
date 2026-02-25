package com.klecer.gottado.data.repository

import com.klecer.gottado.data.db.dao.TaskDao
import com.klecer.gottado.data.db.entity.TaskEntity
import com.klecer.gottado.domain.repository.TaskRepository
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val dao: TaskDao
) : TaskRepository {

    override suspend fun getById(id: Long): TaskEntity? =
        dao.getById(id)

    override suspend fun getByCategory(categoryId: Long): List<TaskEntity> =
        dao.getByCategory(categoryId)

    override suspend fun insert(entity: TaskEntity): Long =
        dao.insert(entity)

    override suspend fun update(entity: TaskEntity) {
        dao.update(
            id = entity.id,
            contentHtml = entity.contentHtml,
            completed = entity.completed,
            bulletColor = entity.bulletColor,
            textColor = entity.textColor,
            scheduledTimeMillis = entity.scheduledTimeMillis,
            sortOrder = entity.sortOrder,
            updatedAtMillis = entity.updatedAtMillis
        )
    }

    override suspend fun updateCategoryAndOrder(id: Long, categoryId: Long, sortOrder: Int, updatedAtMillis: Long) {
        dao.updateCategoryAndOrder(id, categoryId, sortOrder, updatedAtMillis)
    }

    override suspend fun updateOrder(id: Long, sortOrder: Int, updatedAtMillis: Long) {
        dao.updateOrder(id, sortOrder, updatedAtMillis)
    }

    override suspend fun updateCompleted(id: Long, completed: Boolean, updatedAtMillis: Long) {
        dao.updateCompleted(id, completed, updatedAtMillis)
    }

    override suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }
}
