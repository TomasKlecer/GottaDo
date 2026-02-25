package com.klecer.gottado.domain.usecase

import com.klecer.gottado.data.db.entity.CategoryEntity
import com.klecer.gottado.domain.repository.CategoryRepository
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    suspend operator fun invoke(): List<CategoryEntity> =
        categoryRepository.getAll()
}
