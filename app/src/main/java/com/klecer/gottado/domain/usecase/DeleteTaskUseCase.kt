package com.klecer.gottado.domain.usecase

import com.klecer.gottado.data.db.entity.TrashEntryEntity
import com.klecer.gottado.domain.repository.CategoryRepository
import com.klecer.gottado.domain.repository.TaskRepository
import com.klecer.gottado.domain.repository.TrashRepository
import javax.inject.Inject

/**
 * Deletes task from tasks and moves it to trash (soft delete for restore).
 * @return the created trash entry id for Undo, or null if task not found
 */
class DeleteTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val trashRepository: TrashRepository,
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(taskId: Long): Long? {
        val task = taskRepository.getById(taskId) ?: return null
        val categoryName = categoryRepository.getById(task.categoryId)?.name ?: ""
        val trashId = trashRepository.insert(
            TrashEntryEntity(
                originalCategoryId = task.categoryId,
                contentHtml = task.contentHtml,
                completed = task.completed,
                bulletColor = task.bulletColor,
                scheduledTimeMillis = task.scheduledTimeMillis,
                sortOrder = task.sortOrder,
                deletedAtMillis = System.currentTimeMillis(),
                categoryName = categoryName
            )
        )
        taskRepository.deleteById(taskId)
        return trashId
    }
}
