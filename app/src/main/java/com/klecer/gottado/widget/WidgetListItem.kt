package com.klecer.gottado.widget

import com.klecer.gottado.domain.model.CategoryBlock
import com.klecer.gottado.domain.model.TaskItem

/**
 * Flattened row types for the widget list (category, record, or footer).
 */
sealed class WidgetListItem {
    data class CategoryRow(val block: CategoryBlock) : WidgetListItem()
    data class RecordRow(val task: TaskItem, val showCheckbox: Boolean, val showCalendarIcon: Boolean = false) : WidgetListItem()
    object Footer : WidgetListItem()
    data class UndoDelete(val trashId: Long) : WidgetListItem()
    data class HintText(val message: String) : WidgetListItem()
}
