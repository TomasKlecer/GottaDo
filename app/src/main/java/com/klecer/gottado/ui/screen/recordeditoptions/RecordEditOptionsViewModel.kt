package com.klecer.gottado.ui.screen.recordeditoptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klecer.gottado.domain.model.RecordEditOptions
import com.klecer.gottado.domain.usecase.GetRecordEditOptionsUseCase
import com.klecer.gottado.domain.usecase.SaveRecordEditOptionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecordEditOptionsViewModel @Inject constructor(
    private val getRecordEditOptionsUseCase: GetRecordEditOptionsUseCase,
    private val saveRecordEditOptionsUseCase: SaveRecordEditOptionsUseCase
) : ViewModel() {

    private val _options = MutableStateFlow(RecordEditOptions())
    val options: StateFlow<RecordEditOptions> = _options.asStateFlow()

    private var saveJob: Job? = null

    init {
        _options.value = getRecordEditOptionsUseCase()
    }

    private fun autoSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(300)
            saveRecordEditOptionsUseCase(_options.value)
        }
    }

    fun updateShowRichText(value: Boolean) {
        _options.value = _options.value.copy(showRichText = value)
        autoSave()
    }

    fun updateShowTimeField(value: Boolean) {
        _options.value = _options.value.copy(showTimeField = value)
        autoSave()
    }

    fun updateShowCategoryDropdown(value: Boolean) {
        _options.value = _options.value.copy(showCategoryDropdown = value)
        autoSave()
    }

    fun updateShowBulletColor(value: Boolean) {
        _options.value = _options.value.copy(showBulletColor = value)
        autoSave()
    }

    fun updateUseUnifiedColorPicker(value: Boolean) {
        _options.value = _options.value.copy(useUnifiedColorPicker = value)
        autoSave()
    }
}
