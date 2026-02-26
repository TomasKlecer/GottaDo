package com.klecer.gottado.ui.screen.widgetsettings

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klecer.gottado.data.db.entity.CategoryEntity
import com.klecer.gottado.data.db.entity.ReorderHandlePosition
import com.klecer.gottado.data.db.entity.WidgetCategoryJoinEntity
import com.klecer.gottado.data.db.entity.WidgetConfigEntity
import com.klecer.gottado.domain.repository.WidgetCategoryRepository
import com.klecer.gottado.domain.usecase.GetAllWidgetConfigsUseCase
import com.klecer.gottado.domain.usecase.GetCategoriesUseCase
import com.klecer.gottado.domain.usecase.GetWidgetConfigUseCase
import com.klecer.gottado.domain.usecase.SaveWidgetConfigUseCase
import com.klecer.gottado.widget.WidgetUpdateHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class WidgetTabViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    private val getAllWidgetConfigsUseCase: GetAllWidgetConfigsUseCase,
    private val getWidgetConfigUseCase: GetWidgetConfigUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val saveWidgetConfigUseCase: SaveWidgetConfigUseCase,
    private val widgetCategoryRepository: WidgetCategoryRepository
) : ViewModel() {

    private val initialWidgetId: Int? = savedStateHandle.get<String>("widgetId")?.toIntOrNull()

    private val _widgets = MutableStateFlow<List<WidgetConfigEntity>>(emptyList())
    val widgets: StateFlow<List<WidgetConfigEntity>> = _widgets.asStateFlow()

    private val _selectedWidgetId = MutableStateFlow<Int?>(null)
    val selectedWidgetId: StateFlow<Int?> = _selectedWidgetId.asStateFlow()

    private val _config = MutableStateFlow<WidgetConfigEntity?>(null)
    val config: StateFlow<WidgetConfigEntity?> = _config.asStateFlow()

    private val _categoryJoins = MutableStateFlow<List<CategoryJoinItem>>(emptyList())
    val categoryJoins: StateFlow<List<CategoryJoinItem>> = _categoryJoins.asStateFlow()

    private val _allCategories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val allCategories: StateFlow<List<CategoryEntity>> = _allCategories.asStateFlow()

    private var saveJob: Job? = null
    private var hasPendingChanges = false

    init {
        loadWidgets()
    }

    override fun onCleared() {
        super.onCleared()
        if (hasPendingChanges) {
            saveJob?.cancel()
            val c = _config.value ?: return
            runBlocking { saveWidgetConfigUseCase(c) }
        }
    }

    private fun loadWidgets() {
        viewModelScope.launch {
            _widgets.value = getAllWidgetConfigsUseCase()
            if (_selectedWidgetId.value == null) {
                val target = if (initialWidgetId != null && _widgets.value.any { it.widgetId == initialWidgetId }) {
                    initialWidgetId
                } else {
                    _widgets.value.firstOrNull()?.widgetId
                }
                target?.let { selectWidget(it) }
            }
        }
    }

    fun selectWidget(widgetId: Int) {
        _selectedWidgetId.value = widgetId
        viewModelScope.launch {
            val config = getWidgetConfigUseCase(widgetId) ?: return@launch
            val allCats = getCategoriesUseCase()
            _config.value = config
            val joins = widgetCategoryRepository.getJoinsForWidget(widgetId)
            val items = joins.map { join ->
                val cat = allCats.find { it.id == join.categoryId }
                CategoryJoinItem(join, cat?.name ?: "")
            }
            _categoryJoins.value = items
            _allCategories.value = allCats
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _widgets.value = getAllWidgetConfigsUseCase()
            val wId = _selectedWidgetId.value ?: return@launch
            selectWidget(wId)
        }
    }

    private fun autoSave() {
        hasPendingChanges = true
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(500)
            val wId = _selectedWidgetId.value ?: return@launch
            val c = _config.value ?: return@launch
            saveWidgetConfigUseCase(c)
            hasPendingChanges = false
            WidgetUpdateHelper.update(appContext, wId)
            _widgets.value = getAllWidgetConfigsUseCase()
        }
    }

    fun updateTitle(title: String?) {
        _config.value = _config.value?.copy(title = title?.takeIf { it.isNotBlank() })
        autoSave()
    }

    fun updateSubtitle(subtitle: String?) {
        _config.value = _config.value?.copy(subtitle = subtitle?.takeIf { it.isNotBlank() })
        autoSave()
    }

    fun updateNote(note: String?) {
        _config.value = _config.value?.copy(note = note?.takeIf { it.isNotBlank() })
        autoSave()
    }

    fun updateBackgroundColor(color: Int) {
        _config.value = _config.value?.copy(backgroundColor = color)
        autoSave()
    }

    fun updateBackgroundAlpha(alpha: Float) {
        _config.value = _config.value?.copy(backgroundAlpha = alpha.coerceIn(0f, 1f))
        autoSave()
    }

    fun updateCategoryFontSizeSp(sp: Float) {
        _config.value = _config.value?.copy(categoryFontSizeSp = sp.coerceIn(8f, 32f))
        autoSave()
    }

    fun updateRecordFontSizeSp(sp: Float) {
        _config.value = _config.value?.copy(recordFontSizeSp = sp.coerceIn(8f, 32f))
        autoSave()
    }

    fun updateDefaultTextColor(color: Int) {
        _config.value = _config.value?.copy(defaultTextColor = color)
        autoSave()
    }

    fun updateTitleColor(color: Int) {
        _config.value = _config.value?.copy(titleColor = color)
        autoSave()
    }

    fun updateSubtitleColor(color: Int) {
        _config.value = _config.value?.copy(subtitleColor = color)
        autoSave()
    }

    fun updateBulletSizeDp(size: Int) {
        _config.value = _config.value?.copy(bulletSizeDp = size.coerceIn(4, 24))
        autoSave()
    }

    fun updateCheckboxSizeDp(size: Int) {
        _config.value = _config.value?.copy(checkboxSizeDp = size.coerceIn(4, 24))
        autoSave()
    }

    fun updateShowTitleOnWidget(show: Boolean) {
        _config.value = _config.value?.copy(showTitleOnWidget = show)
        autoSave()
    }

    fun updateReorderHandlePosition(position: ReorderHandlePosition) {
        _config.value = _config.value?.copy(reorderHandlePosition = position)
        autoSave()
    }

    fun setCategoryVisible(categoryId: Long, visible: Boolean) {
        val wId = _selectedWidgetId.value ?: return
        viewModelScope.launch {
            val joins = _categoryJoins.value
            val item = joins.find { it.join.categoryId == categoryId } ?: return@launch
            widgetCategoryRepository.updateJoin(wId, categoryId, item.join.sortOrder, visible)
            WidgetUpdateHelper.update(appContext, wId)
            selectWidget(wId)
        }
    }

    fun moveCategoryUp(categoryId: Long) {
        val wId = _selectedWidgetId.value ?: return
        viewModelScope.launch {
            val joins = _categoryJoins.value
            val idx = joins.indexOfFirst { it.join.categoryId == categoryId }.takeIf { it > 0 } ?: return@launch
            reorderJoins(joins, idx, idx - 1)
        }
    }

    fun moveCategoryDown(categoryId: Long) {
        val wId = _selectedWidgetId.value ?: return
        viewModelScope.launch {
            val joins = _categoryJoins.value
            val idx = joins.indexOfFirst { it.join.categoryId == categoryId }
            if (idx < 0 || idx >= joins.size - 1) return@launch
            reorderJoins(joins, idx, idx + 1)
        }
    }

    private suspend fun reorderJoins(joins: List<CategoryJoinItem>, from: Int, to: Int) {
        val wId = _selectedWidgetId.value ?: return
        val reordered = joins.toMutableList().apply { add(to, removeAt(from)) }
        reordered.forEachIndexed { index, item ->
            widgetCategoryRepository.updateJoin(wId, item.join.categoryId, index, item.join.visible)
        }
        WidgetUpdateHelper.update(appContext, wId)
        selectWidget(wId)
    }

    fun removeCategoryFromWidget(categoryId: Long) {
        val wId = _selectedWidgetId.value ?: return
        viewModelScope.launch {
            widgetCategoryRepository.removeCategoryFromWidget(wId, categoryId)
            WidgetUpdateHelper.update(appContext, wId)
            selectWidget(wId)
        }
    }

    fun addCategoryToWidget(categoryId: Long) {
        val wId = _selectedWidgetId.value ?: return
        viewModelScope.launch {
            val nextOrder = _categoryJoins.value.size
            widgetCategoryRepository.addCategoryToWidget(wId, categoryId, nextOrder, true)
            WidgetUpdateHelper.update(appContext, wId)
            selectWidget(wId)
        }
    }
}
