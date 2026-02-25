package com.klecer.gottado.domain.usecase

import com.klecer.gottado.data.db.entity.TaskEntity
import com.klecer.gottado.domain.repository.TaskRepository
import javax.inject.Inject

class SaveTaskUseCase @Inject constructor(
    private val taskRepository: TaskRepository
) {
    /**
     * Insert new task (id = 0) or update existing. Returns the task id.
     */
    suspend operator fun invoke(task: TaskEntity): Long {
        return if (task.id == 0L) {
            taskRepository.insert(task)
        } else {
            taskRepository.update(task)
            task.id
        }
    }
}
