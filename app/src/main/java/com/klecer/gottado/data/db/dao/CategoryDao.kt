package com.klecer.gottado.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.klecer.gottado.data.db.entity.CategoryEntity

@Dao
interface CategoryDao {

    @Query("SELECT * FROM category ORDER BY sortOrder ASC, id ASC")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT * FROM category WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<CategoryEntity>)

    @Query("UPDATE category SET name = :name, sortOrder = :sortOrder, showCheckboxInsteadOfBullet = :showCheckboxInsteadOfBullet, tasksWithTimeFirst = :tasksWithTimeFirst, categoryType = :categoryType, color = :color, syncWithCalendarToday = :syncWithCalendarToday, showCalendarIcon = :showCalendarIcon, showDeleteButton = :showDeleteButton WHERE id = :id")
    suspend fun update(id: Long, name: String, sortOrder: Int, showCheckboxInsteadOfBullet: Boolean, tasksWithTimeFirst: Boolean, categoryType: String, color: Int, syncWithCalendarToday: Boolean, showCalendarIcon: Boolean, showDeleteButton: Boolean)

    @Query("DELETE FROM category WHERE id = :id")
    suspend fun deleteById(id: Long)
}
