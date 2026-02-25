package com.klecer.gottado.domain.usecase

import com.klecer.gottado.data.db.entity.RoutineEntity
import com.klecer.gottado.domain.repository.RoutineRepository
import javax.inject.Inject

class SaveRoutineUseCase @Inject constructor(
    private val routineRepository: RoutineRepository
) {
    suspend operator fun invoke(routine: RoutineEntity): Long =
        routineRepository.insert(routine)
}
