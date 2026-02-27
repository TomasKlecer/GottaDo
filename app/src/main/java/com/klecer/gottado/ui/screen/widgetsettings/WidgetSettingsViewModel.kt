package com.klecer.gottado.ui.screen.widgetsettings

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klecer.gottado.data.db.entity.ReorderHandlePosition
import com.klecer.gottado.data.db.entity.WidgetCategoryJoinEntity
import com.klecer.gottado.data.db.entity.WidgetConfigEntity
import com.klecer.gottado.data.db.entity.CategoryEntity
import com.klecer.gottado.domain.repository.WidgetCategoryRepository
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
import javax.inject.Inject

data class CategoryJoinItem(
    val join: WidgetCategoryJoinEntity,
    val categoryName: String
)

@HiltViewModel
class WidgetSettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    private val getWidgetConfigUseCase: GetWidgetConfigUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val saveWidgetConfigUseCase: SaveWidgetConfigUseCase,
    private val widgetCategoryRepository: WidgetCategoryRepository
) : ViewModel() {

    private val widgetId: Int = savedStateHandle.get<String>("widgetId")?.toIntOrNull() ?: -1

    private val _config = MutableStateFlow<WidgetConfigEntity?>(null)
    val config: StateFlow<WidgetConfigEntity?> = _config.asStateFlow()

    private val _categoryJoins = MutableStateFlow<List<CategoryJoinItem>>(emptyList())
    val categoryJoins: StateFlow<List<CategoryJoinItem>> = _categoryJoins.asStateFlow()

    private val _allCategories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val allCategories: StateFlow<List<CategoryEntity>> = _allCategories.asStateFlow()

    private var saveJob: Job? = null

    init {
        load()
    }

    fun load() {
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

    private fun autoSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(500)
            val c = _config.value ?: return@launch
            saveWidgetConfigUseCase(c)
            WidgetUpdateHelper.updateAllForPreset(appContext, widgetId)
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
        viewModelScope.launch {
            val joins = _categoryJoins.value
            val item = joins.find { it.join.categoryId == categoryId } ?: return@launch
            widgetCategoryRepository.updateJoin(widgetId, categoryId, item.join.sortOrder, visible)
            WidgetUpdateHelper.updateAllForPreset(appContext, widgetId)
            load()
        }
    }

    fun moveCategoryUp(categoryId: Long) {
        viewModelScope.launch {
            val joins = _categoryJoins.value
            val idx = joins.indexOfFirst { it.join.categoryId == categoryId }.takeIf { it > 0 } ?: return@launch
            reorderJoins(joins, idx, idx - 1)
        }
    }

    fun moveCategoryDown(categoryId: Long) {
        viewModelScope.launch {
            val joins = _categoryJoins.value
            val idx = joins.indexOfFirst { it.join.categoryId == categoryId }
            if (idx < 0 || idx >= joins.size - 1) return@launch
            reorderJoins(joins, idx, idx + 1)
        }
    }

    private suspend fun reorderJoins(joins: List<CategoryJoinItem>, from: Int, to: Int) {
        val reordered = joins.toMutableList().apply { add(to, removeAt(from)) }
        reordered.forEachIndexed { index, item ->
            widgetCategoryRepository.updateJoin(widgetId, item.join.categoryId, index, item.join.visible)
        }
        WidgetUpdateHelper.updateAllForPreset(appContext, widgetId)
        load()
    }

    fun removeCategoryFromWidget(categoryId: Long) {
        viewModelScope.launch {
            widgetCategoryRepository.removeCategoryFromWidget(widgetId, categoryId)
            WidgetUpdateHelper.updateAllForPreset(appContext, widgetId)
            load()
        }
    }

    fun addCategoryToWidget(categoryId: Long) {
        viewModelScope.launch {
            val nextOrder = _categoryJoins.value.size
            widgetCategoryRepository.addCategoryToWidget(widgetId, categoryId, nextOrder, true)
            WidgetUpdateHelper.updateAllForPreset(appContext, widgetId)
            load()
        }
    }
}
