package com.klecer.gottado.ui.screen.recordeditoptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klecer.gottado.domain.model.RecordEditOptions
import com.klecer.gottado.domain.usecase.GetRecordEditOptionsUseCase
import com.klecer.gottado.domain.usecase.SaveRecordEditOptionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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

    init {
        _options.value = getRecordEditOptionsUseCase()
    }

    private fun save() {
        viewModelScope.launch {
            saveRecordEditOptionsUseCase(_options.value)
        }
    }

    fun updateShowRichText(value: Boolean) {
        _options.value = _options.value.copy(showRichText = value)
        save()
    }

    fun updateShowTimeField(value: Boolean) {
        _options.value = _options.value.copy(showTimeField = value)
        save()
    }

    fun updateShowCategoryDropdown(value: Boolean) {
        _options.value = _options.value.copy(showCategoryDropdown = value)
        save()
    }

    fun updateShowBulletColor(value: Boolean) {
        _options.value = _options.value.copy(showBulletColor = value)
        save()
    }

    fun updateShowTextColor(value: Boolean) {
        _options.value = _options.value.copy(showTextColor = value)
        save()
    }

    fun updateUseUnifiedColorPicker(value: Boolean) {
        _options.value = _options.value.copy(useUnifiedColorPicker = value)
        save()
    }

    fun updateShowCompletedCheckbox(value: Boolean) {
        _options.value = _options.value.copy(showCompletedCheckbox = value)
        save()
    }
}
