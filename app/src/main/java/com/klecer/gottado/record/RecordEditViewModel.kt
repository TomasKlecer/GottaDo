package com.klecer.gottado.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klecer.gottado.data.db.entity.CategoryEntity
import com.klecer.gottado.data.db.entity.TaskEntity
import com.klecer.gottado.domain.repository.TaskRepository
import com.klecer.gottado.domain.usecase.DeleteTaskUseCase
import com.klecer.gottado.domain.usecase.GetCategoriesUseCase
import com.klecer.gottado.domain.usecase.GetCategoryUseCase
import com.klecer.gottado.domain.usecase.GetTaskUseCase
import com.klecer.gottado.domain.usecase.MoveTaskToCategoryUseCase
import com.klecer.gottado.domain.usecase.SaveTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecordEditState(
    val contentHtml: String = "",
    val completed: Boolean = false,
    val bulletColor: Int = 0xFF000000.toInt(),
    val textColor: Int = 0,
    val scheduledTimeMillis: Long? = null,
    val categoryId: Long = 0L,
    val categories: List<CategoryEntity> = emptyList(),
    val isAddMode: Boolean = true,
    val taskId: Long? = null,
    val widgetId: Int = -1,
    val isLoading: Boolean = true,
    val finishWithRefresh: Boolean = false,
    val deletedTrashIdForUndo: Long? = null
)

@HiltViewModel
class RecordEditViewModel @Inject constructor(
    private val getTaskUseCase: GetTaskUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getCategoryUseCase: GetCategoryUseCase,
    private val saveTaskUseCase: SaveTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val moveTaskToCategoryUseCase: MoveTaskToCategoryUseCase,
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RecordEditState())
    val state: StateFlow<RecordEditState> = _state.asStateFlow()

    fun init(taskId: Long?, categoryId: Long, widgetId: Int, defaultBulletColor: Int = 0xFFFFFFFF.toInt()) {
        viewModelScope.launch {
            val categories = getCategoriesUseCase()
            val isAddMode = taskId == null
            if (taskId != null) {
                val task = getTaskUseCase(taskId)
                if (task != null) {
                    _state.value = _state.value.copy(
                        contentHtml = task.contentHtml,
                        completed = task.completed,
                        bulletColor = task.bulletColor,
                        textColor = task.textColor,
                        scheduledTimeMillis = task.scheduledTimeMillis,
                        categoryId = task.categoryId,
                        categories = categories,
                        isAddMode = false,
                        taskId = taskId,
                        widgetId = widgetId,
                        isLoading = false
                    )
                    return@launch
                }
            }
            val cat = if (categoryId != 0L) getCategoryUseCase(categoryId) else null
            val bulletCol = if (cat != null && cat.defaultBulletColor != 0) cat.defaultBulletColor else defaultBulletColor
            val textCol = if (cat != null && cat.defaultTextColor != 0) cat.defaultTextColor else 0
            _state.value = _state.value.copy(
                bulletColor = bulletCol,
                textColor = textCol,
                categoryId = categoryId,
                categories = categories,
                isAddMode = isAddMode,
                taskId = taskId,
                widgetId = widgetId,
                isLoading = false
            )
        }
    }

    fun updateContent(html: String) {
        _state.value = _state.value.copy(contentHtml = html)
    }

    fun updateCompleted(completed: Boolean) {
        _state.value = _state.value.copy(completed = completed)
    }

    fun updateBulletColor(color: Int) {
        _state.value = _state.value.copy(bulletColor = color)
    }

    fun updateTextColor(color: Int) {
        _state.value = _state.value.copy(textColor = color)
    }

    fun updateScheduledTime(millis: Long?) {
        _state.value = _state.value.copy(scheduledTimeMillis = millis)
    }

    fun updateCategoryId(categoryId: Long) {
        _state.value = _state.value.copy(categoryId = categoryId)
    }

    fun save() {
        viewModelScope.launch {
            val s = _state.value
            val now = System.currentTimeMillis()
            if (s.taskId != null) {
                val task = getTaskUseCase(s.taskId) ?: return@launch
                val sameCategory = task.categoryId == s.categoryId
                val entity = TaskEntity(
                    id = task.id,
                    categoryId = s.categoryId,
                    contentHtml = s.contentHtml,
                    completed = s.completed,
                    bulletColor = s.bulletColor,
                    textColor = s.textColor,
                    scheduledTimeMillis = s.scheduledTimeMillis,
                    sortOrder = if (sameCategory) task.sortOrder else nextSortOrderInCategory(s.categoryId),
                    createdAtMillis = task.createdAtMillis,
                    updatedAtMillis = now
                )
                saveTaskUseCase(entity)
                if (!sameCategory) {
                    moveTaskToCategoryUseCase(task.id, s.categoryId, entity.sortOrder)
                }
            } else {
                val sortOrder = nextSortOrderInCategory(s.categoryId)
                val entity = TaskEntity(
                    id = 0L,
                    categoryId = s.categoryId,
                    contentHtml = s.contentHtml,
                    completed = s.completed,
                    bulletColor = s.bulletColor,
                    textColor = s.textColor,
                    scheduledTimeMillis = s.scheduledTimeMillis,
                    sortOrder = sortOrder,
                    createdAtMillis = now,
                    updatedAtMillis = now
                )
                saveTaskUseCase(entity)
            }
            _state.value = s.copy(finishWithRefresh = true)
        }
    }

    private suspend fun nextSortOrderInCategory(categoryId: Long): Int {
        val tasks = taskRepository.getByCategory(categoryId)
        return if (tasks.isEmpty()) 0 else (tasks.maxOfOrNull { it.sortOrder } ?: -1) + 1
    }

    fun delete() {
        viewModelScope.launch {
            val s = _state.value
            val tid = s.taskId ?: return@launch
            val trashId = deleteTaskUseCase(tid)
            _state.value = s.copy(finishWithRefresh = true, deletedTrashIdForUndo = trashId)
        }
    }

    fun clearFinishAndUndoFlag() {
        _state.value = _state.value.copy(finishWithRefresh = false, deletedTrashIdForUndo = null)
    }
}
