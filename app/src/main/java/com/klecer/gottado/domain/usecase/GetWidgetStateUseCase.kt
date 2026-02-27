package com.klecer.gottado.domain.usecase

import com.klecer.gottado.data.db.entity.WidgetConfigEntity
import com.klecer.gottado.domain.model.CategoryBlock
import com.klecer.gottado.domain.model.TaskItem
import com.klecer.gottado.domain.model.WidgetState
import com.klecer.gottado.domain.repository.CategoryRepository
import com.klecer.gottado.domain.repository.TaskRepository
import com.klecer.gottado.domain.repository.WidgetCategoryRepository
import com.klecer.gottado.domain.repository.WidgetConfigRepository
import javax.inject.Inject

/**
 * Returns all data needed to render one widget: config + ordered category blocks with tasks.
 * Ordering: by widget's category order/visibility; within category by tasksWithTimeFirst then time/sortOrder.
 */
class GetWidgetStateUseCase @Inject constructor(
    private val widgetConfigRepository: WidgetConfigRepository,
    private val widgetCategoryRepository: WidgetCategoryRepository,
    private val categoryRepository: CategoryRepository,
    private val taskRepository: TaskRepository
) {
    suspend operator fun invoke(widgetId: Int): WidgetState? {
        val config = widgetConfigRepository.getByWidgetId(widgetId) ?: return null
        val joins = widgetCategoryRepository.getJoinsForWidget(widgetId)
            .filter { it.visible }
            .sortedBy { it.sortOrder }
        val categoryBlocks = joins.mapNotNull { join ->
            val category = categoryRepository.getById(join.categoryId) ?: return@mapNotNull null
            var tasks = taskRepository.getByCategory(category.id)
            tasks = applySorting(tasks, category)
            CategoryBlock(
                categoryId = category.id,
                name = category.name,
                showCheckboxInsteadOfBullet = category.showCheckboxInsteadOfBullet,
                tasksWithTimeFirst = category.tasksWithTimeFirst,
                color = category.color,
                showCalendarIcon = category.showCalendarIcon,
                showDeleteButton = category.showDeleteButton,
                tasks = tasks.map { t ->
                    TaskItem(
                        id = t.id,
                        contentHtml = t.contentHtml,
                        completed = t.completed,
                        bulletColor = t.bulletColor,
                        textColor = t.textColor,
                        scheduledTimeMillis = t.scheduledTimeMillis,
                        sortOrder = t.sortOrder,
                        fromCalendarSync = t.fromCalendarSync
                    )
                }
            )
        }
        return WidgetState(config = config, categoryBlocks = categoryBlocks)
    }

    private fun applySorting(
        tasks: List<com.klecer.gottado.data.db.entity.TaskEntity>,
        category: com.klecer.gottado.data.db.entity.CategoryEntity
    ): List<com.klecer.gottado.data.db.entity.TaskEntity> {
        if (!category.autoSortTimedEntries) return tasks

        val withTime = tasks.filter { it.scheduledTimeMillis != null }.let { list ->
            if (category.timedEntriesAscending) list.sortedBy { it.scheduledTimeMillis }
            else list.sortedByDescending { it.scheduledTimeMillis }
        }
        val withoutTime = tasks.filter { it.scheduledTimeMillis == null }
        return if (category.tasksWithTimeFirst) withTime + withoutTime else withoutTime + withTime
    }
}
