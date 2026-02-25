package com.klecer.gottado.domain.repository

import com.klecer.gottado.domain.model.RecordEditOptions

interface RecordEditOptionsRepository {
    fun get(): RecordEditOptions
    suspend fun set(options: RecordEditOptions)
}
