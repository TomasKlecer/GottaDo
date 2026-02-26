package com.klecer.gottado.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.klecer.gottado.data.db.entity.TrashEntryEntity

@Dao
interface TrashEntryDao {

    @Query("SELECT * FROM trash_entry ORDER BY deletedAtMillis DESC")
    suspend fun getAll(): List<TrashEntryEntity>

    @Query("SELECT * FROM trash_entry ORDER BY deletedAtMillis DESC")
    suspend fun getAllOrderedByDeletedAt(): List<TrashEntryEntity>

    @Query("SELECT * FROM trash_entry WHERE id = :id")
    suspend fun getById(id: Long): TrashEntryEntity?

    @Insert
    suspend fun insert(entity: TrashEntryEntity): Long

    @Query("UPDATE trash_entry SET contentHtml = :contentHtml WHERE id = :id")
    suspend fun updateContent(id: Long, contentHtml: String)

    @Query("DELETE FROM trash_entry WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM trash_entry")
    suspend fun deleteAll()
}
