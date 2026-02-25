package com.klecer.gottado.domain.usecase

import com.klecer.gottado.domain.repository.RoutineRepository
import javax.inject.Inject

class DeleteRoutineUseCase @Inject constructor(
    private val routineRepository: RoutineRepository
) {
    suspend operator fun invoke(routineId: Long) {
        routineRepository.deleteById(routineId)
    }
}
