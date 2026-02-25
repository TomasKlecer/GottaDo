package com.klecer.gottado.domain.usecase

import com.klecer.gottado.data.db.entity.TaskEntity
import com.klecer.gottado.domain.repository.TaskRepository
import javax.inject.Inject

class GetTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(taskId: Long): TaskEntity? =
        taskRepository.getById(taskId)
}
