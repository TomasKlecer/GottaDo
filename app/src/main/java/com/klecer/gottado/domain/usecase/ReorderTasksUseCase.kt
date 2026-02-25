package com.klecer.gottado.domain.usecase

import com.klecer.gottado.domain.repository.TaskRepository
import javax.inject.Inject

/**
 * Reorders a task within its category: set new sortOrder. Caller must ensure sortOrder values
 * are consistent (e.g. shift other tasks). For moving to another category use MoveTaskToCategoryUseCase.
 */
class ReorderTasksUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(taskId: Long, newSortOrder: Int) {
        val task = taskRepository.getById(taskId) ?: return
        taskRepository.updateOrder(taskId, newSortOrder, System.currentTimeMillis())
    }
}
