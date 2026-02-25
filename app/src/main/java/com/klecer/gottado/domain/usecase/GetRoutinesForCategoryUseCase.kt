package com.klecer.gottado.domain.usecase

import com.klecer.gottado.data.db.entity.RoutineEntity
import com.klecer.gottado.domain.repository.RoutineRepository
import javax.inject.Inject

/** CRUD for routines; execution logic in Stage 5. */
class GetRoutinesForCategoryUseCase @Inject constructor(
    private val routineRepository: RoutineRepository
) {
    suspend operator fun invoke(categoryId: Long): List<RoutineEntity> =
        routineRepository.getByCategoryId(categoryId)
}
