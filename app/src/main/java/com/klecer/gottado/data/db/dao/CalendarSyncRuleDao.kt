package com.klecer.gottado.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.klecer.gottado.data.db.entity.CalendarSyncRuleEntity

@Dao
interface CalendarSyncRuleDao {

    @Query("SELECT * FROM calendar_sync_rule WHERE categoryId = :categoryId")
    suspend fun getRulesForCategory(categoryId: Long): List<CalendarSyncRuleEntity>

    @Query("SELECT DISTINCT categoryId FROM calendar_sync_rule")
    suspend fun getCategoryIdsWithRules(): List<Long>

    @Insert
    suspend fun insert(entity: CalendarSyncRuleEntity)

    @Query("DELETE FROM calendar_sync_rule WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM calendar_sync_rule WHERE categoryId = :categoryId")
    suspend fun deleteAllForCategory(categoryId: Long)
}
