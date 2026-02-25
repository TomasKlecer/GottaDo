package com.klecer.gottado.domain.model

import com.klecer.gottado.data.db.entity.TrashEntryEntity

/**
 * Trash entries grouped by day (date at start of day in millis) for display.
 */
data class TrashDay(
    val dayStartMillis: Long,
    val entries: List<TrashEntryEntity>
)
