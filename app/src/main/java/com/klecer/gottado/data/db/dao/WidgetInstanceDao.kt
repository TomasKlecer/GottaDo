package com.klecer.gottado.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.klecer.gottado.data.db.entity.WidgetInstanceEntity

@Dao
interface WidgetInstanceDao {

    @Query("SELECT presetId FROM widget_instance WHERE appWidgetId = :appWidgetId")
    suspend fun getPresetId(appWidgetId: Int): Int?

    @Query("SELECT * FROM widget_instance")
    suspend fun getAll(): List<WidgetInstanceEntity>

    @Upsert
    suspend fun upsert(entity: WidgetInstanceEntity)

    @Query("DELETE FROM widget_instance WHERE appWidgetId = :appWidgetId")
    suspend fun deleteByAppWidgetId(appWidgetId: Int)
}
