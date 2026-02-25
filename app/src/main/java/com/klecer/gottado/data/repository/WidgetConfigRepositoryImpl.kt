package com.klecer.gottado.data.repository

import com.klecer.gottado.data.db.dao.WidgetConfigDao
import com.klecer.gottado.data.db.entity.WidgetConfigEntity
import com.klecer.gottado.domain.repository.WidgetConfigRepository
import javax.inject.Inject

class WidgetConfigRepositoryImpl @Inject constructor(
    private val dao: WidgetConfigDao
) : WidgetConfigRepository {

    override suspend fun getByWidgetId(widgetId: Int): WidgetConfigEntity? =
        dao.getByWidgetId(widgetId)

    override suspend fun getAll(): List<WidgetConfigEntity> =
        dao.getAll()

    override suspend fun insert(entity: WidgetConfigEntity) {
        dao.insert(entity)
    }

    override suspend fun deleteByWidgetId(widgetId: Int) {
        dao.deleteByWidgetId(widgetId)
    }
}
