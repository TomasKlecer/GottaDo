package com.klecer.gottado.data.repository

import com.klecer.gottado.data.db.dao.TrashEntryDao
import com.klecer.gottado.data.db.entity.TrashEntryEntity
import com.klecer.gottado.domain.repository.TrashRepository
import javax.inject.Inject

class TrashRepositoryImpl @Inject constructor(
    private val dao: TrashEntryDao
) : TrashRepository {

    override suspend fun getAllOrderedByDeletedAt(): List<TrashEntryEntity> =
        dao.getAllOrderedByDeletedAt()

    override suspend fun getById(id: Long): TrashEntryEntity? =
        dao.getById(id)

    override suspend fun insert(entity: TrashEntryEntity): Long =
        dao.insert(entity)

    override suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun deleteAll() {
        dao.deleteAll()
    }
}
