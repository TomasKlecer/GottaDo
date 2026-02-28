package com.klecer.gottado.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.klecer.gottado.data.db.entity.CategoryEntity
import com.klecer.gottado.data.db.entity.WidgetCategoryJoinEntity

@Dao
interface WidgetCategoryJoinDao {

    @Query("SELECT * FROM widget_category_join")
    suspend fun getAll(): List<WidgetCategoryJoinEntity>

    @Query("""
        SELECT c.* FROM category c
        INNER JOIN widget_category_join wcj ON wcj.categoryId = c.id
        WHERE wcj.widgetId = :widgetId
        ORDER BY wcj.sortOrder ASC, c.id ASC
    """)
    suspend fun getCategoriesForWidget(widgetId: Int): List<CategoryEntity>

    @Query("SELECT * FROM widget_category_join WHERE widgetId = :widgetId ORDER BY sortOrder ASC")
    suspend fun getJoinsForWidget(widgetId: Int): List<WidgetCategoryJoinEntity>

    @Query("SELECT DISTINCT widgetId FROM widget_category_join WHERE categoryId = :categoryId")
    suspend fun getWidgetIdsForCategory(categoryId: Long): List<Int>

    @Query("SELECT * FROM widget_category_join WHERE widgetId = :widgetId AND categoryId = :categoryId")
    suspend fun getJoin(widgetId: Int, categoryId: Long): WidgetCategoryJoinEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(join: WidgetCategoryJoinEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(joins: List<WidgetCategoryJoinEntity>)

    @Query("UPDATE widget_category_join SET sortOrder = :sortOrder, visible = :visible WHERE widgetId = :widgetId AND categoryId = :categoryId")
    suspend fun updateJoin(widgetId: Int, categoryId: Long, sortOrder: Int, visible: Boolean)

    @Query("DELETE FROM widget_category_join WHERE widgetId = :widgetId AND categoryId = :categoryId")
    suspend fun delete(widgetId: Int, categoryId: Long)

    @Query("DELETE FROM widget_category_join WHERE widgetId = :widgetId")
    suspend fun deleteAllForWidget(widgetId: Int)

    @Query("DELETE FROM widget_category_join WHERE categoryId = :categoryId")
    suspend fun deleteAllForCategory(categoryId: Long)

    @Query("DELETE FROM widget_category_join")
    suspend fun deleteAll()
}
