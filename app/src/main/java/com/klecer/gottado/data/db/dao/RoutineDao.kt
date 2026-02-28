package com.klecer.gottado.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.klecer.gottado.data.db.entity.RoutineEntity

@Dao
interface RoutineDao {

    @Query("SELECT * FROM routine ORDER BY id ASC")
    suspend fun getAll(): List<RoutineEntity>

    @Query("SELECT * FROM routine WHERE categoryId = :categoryId ORDER BY id ASC")
    suspend fun getByCategoryId(categoryId: Long): List<RoutineEntity>

    @Query("SELECT * FROM routine WHERE id = :id")
    suspend fun getById(id: Long): RoutineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RoutineEntity): Long

    @Query("DELETE FROM routine WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<RoutineEntity>)

    @Query("DELETE FROM routine")
    suspend fun deleteAll()
}
