package com.klecer.gottado.domain.usecase

import com.klecer.gottado.data.db.entity.TrashEntryEntity
import com.klecer.gottado.domain.model.TrashDay
import com.klecer.gottado.domain.repository.TrashRepository
import java.util.Calendar
import javax.inject.Inject

/**
 * Returns trash entries grouped by day (calendar day in default timezone). Newest day first.
 */
class GetTrashEntriesByDayUseCase @Inject constructor(
    private val trashRepository: TrashRepository
) {
    suspend operator fun invoke(): List<TrashDay> {
        val entries = trashRepository.getAllOrderedByDeletedAt()
        val calendar = Calendar.getInstance()
        val grouped = entries.groupBy { entity ->
            calendar.timeInMillis = entity.deletedAtMillis
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        }
        return grouped.entries
            .sortedByDescending { it.key }
            .map { (dayStartMillis, list) -> TrashDay(dayStartMillis = dayStartMillis, entries = list) }
    }
}
