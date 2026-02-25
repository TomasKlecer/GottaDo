package com.klecer.gottado.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.klecer.gottado.data.db.entity.TaskEntity

@Dao
interface TaskDao {

    @Query("SELECT * FROM task WHERE id = :id")
    suspend fun getById(id: Long): TaskEntity?

    @Query("""
        SELECT * FROM task WHERE categoryId = :categoryId
        ORDER BY 
            CASE WHEN scheduledTimeMillis IS NULL THEN 1 ELSE 0 END,
            scheduledTimeMillis ASC,
            sortOrder ASC,
            id ASC
    """)
    suspend fun getByCategoryOrderedByTimeThenOrder(categoryId: Long): List<TaskEntity>

    /**
     * Tasks in category ordered by: tasks with time first (or last per category setting), then by time, then sortOrder.
     * Caller should apply tasksWithTimeFirst ordering; this returns natural order (time ASC, sortOrder ASC).
     */
    @Query("""
        SELECT * FROM task WHERE categoryId = :categoryId
        ORDER BY scheduledTimeMillis ASC, sortOrder ASC, id ASC
    """)
    suspend fun getByCategory(categoryId: Long): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<TaskEntity>)

    @Query("UPDATE task SET contentHtml = :contentHtml, completed = :completed, bulletColor = :bulletColor, textColor = :textColor, scheduledTimeMillis = :scheduledTimeMillis, sortOrder = :sortOrder, updatedAtMillis = :updatedAtMillis WHERE id = :id")
    suspend fun update(id: Long, contentHtml: String, completed: Boolean, bulletColor: Int, textColor: Int, scheduledTimeMillis: Long?, sortOrder: Int, updatedAtMillis: Long)

    @Query("UPDATE task SET categoryId = :categoryId, sortOrder = :sortOrder, updatedAtMillis = :updatedAtMillis WHERE id = :id")
    suspend fun updateCategoryAndOrder(id: Long, categoryId: Long, sortOrder: Int, updatedAtMillis: Long)

    @Query("UPDATE task SET sortOrder = :sortOrder, updatedAtMillis = :updatedAtMillis WHERE id = :id")
    suspend fun updateOrder(id: Long, sortOrder: Int, updatedAtMillis: Long)

    @Query("UPDATE task SET completed = :completed, updatedAtMillis = :updatedAtMillis WHERE id = :id")
    suspend fun updateCompleted(id: Long, completed: Boolean, updatedAtMillis: Long)

    @Query("DELETE FROM task WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM task WHERE categoryId = :categoryId")
    suspend fun deleteByCategoryId(categoryId: Long)
}
