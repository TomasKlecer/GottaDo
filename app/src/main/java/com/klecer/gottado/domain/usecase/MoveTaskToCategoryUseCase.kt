package com.klecer.gottado.domain.usecase

import com.klecer.gottado.domain.repository.TaskRepository
import javax.inject.Inject

/**
 * Moves a task to another category and sets its sort order there.
 */
class MoveTaskToCategoryUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(taskId: Long, targetCategoryId: Long, sortOrder: Int) {
        taskRepository.updateCategoryAndOrder(taskId, targetCategoryId, sortOrder, System.currentTimeMillis())
    }
}
