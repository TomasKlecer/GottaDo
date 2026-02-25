package com.klecer.gottado.domain.repository

import com.klecer.gottado.data.db.entity.RoutineEntity

interface RoutineRepository {
    suspend fun getAll(): List<RoutineEntity>
    suspend fun getByCategoryId(categoryId: Long): List<RoutineEntity>
    suspend fun getById(id: Long): RoutineEntity?
    suspend fun insert(entity: RoutineEntity): Long
    suspend fun deleteById(id: Long)
}
