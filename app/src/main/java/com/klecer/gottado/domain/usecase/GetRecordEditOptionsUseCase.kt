package com.klecer.gottado.domain.usecase

import com.klecer.gottado.domain.model.RecordEditOptions
import com.klecer.gottado.domain.repository.RecordEditOptionsRepository
import javax.inject.Inject

class GetRecordEditOptionsUseCase @Inject constructor(
    private val repository: RecordEditOptionsRepository
) {
    operator fun invoke(): RecordEditOptions = repository.get()
}
