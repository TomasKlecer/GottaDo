package com.klecer.gottado.ui.screen.categorysettings

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klecer.gottado.calendar.CalendarSyncPrefs
import com.klecer.gottado.data.db.dao.CalendarSyncRuleDao
import com.klecer.gottado.data.db.entity.CalendarSyncRuleEntity
import com.klecer.gottado.data.db.entity.CategoryEntity
import com.klecer.gottado.data.db.entity.RoutineEntity
import com.klecer.gottado.domain.repository.WidgetCategoryRepository
import com.klecer.gottado.domain.usecase.DeleteRoutineUseCase
import com.klecer.gottado.domain.usecase.GetCategoriesUseCase
import com.klecer.gottado.domain.usecase.GetCategoryUseCase
import com.klecer.gottado.domain.usecase.GetRoutinesForCategoryUseCase
import com.klecer.gottado.domain.usecase.SaveCategoryUseCase
import com.klecer.gottado.notification.TaskNotificationScheduler
import com.klecer.gottado.ui.color.ColorPrefs
import com.klecer.gottado.widget.WidgetUpdateHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryTabViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    savedStateHandle: SavedStateHandle,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getCategoryUseCase: GetCategoryUseCase,
    private val saveCategoryUseCase: SaveCategoryUseCase,
    private val widgetCategoryRepository: WidgetCategoryRepository,
    private val getRoutinesForCategoryUseCase: GetRoutinesForCategoryUseCase,
    private val deleteRoutineUseCase: DeleteRoutineUseCase,
    private val calendarSyncRuleDao: CalendarSyncRuleDao,
    val syncPrefs: CalendarSyncPrefs,
    private val notificationScheduler: TaskNotificationScheduler,
    val colorPrefs: ColorPrefs
) : ViewModel() {

    private val initialCategoryId: Long? = savedStateHandle.get<String>("categoryId")?.toLongOrNull()

    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categories: StateFlow<List<CategoryEntity>> = _categories.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    private val _category = MutableStateFlow<CategoryEntity?>(null)
    val category: StateFlow<CategoryEntity?> = _category.asStateFlow()

    private val _routines = MutableStateFlow<List<RoutineEntity>>(emptyList())
    val routines: StateFlow<List<RoutineEntity>> = _routines.asStateFlow()

    private val _syncRules = MutableStateFlow<List<CalendarSyncRuleEntity>>(emptyList())
    val syncRules: StateFlow<List<CalendarSyncRuleEntity>> = _syncRules.asStateFlow()

    private var saveJob: Job? = null
    private var hasPendingChanges = false

    init {
        loadCategories()
    }

    override fun onCleared() {
        super.onCleared()
        if (hasPendingChanges) {
            saveJob?.cancel()
            val cat = _category.value ?: return
            runBlocking {
                saveCategoryUseCase(cat)
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _categories.value = getCategoriesUseCase()
            if (_selectedCategoryId.value == null) {
                val target = if (initialCategoryId != null && _categories.value.any { it.id == initialCategoryId }) {
                    initialCategoryId
                } else {
                    _categories.value.firstOrNull()?.id
                }
                target?.let { selectCategory(it) }
            }
        }
    }

    fun selectCategory(categoryId: Long) {
        _selectedCategoryId.value = categoryId
        viewModelScope.launch {
            _category.value = getCategoryUseCase(categoryId)
            _routines.value = getRoutinesForCategoryUseCase(categoryId)
            _syncRules.value = calendarSyncRuleDao.getRulesForCategory(categoryId)
        }
    }

    fun refreshRoutines() {
        val catId = _selectedCategoryId.value ?: return
        viewModelScope.launch {
            _routines.value = getRoutinesForCategoryUseCase(catId)
        }
    }

    fun deleteRoutine(routineId: Long) {
        viewModelScope.launch {
            deleteRoutineUseCase(routineId)
            val catId = _selectedCategoryId.value ?: return@launch
            _routines.value = getRoutinesForCategoryUseCase(catId)
        }
    }

    fun addCategory(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val maxOrder = _categories.value.maxOfOrNull { it.sortOrder } ?: -1
            val newId = saveCategoryUseCase(
                CategoryEntity(
                    id = 0L,
                    name = name.trim(),
                    sortOrder = maxOrder + 1,
                    showCheckboxInsteadOfBullet = false,
                    tasksWithTimeFirst = true,
                    categoryType = "normal",
                    color = 0xFFFFFFFF.toInt()
                )
            )
            _categories.value = getCategoriesUseCase()
            selectCategory(newId)
        }
    }

    private fun autoSave() {
        hasPendingChanges = true
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(500)
            val cat = _category.value ?: return@launch
            saveCategoryUseCase(cat)
            hasPendingChanges = false
            val catId = _selectedCategoryId.value ?: return@launch
            widgetCategoryRepository.getWidgetIdsForCategory(catId).forEach { presetId ->
                WidgetUpdateHelper.updateAllForPreset(appContext, presetId)
            }
            _categories.value = getCategoriesUseCase()
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

    fun updateAutoSortTimedEntries(value: Boolean) {
        _category.value = _category.value?.copy(autoSortTimedEntries = value)
        autoSave()
    }

    fun updateTimedEntriesAscending(value: Boolean) {
        _category.value = _category.value?.copy(timedEntriesAscending = value)
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

    fun updateSyncWithCalendarToday(value: Boolean) {
        _category.value = _category.value?.copy(syncWithCalendarToday = value)
        autoSave()
    }

    fun updateShowCalendarIcon(value: Boolean) {
        _category.value = _category.value?.copy(showCalendarIcon = value)
        autoSave()
    }

    fun updateShowDeleteButton(value: Boolean) {
        _category.value = _category.value?.copy(showDeleteButton = value)
        autoSave()
    }

    fun updateNotifyOnTime(value: Boolean) {
        _category.value = _category.value?.copy(notifyOnTime = value)
        autoSave()
        rescheduleNotifications()
    }

    fun updateNotifyMinutesBefore(value: Int) {
        _category.value = _category.value?.copy(notifyMinutesBefore = value)
        autoSave()
        rescheduleNotifications()
    }

    private fun rescheduleNotifications() {
        val catId = _selectedCategoryId.value ?: return
        viewModelScope.launch {
            notificationScheduler.rescheduleForCategory(catId)
        }
    }

    fun addSyncRule(ruleType: String) {
        val catId = _selectedCategoryId.value ?: return
        if (_syncRules.value.any { it.ruleType == ruleType }) return
        viewModelScope.launch {
            calendarSyncRuleDao.insert(CalendarSyncRuleEntity(categoryId = catId, ruleType = ruleType))
            _syncRules.value = calendarSyncRuleDao.getRulesForCategory(catId)
        }
    }

    fun removeSyncRule(ruleId: Long) {
        val catId = _selectedCategoryId.value ?: return
        viewModelScope.launch {
            calendarSyncRuleDao.deleteById(ruleId)
            _syncRules.value = calendarSyncRuleDao.getRulesForCategory(catId)
        }
    }
}
