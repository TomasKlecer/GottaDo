package com.klecer.gottado.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.klecer.gottado.data.db.entity.WidgetConfigEntity

@Dao
interface WidgetConfigDao {

    @Query("SELECT * FROM widget_config WHERE widgetId = :widgetId")
    suspend fun getByWidgetId(widgetId: Int): WidgetConfigEntity?

    @Query("SELECT * FROM widget_config ORDER BY widgetId")
    suspend fun getAll(): List<WidgetConfigEntity>

    @Upsert
    suspend fun insert(entity: WidgetConfigEntity)

    @Upsert
    suspend fun insertAll(entities: List<WidgetConfigEntity>)

    @Query("DELETE FROM widget_config WHERE widgetId = :widgetId")
    suspend fun deleteByWidgetId(widgetId: Int)
}
