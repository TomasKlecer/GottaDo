package com.klecer.gottado.domain.model

import com.klecer.gottado.data.db.entity.WidgetConfigEntity

data class WidgetState(
    val config: WidgetConfigEntity,
    val categoryBlocks: List<CategoryBlock>
)

data class CategoryBlock(
    val categoryId: Long,
    val name: String,
    val showCheckboxInsteadOfBullet: Boolean,
    val tasksWithTimeFirst: Boolean,
    val tasks: List<TaskItem>,
    val color: Int = 0,
    val showCalendarIcon: Boolean = true
)

data class TaskItem(
    val id: Long,
    val contentHtml: String,
    val completed: Boolean,
    val bulletColor: Int,
    val textColor: Int = 0,
    val scheduledTimeMillis: Long?,
    val sortOrder: Int = 0,
    val fromCalendarSync: Boolean = false
)
