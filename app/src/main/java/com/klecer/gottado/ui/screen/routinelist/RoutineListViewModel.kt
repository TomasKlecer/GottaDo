package com.klecer.gottado.ui.screen.routinelist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klecer.gottado.data.db.entity.RoutineEntity
import com.klecer.gottado.domain.usecase.DeleteRoutineUseCase
import com.klecer.gottado.domain.usecase.GetRoutinesForCategoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoutineListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getRoutinesForCategoryUseCase: GetRoutinesForCategoryUseCase,
    private val deleteRoutineUseCase: DeleteRoutineUseCase
) : ViewModel() {

    val categoryId: Long = savedStateHandle.get<String>("categoryId")?.toLongOrNull() ?: -1L

    private val _routines = MutableStateFlow<List<RoutineEntity>>(emptyList())
    val routines: StateFlow<List<RoutineEntity>> = _routines.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _routines.value = getRoutinesForCategoryUseCase(categoryId)
        }
    }

    fun delete(routineId: Long) {
        viewModelScope.launch {
            deleteRoutineUseCase(routineId)
            load()
        }
    }
}
