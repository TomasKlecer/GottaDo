package com.klecer.gottado.data.repository

import com.klecer.gottado.data.db.dao.WidgetCategoryJoinDao
import com.klecer.gottado.data.db.entity.CategoryEntity
import com.klecer.gottado.data.db.entity.WidgetCategoryJoinEntity
import com.klecer.gottado.domain.repository.WidgetCategoryRepository
import javax.inject.Inject

class WidgetCategoryRepositoryImpl @Inject constructor(
    private val dao: WidgetCategoryJoinDao
) : WidgetCategoryRepository {

    override suspend fun getCategoriesForWidget(widgetId: Int): List<CategoryEntity> =
        dao.getCategoriesForWidget(widgetId)

    override suspend fun getJoinsForWidget(widgetId: Int): List<WidgetCategoryJoinEntity> =
        dao.getJoinsForWidget(widgetId)

    override suspend fun getWidgetIdsForCategory(categoryId: Long): List<Int> =
        dao.getWidgetIdsForCategory(categoryId)

    override suspend fun addCategoryToWidget(widgetId: Int, categoryId: Long, sortOrder: Int, visible: Boolean) {
        dao.insert(WidgetCategoryJoinEntity(widgetId = widgetId, categoryId = categoryId, sortOrder = sortOrder, visible = visible))
    }

    override suspend fun updateJoin(widgetId: Int, categoryId: Long, sortOrder: Int, visible: Boolean) {
        dao.updateJoin(widgetId, categoryId, sortOrder, visible)
    }

    override suspend fun removeCategoryFromWidget(widgetId: Int, categoryId: Long) {
        dao.delete(widgetId, categoryId)
    }

    override suspend fun removeAllForWidget(widgetId: Int) {
        dao.deleteAllForWidget(widgetId)
    }
}
