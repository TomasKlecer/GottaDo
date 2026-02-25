package com.klecer.gottado.domain.usecase

import com.klecer.gottado.domain.repository.TrashRepository
import javax.inject.Inject

/** Permanently deletes one entry from trash. */
class DeleteFromTrashUseCase @Inject constructor(
    private val trashRepository: TrashRepository
) {
    suspend operator fun invoke(trashEntryId: Long) {
        trashRepository.deleteById(trashEntryId)
    }
}
