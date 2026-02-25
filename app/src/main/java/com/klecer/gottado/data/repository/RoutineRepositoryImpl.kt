package com.klecer.gottado.data.repository

import com.klecer.gottado.data.db.dao.RoutineDao
import com.klecer.gottado.data.db.entity.RoutineEntity
import com.klecer.gottado.domain.repository.RoutineRepository
import javax.inject.Inject

class RoutineRepositoryImpl @Inject constructor(
    private val dao: RoutineDao
) : RoutineRepository {

    override suspend fun getAll(): List<RoutineEntity> =
        dao.getAll()

    override suspend fun getByCategoryId(categoryId: Long): List<RoutineEntity> =
        dao.getByCategoryId(categoryId)

    override suspend fun getById(id: Long): RoutineEntity? =
        dao.getById(id)

    override suspend fun insert(entity: RoutineEntity): Long =
        dao.insert(entity)

    override suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }
}
