package com.klecer.gottado.domain.usecase

import com.klecer.gottado.data.db.entity.CategoryEntity
import com.klecer.gottado.domain.repository.WidgetCategoryRepository
import javax.inject.Inject

class GetCategoriesForWidgetUseCase @Inject constructor(
    private val widgetCategoryRepository: WidgetCategoryRepository
) {
    suspend operator fun invoke(widgetId: Int): List<CategoryEntity> =
        widgetCategoryRepository.getCategoriesForWidget(widgetId)
}
