package com.klecer.gottado.ui.screen.categorysettings

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klecer.gottado.data.db.entity.CategoryEntity
import com.klecer.gottado.domain.repository.WidgetCategoryRepository
import com.klecer.gottado.domain.usecase.GetCategoryUseCase
import com.klecer.gottado.domain.usecase.SaveCategoryUseCase
import com.klecer.gottado.widget.WidgetUpdateHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategorySettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    private val getCategoryUseCase: GetCategoryUseCase,
    private val saveCategoryUseCase: SaveCategoryUseCase,
    private val widgetCategoryRepository: WidgetCategoryRepository
) : ViewModel() {

    private val categoryId: Long = savedStateHandle.get<String>("categoryId")?.toLongOrNull() ?: -1L

    private val _category = MutableStateFlow<CategoryEntity?>(null)
    val category: StateFlow<CategoryEntity?> = _category.asStateFlow()

    private var saveJob: Job? = null

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _category.value = getCategoryUseCase(categoryId)
        }
    }

    private fun autoSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(500)
            val cat = _category.value ?: return@launch
            saveCategoryUseCase(cat)
            widgetCategoryRepository.getWidgetIdsForCategory(categoryId).forEach { presetId ->
                WidgetUpdateHelper.updateAllForPreset(appContext, presetId)
            }
        }
    }

    fun updateName(name: String) {
        _category.value = _category.value?.copy(name = name)
        autoSave()
    }

    fun updateShowCheckboxInsteadOfBullet(value: Boolean) {
        _category.value = _category.value?.copy(showCheckboxInsteadOfBullet = value)
        autoSave()
    }

    fun updateTasksWithTimeFirst(value: Boolean) {
        _category.value = _category.value?.copy(tasksWithTimeFirst = value)
        autoSave()
    }

    fun updateCategoryType(type: String) {
        _category.value = _category.value?.copy(categoryType = type)
        autoSave()
    }

    fun updateColor(color: Int) {
        _category.value = _category.value?.copy(color = color)
        autoSave()
    }
}
