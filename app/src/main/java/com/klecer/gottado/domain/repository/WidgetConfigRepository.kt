package com.klecer.gottado.domain.repository

import com.klecer.gottado.data.db.entity.WidgetConfigEntity

interface WidgetConfigRepository {
    suspend fun getByWidgetId(widgetId: Int): WidgetConfigEntity?
    suspend fun getAll(): List<WidgetConfigEntity>
    suspend fun insert(entity: WidgetConfigEntity)
    suspend fun deleteByWidgetId(widgetId: Int)
}
