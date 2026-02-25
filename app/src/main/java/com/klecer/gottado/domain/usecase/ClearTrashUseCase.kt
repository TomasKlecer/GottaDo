package com.klecer.gottado.domain.usecase

import com.klecer.gottado.domain.repository.TrashRepository
import javax.inject.Inject

class ClearTrashUseCase @Inject constructor(
    private val trashRepository: TrashRepository
) {
    suspend operator fun invoke() {
        trashRepository.deleteAll()
    }
}
