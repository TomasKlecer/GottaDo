package com.klecer.gottado.ui.screen.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klecer.gottado.data.db.entity.TrashEntryEntity
import com.klecer.gottado.domain.usecase.ClearTrashUseCase
import com.klecer.gottado.domain.usecase.DeleteFromTrashUseCase
import com.klecer.gottado.domain.usecase.RestoreFromTrashUseCase
import com.klecer.gottado.domain.repository.TrashRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

enum class HistoryViewMode { BY_CATEGORY, BY_TIME }

data class HistoryCategoryGroup(
    val categoryName: String,
    val timeGroups: List<HistoryDateGroup>
)

data class HistoryDateGroup(
    val label: String,
    val entries: List<TrashEntryEntity>
)

data class HistoryYearGroup(
    val year: Int,
    val months: List<HistoryMonthGroup>
)

data class HistoryMonthGroup(
    val label: String,
    val yearMonth: String,
    val weeks: List<HistoryWeekGroup>
)

data class HistoryWeekGroup(
    val label: String,
    val entries: List<TrashEntryEntity>
)

data class HistoryState(
    val entries: List<TrashEntryEntity> = emptyList(),
    val viewMode: HistoryViewMode = HistoryViewMode.BY_CATEGORY,
    val categoryGroups: List<HistoryCategoryGroup> = emptyList(),
    val yearGroups: List<HistoryYearGroup> = emptyList()
)

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val trashRepository: TrashRepository,
    private val restoreFromTrashUseCase: RestoreFromTrashUseCase,
    private val deleteFromTrashUseCase: DeleteFromTrashUseCase,
    private val clearTrashUseCase: ClearTrashUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryState())
    val state: StateFlow<HistoryState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            val entries = trashRepository.getAllOrderedByDeletedAt()
            val mode = _state.value.viewMode
            _state.value = _state.value.copy(
                entries = entries,
                categoryGroups = buildCategoryGroups(entries),
                yearGroups = buildYearGroups(entries)
            )
        }
    }

    fun setViewMode(mode: HistoryViewMode) {
        _state.value = _state.value.copy(viewMode = mode)
    }

    fun restore(trashEntryId: Long) {
        viewModelScope.launch {
            restoreFromTrashUseCase(trashEntryId)
            load()
        }
    }

    fun deletePermanently(trashEntryId: Long) {
        viewModelScope.launch {
            deleteFromTrashUseCase(trashEntryId)
            load()
        }
    }

    fun updateContent(trashEntryId: Long, newContentHtml: String) {
        viewModelScope.launch {
            trashRepository.updateContent(trashEntryId, newContentHtml)
            load()
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            clearTrashUseCase()
            load()
        }
    }

    private fun buildCategoryGroups(entries: List<TrashEntryEntity>): List<HistoryCategoryGroup> {
        val cal = Calendar.getInstance()
        return entries.groupBy { it.categoryName }
            .map { (catName, catEntries) ->
                val timeGroups = catEntries
                    .groupBy { entry ->
                        cal.timeInMillis = entry.deletedAtMillis
                        formatDate(cal)
                    }
                    .map { (dateLabel, dateEntries) ->
                        HistoryDateGroup(label = dateLabel, entries = dateEntries)
                    }
                HistoryCategoryGroup(categoryName = catName, timeGroups = timeGroups)
            }
            .sortedBy { it.categoryName }
    }

    private fun buildYearGroups(entries: List<TrashEntryEntity>): List<HistoryYearGroup> {
        val cal = Calendar.getInstance()
        return entries.groupBy { entry ->
            cal.timeInMillis = entry.deletedAtMillis
            cal.get(Calendar.YEAR)
        }.entries.sortedByDescending { it.key }.map { (year, yearEntries) ->
            val monthGroups = yearEntries.groupBy { entry ->
                cal.timeInMillis = entry.deletedAtMillis
                cal.get(Calendar.MONTH)
            }.entries.sortedByDescending { it.key }.map { (month, monthEntries) ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                val monthLabel = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
                    ?: "Month ${month + 1}"
                val yearMonthKey = "$year-$month"

                val weekGroups = monthEntries.groupBy { entry ->
                    cal.timeInMillis = entry.deletedAtMillis
                    cal.get(Calendar.WEEK_OF_YEAR)
                }.entries.sortedByDescending { it.key }.map { (week, weekEntries) ->
                    cal.set(Calendar.WEEK_OF_YEAR, week)
                    cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                    val weekStart = "${cal.get(Calendar.DAY_OF_MONTH)}. ${
                        cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault())
                    }"
                    HistoryWeekGroup(
                        label = "Week of $weekStart",
                        entries = weekEntries
                    )
                }

                HistoryMonthGroup(
                    label = "$monthLabel $year",
                    yearMonth = yearMonthKey,
                    weeks = weekGroups
                )
            }
            HistoryYearGroup(year = year, months = monthGroups)
        }
    }

    private fun formatDate(cal: Calendar): String {
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val monthName = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault())
        val year = cal.get(Calendar.YEAR)
        val dayName = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault())
        return "$dayName, $day. $monthName $year"
    }
}
