package com.klecer.gottado.ui.screen.widgetlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klecer.gottado.data.db.entity.WidgetConfigEntity
import com.klecer.gottado.domain.usecase.GetAllWidgetConfigsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WidgetListViewModel @Inject constructor(
    private val getAllWidgetConfigsUseCase: GetAllWidgetConfigsUseCase
) : ViewModel() {

    private val _widgets = MutableStateFlow<List<WidgetConfigEntity>>(emptyList())
    val widgets: StateFlow<List<WidgetConfigEntity>> = _widgets.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _widgets.value = getAllWidgetConfigsUseCase()
        }
    }
}
