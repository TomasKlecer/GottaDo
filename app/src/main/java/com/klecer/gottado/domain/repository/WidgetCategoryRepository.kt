package com.klecer.gottado.domain.repository

import com.klecer.gottado.data.db.entity.CategoryEntity
import com.klecer.gottado.data.db.entity.WidgetCategoryJoinEntity

interface WidgetCategoryRepository {
    /** Categories assigned to this widget, ordered by sortOrder (with join data). */
    suspend fun getCategoriesForWidget(widgetId: Int): List<CategoryEntity>
    suspend fun getJoinsForWidget(widgetId: Int): List<WidgetCategoryJoinEntity>
    /** Widget IDs that have this category in their join (for refreshing after category change). */
    suspend fun getWidgetIdsForCategory(categoryId: Long): List<Int>
    suspend fun addCategoryToWidget(widgetId: Int, categoryId: Long, sortOrder: Int, visible: Boolean)
    suspend fun updateJoin(widgetId: Int, categoryId: Long, sortOrder: Int, visible: Boolean)
    suspend fun removeCategoryFromWidget(widgetId: Int, categoryId: Long)
    suspend fun removeAllForWidget(widgetId: Int)
}
