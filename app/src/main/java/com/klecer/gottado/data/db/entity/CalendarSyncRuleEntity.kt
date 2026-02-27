package com.klecer.gottado.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calendar_sync_rule")
data class CalendarSyncRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val ruleType: String
)
