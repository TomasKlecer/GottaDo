package com.klecer.gottado.domain.usecase

import com.klecer.gottado.domain.repository.TaskRepository
import javax.inject.Inject

class ToggleTaskCompletedUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(taskId: Long) {
        val task = taskRepository.getById(taskId) ?: return
        taskRepository.updateCompleted(taskId, !task.completed, System.currentTimeMillis())
    }
}
