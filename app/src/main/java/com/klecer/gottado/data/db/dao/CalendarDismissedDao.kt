package com.klecer.gottado.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.klecer.gottado.data.db.entity.CalendarDismissedEntity

@Dao
interface CalendarDismissedDao {

    @Query("SELECT eventTitle FROM calendar_dismissed WHERE categoryId = :categoryId")
    suspend fun getDismissedTitles(categoryId: Long): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: CalendarDismissedEntity)
}
