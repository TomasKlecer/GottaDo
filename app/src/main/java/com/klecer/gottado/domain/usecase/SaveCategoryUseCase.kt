package com.klecer.gottado.domain.usecase

import com.klecer.gottado.data.db.entity.CategoryEntity
import com.klecer.gottado.domain.repository.CategoryRepository
import javax.inject.Inject

class SaveCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository
) {
    /**
     * Insert (id = 0) or update. Returns the category id.
     */
    suspend operator fun invoke(category: CategoryEntity): Long {
        return if (category.id == 0L) {
            categoryRepository.insert(category)
        } else {
            categoryRepository.update(category)
            category.id
        }
    }
}
