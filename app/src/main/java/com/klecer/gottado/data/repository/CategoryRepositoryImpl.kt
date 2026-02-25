package com.klecer.gottado.data.repository

import com.klecer.gottado.data.db.dao.CategoryDao
import com.klecer.gottado.data.db.entity.CategoryEntity
import com.klecer.gottado.domain.repository.CategoryRepository
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val dao: CategoryDao
) : CategoryRepository {

    override suspend fun getAll(): List<CategoryEntity> =
        dao.getAll()

    override suspend fun getById(id: Long): CategoryEntity? =
        dao.getById(id)

    override suspend fun insert(entity: CategoryEntity): Long =
        dao.insert(entity)

    override suspend fun update(entity: CategoryEntity) {
        dao.update(
            id = entity.id,
            name = entity.name,
            sortOrder = entity.sortOrder,
            showCheckboxInsteadOfBullet = entity.showCheckboxInsteadOfBullet,
            tasksWithTimeFirst = entity.tasksWithTimeFirst,
            categoryType = entity.categoryType,
            color = entity.color
        )
    }

    override suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }
}
