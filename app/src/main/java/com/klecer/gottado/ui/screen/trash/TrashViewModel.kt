package com.klecer.gottado.ui.screen.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klecer.gottado.domain.model.TrashDay
import com.klecer.gottado.domain.usecase.ClearTrashUseCase
import com.klecer.gottado.domain.usecase.DeleteFromTrashUseCase
import com.klecer.gottado.domain.usecase.GetTrashEntriesByDayUseCase
import com.klecer.gottado.domain.usecase.RestoreFromTrashUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val getTrashEntriesByDayUseCase: GetTrashEntriesByDayUseCase,
    private val restoreFromTrashUseCase: RestoreFromTrashUseCase,
    private val deleteFromTrashUseCase: DeleteFromTrashUseCase,
    private val clearTrashUseCase: ClearTrashUseCase
) : ViewModel() {

    private val _days = MutableStateFlow<List<TrashDay>>(emptyList())
    val days: StateFlow<List<TrashDay>> = _days.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _days.value = getTrashEntriesByDayUseCase()
        }
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

    fun emptyTrash() {
        viewModelScope.launch {
            clearTrashUseCase()
            load()
        }
    }
}
