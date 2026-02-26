package com.klecer.gottado.domain.usecase

import com.klecer.gottado.data.db.dao.CalendarDismissedDao
import com.klecer.gottado.data.db.entity.CalendarDismissedEntity
import com.klecer.gottado.data.db.entity.RoutineEntity
import com.klecer.gottado.data.db.entity.RoutineFrequency
import com.klecer.gottado.data.db.entity.RoutineTaskAction
import com.klecer.gottado.data.db.entity.TrashEntryEntity
import com.klecer.gottado.domain.repository.CategoryRepository
import com.klecer.gottado.domain.repository.RoutineRepository
import com.klecer.gottado.domain.repository.TaskRepository
import com.klecer.gottado.domain.repository.TrashRepository
import com.klecer.gottado.domain.repository.WidgetCategoryRepository
import java.util.Calendar
import javax.inject.Inject

/**
 * Finds all routines that are due right now and applies their business rules to tasks.
 * Returns the set of widgetIds whose display was affected so the caller can refresh them.
 *
 * Extension point: Calendar sync can reuse the same "apply action to tasks" helpers.
 */
class ExecuteRoutinesDueUseCase @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val taskRepository: TaskRepository,
    private val trashRepository: TrashRepository,
    private val categoryRepository: CategoryRepository,
    private val widgetCategoryRepository: WidgetCategoryRepository,
    private val calendarDismissedDao: CalendarDismissedDao
) {
    /**
     * @return set of widgetIds that need visual refresh
     */
    suspend operator fun invoke(): Set<Int> {
        val now = Calendar.getInstance()
        val allRoutines = routineRepository.getAll()
        val affectedWidgetIds = mutableSetOf<Int>()

        for (routine in allRoutines) {
            if (!isDue(routine, now)) continue
            applyRoutine(routine)
            affectedWidgetIds += widgetCategoryRepository.getWidgetIdsForCategory(routine.categoryId)
        }
        return affectedWidgetIds
    }

    /**
     * Check whether [routine] is due in the current period.
     * Uses a simple "does the schedule match the current calendar fields?" approach.
     * WorkManager runs at ~15min intervals, so we check hour+minute within a 15-min window.
     */
    private fun isDue(routine: RoutineEntity, now: Calendar): Boolean {
        val hourNow = now.get(Calendar.HOUR_OF_DAY)
        val minuteNow = now.get(Calendar.MINUTE)
        val minuteOfDayNow = hourNow * 60 + minuteNow
        val minuteOfDaySchedule = routine.scheduleTimeHour * 60 + routine.scheduleTimeMinute
        val withinWindow = minuteOfDayNow in minuteOfDaySchedule..(minuteOfDaySchedule + 29)
        if (!withinWindow) return false

        return when (routine.frequency) {
            RoutineFrequency.DAILY -> true
            RoutineFrequency.WEEKLY -> {
                routine.scheduleDayOfWeek == now.get(Calendar.DAY_OF_WEEK)
            }
            RoutineFrequency.MONTHLY -> {
                routine.scheduleDayOfMonth == now.get(Calendar.DAY_OF_MONTH)
            }
            RoutineFrequency.YEARLY -> {
                routine.scheduleMonth == now.get(Calendar.MONTH) &&
                    routine.scheduleDay == now.get(Calendar.DAY_OF_MONTH)
            }
        }
    }

    private suspend fun applyRoutine(routine: RoutineEntity) {
        val tasks = taskRepository.getByCategory(routine.categoryId)
        val incompleteTasks = tasks.filter { !it.completed }
        val completedTasks = tasks.filter { it.completed }
        val now = System.currentTimeMillis()
        val categoryName = categoryRepository.getById(routine.categoryId)?.name ?: ""

        for (task in incompleteTasks) {
            applyAction(routine.incompleteAction, routine.incompleteMoveToCategoryId, task.id, task.categoryId, categoryName, now)
        }
        for (task in completedTasks) {
            applyAction(routine.completedAction, routine.completedMoveToCategoryId, task.id, task.categoryId, categoryName, now)
        }
    }

    private suspend fun applyAction(
        action: RoutineTaskAction,
        moveToCategoryId: Long?,
        taskId: Long,
        currentCategoryId: Long,
        categoryName: String,
        now: Long
    ) {
        val task = taskRepository.getById(taskId) ?: return
        when (action) {
            RoutineTaskAction.DELETE -> {
                if (task.fromCalendarSync) {
                    calendarDismissedDao.insert(
                        CalendarDismissedEntity(
                            categoryId = currentCategoryId,
                            eventTitle = task.contentHtml.trim()
                        )
                    )
                }
                trashRepository.insert(
                    TrashEntryEntity(
                        originalCategoryId = currentCategoryId,
                        contentHtml = task.contentHtml,
                        completed = task.completed,
                        bulletColor = task.bulletColor,
                        scheduledTimeMillis = task.scheduledTimeMillis,
                        sortOrder = task.sortOrder,
                        deletedAtMillis = now,
                        categoryName = categoryName
                    )
                )
                taskRepository.deleteById(taskId)
            }
            RoutineTaskAction.COMPLETE -> {
                taskRepository.updateCompleted(taskId, true, now)
                moveIfNeeded(taskId, moveToCategoryId, now)
            }
            RoutineTaskAction.UNCOMPLETE -> {
                taskRepository.updateCompleted(taskId, false, now)
                moveIfNeeded(taskId, moveToCategoryId, now)
            }
            RoutineTaskAction.MOVE -> {
                moveIfNeeded(taskId, moveToCategoryId, now)
            }
        }
    }

    private suspend fun moveIfNeeded(taskId: Long, targetCategoryId: Long?, now: Long) {
        if (targetCategoryId == null || targetCategoryId <= 0L) return
        val task = taskRepository.getById(taskId) ?: return
        if (task.categoryId == targetCategoryId) return
        val maxOrder = taskRepository.getByCategory(targetCategoryId).maxOfOrNull { it.sortOrder } ?: -1
        taskRepository.updateCategoryAndOrder(taskId, targetCategoryId, maxOrder + 1, now)
    }
}
