package com.klecer.gottado.domain.usecase

import com.klecer.gottado.domain.model.RecordEditOptions
import com.klecer.gottado.domain.repository.RecordEditOptionsRepository
import javax.inject.Inject

class SaveRecordEditOptionsUseCase @Inject constructor(
    private val repository: RecordEditOptionsRepository
) {
    suspend operator fun invoke(options: RecordEditOptions) {
        repository.set(options)
    }
}
