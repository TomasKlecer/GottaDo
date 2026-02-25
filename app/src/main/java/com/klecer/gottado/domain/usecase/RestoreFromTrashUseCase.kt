package com.klecer.gottado.domain.usecase

import com.klecer.gottado.data.db.entity.TaskEntity
import com.klecer.gottado.domain.repository.CategoryRepository
import com.klecer.gottado.domain.repository.TaskRepository
import com.klecer.gottado.domain.repository.TrashRepository
import javax.inject.Inject

/**
 * Restores a task from trash into its original category (or keeps category id if category was deleted).
 */
class RestoreFromTrashUseCase @Inject constructor(
    private val trashRepository: TrashRepository,
    private val taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository
) {
    /**
     * @return true if restored, false if trash entry not found or category no longer exists and we skip
     */
    suspend operator fun invoke(trashEntryId: Long): Boolean {
        val entry = trashRepository.getById(trashEntryId) ?: return false
        val now = System.currentTimeMillis()
        val task = TaskEntity(
            id = 0L,
            categoryId = entry.originalCategoryId,
            contentHtml = entry.contentHtml,
            completed = entry.completed,
            bulletColor = entry.bulletColor,
            scheduledTimeMillis = entry.scheduledTimeMillis,
            sortOrder = entry.sortOrder,
            createdAtMillis = now,
            updatedAtMillis = now
        )
        taskRepository.insert(task)
        trashRepository.deleteById(trashEntryId)
        return true
    }
}
