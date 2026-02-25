package com.klecer.gottado.ui.screen.routineedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klecer.gottado.data.db.entity.CategoryEntity
import com.klecer.gottado.data.db.entity.RoutineEntity
import com.klecer.gottado.data.db.entity.RoutineFrequency
import com.klecer.gottado.data.db.entity.RoutineTaskAction
import com.klecer.gottado.data.db.entity.RoutineVisibilityMode
import com.klecer.gottado.domain.repository.RoutineRepository
import com.klecer.gottado.domain.usecase.DeleteRoutineUseCase
import com.klecer.gottado.domain.usecase.GetCategoriesUseCase
import com.klecer.gottado.domain.usecase.SaveRoutineUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoutineEditState(
    val isNew: Boolean = true,
    val id: Long = 0,
    val categoryId: Long = 0,
    val name: String = "",
    val frequency: RoutineFrequency = RoutineFrequency.DAILY,
    val hour: Int = 8,
    val minute: Int = 0,
    val dayOfWeek: Int? = null,
    val dayOfMonth: Int? = null,
    val month: Int? = null,
    val day: Int? = null,
    val visibilityMode: RoutineVisibilityMode = RoutineVisibilityMode.VISIBLE,
    val visibilityFrom: Long? = null,
    val visibilityTo: Long? = null,
    val incompleteAction: RoutineTaskAction = RoutineTaskAction.DELETE,
    val incompleteMoveToCategoryId: Long? = null,
    val completedAction: RoutineTaskAction = RoutineTaskAction.DELETE,
    val completedMoveToCategoryId: Long? = null,
    val categories: List<CategoryEntity> = emptyList(),
    val isLoading: Boolean = true,
    val saved: Boolean = false
)

@HiltViewModel
class RoutineEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val saveRoutineUseCase: SaveRoutineUseCase,
    private val deleteRoutineUseCase: DeleteRoutineUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val routineRepository: RoutineRepository
) : ViewModel() {

    private val routineId: Long = savedStateHandle.get<String>("routineId")?.toLongOrNull() ?: 0L
    private val initialCategoryId: Long = savedStateHandle.get<String>("categoryId")?.toLongOrNull() ?: 0L

    private val _state = MutableStateFlow(RoutineEditState())
    val state: StateFlow<RoutineEditState> = _state.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            val categories = getCategoriesUseCase()
            if (routineId > 0) {
                val entity = routineRepository.getById(routineId)
                if (entity != null) {
                    _state.value = RoutineEditState(
                        isNew = false,
                        id = entity.id,
                        categoryId = entity.categoryId,
                        name = entity.name ?: "",
                        frequency = entity.frequency,
                        hour = entity.scheduleTimeHour,
                        minute = entity.scheduleTimeMinute,
                        dayOfWeek = entity.scheduleDayOfWeek,
                        dayOfMonth = entity.scheduleDayOfMonth,
                        month = entity.scheduleMonth,
                        day = entity.scheduleDay,
                        visibilityMode = entity.visibilityMode,
                        visibilityFrom = entity.visibilityFrom,
                        visibilityTo = entity.visibilityTo,
                        incompleteAction = entity.incompleteAction,
                        incompleteMoveToCategoryId = entity.incompleteMoveToCategoryId,
                        completedAction = entity.completedAction,
                        completedMoveToCategoryId = entity.completedMoveToCategoryId,
                        categories = categories,
                        isLoading = false
                    )
                    return@launch
                }
            }
            _state.value = RoutineEditState(
                isNew = true,
                categoryId = initialCategoryId,
                categories = categories,
                isLoading = false
            )
        }
    }

    fun updateName(name: String) { _state.value = _state.value.copy(name = name) }
    fun updateFrequency(f: RoutineFrequency) { _state.value = _state.value.copy(frequency = f) }
    fun updateHour(h: Int) { _state.value = _state.value.copy(hour = h) }
    fun updateMinute(m: Int) { _state.value = _state.value.copy(minute = m) }
    fun updateDayOfWeek(d: Int?) { _state.value = _state.value.copy(dayOfWeek = d) }
    fun updateDayOfMonth(d: Int?) { _state.value = _state.value.copy(dayOfMonth = d) }
    fun updateMonth(m: Int?) { _state.value = _state.value.copy(month = m) }
    fun updateDay(d: Int?) { _state.value = _state.value.copy(day = d) }
    fun updateVisibilityMode(m: RoutineVisibilityMode) { _state.value = _state.value.copy(visibilityMode = m) }
    fun updateIncompleteAction(a: RoutineTaskAction) { _state.value = _state.value.copy(incompleteAction = a) }
    fun updateIncompleteMoveTo(id: Long?) { _state.value = _state.value.copy(incompleteMoveToCategoryId = id) }
    fun updateCompletedAction(a: RoutineTaskAction) { _state.value = _state.value.copy(completedAction = a) }
    fun updateCompletedMoveTo(id: Long?) { _state.value = _state.value.copy(completedMoveToCategoryId = id) }

    fun save() {
        viewModelScope.launch {
            val s = _state.value
            val entity = RoutineEntity(
                id = if (s.isNew) 0 else s.id,
                categoryId = s.categoryId,
                name = s.name.takeIf { it.isNotBlank() },
                frequency = s.frequency,
                scheduleTimeHour = s.hour,
                scheduleTimeMinute = s.minute,
                scheduleDayOfWeek = s.dayOfWeek,
                scheduleDayOfMonth = s.dayOfMonth,
                scheduleMonth = s.month,
                scheduleDay = s.day,
                visibilityMode = s.visibilityMode,
                visibilityFrom = s.visibilityFrom,
                visibilityTo = s.visibilityTo,
                incompleteAction = s.incompleteAction,
                incompleteMoveToCategoryId = s.incompleteMoveToCategoryId,
                completedAction = s.completedAction,
                completedMoveToCategoryId = s.completedMoveToCategoryId
            )
            saveRoutineUseCase(entity)
            _state.value = s.copy(saved = true)
        }
    }

    fun delete() {
        viewModelScope.launch {
            if (!_state.value.isNew) deleteRoutineUseCase(_state.value.id)
            _state.value = _state.value.copy(saved = true)
        }
    }
}
