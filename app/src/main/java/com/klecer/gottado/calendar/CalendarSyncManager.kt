package com.klecer.gottado.calendar

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.klecer.gottado.data.db.dao.CalendarDismissedDao
import com.klecer.gottado.data.db.dao.CalendarSyncRuleDao
import com.klecer.gottado.data.db.entity.CalendarSyncRuleType
import com.klecer.gottado.data.db.entity.TaskEntity
import com.klecer.gottado.domain.repository.CategoryRepository
import com.klecer.gottado.domain.repository.TaskRepository
import com.klecer.gottado.domain.repository.WidgetCategoryRepository
import com.klecer.gottado.notification.TaskNotificationScheduler
import com.klecer.gottado.widget.WidgetUpdateHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

data class CalendarEvent(
    val id: Long,
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
    val allDay: Boolean
)

@Singleton
class CalendarSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val categoryRepository: CategoryRepository,
    private val taskRepository: TaskRepository,
    private val widgetCategoryRepository: WidgetCategoryRepository,
    private val syncPrefs: CalendarSyncPrefs,
    private val calendarDismissedDao: CalendarDismissedDao,
    private val calendarSyncRuleDao: CalendarSyncRuleDao,
    private val notificationScheduler: TaskNotificationScheduler
) {

    companion object {
        private const val TAG = "CalendarSync"
    }

    fun hasCalendarPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED

    fun readEvents(startMillis: Long, endMillis: Long): List<CalendarEvent> {
        if (!hasCalendarPermission()) return emptyList()

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY
        )

        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().let {
            ContentUris.appendId(it, startMillis)
            ContentUris.appendId(it, endMillis)
            it.build()
        }
        Log.d(TAG, "readEvents: uri=$uri, range=[$startMillis .. $endMillis]")

        val events = mutableListOf<CalendarEvent>()
        try {
            context.contentResolver.query(uri, projection, null, null, "${CalendarContract.Instances.BEGIN} ASC")
                ?.use { cursor ->
                    val idIdx = cursor.getColumnIndex(CalendarContract.Instances.EVENT_ID)
                    val titleIdx = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
                    val beginIdx = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
                    val endIdx = cursor.getColumnIndex(CalendarContract.Instances.END)
                    val allDayIdx = cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY)
                    while (cursor.moveToNext()) {
                        val title = cursor.getString(titleIdx) ?: continue
                        events.add(
                            CalendarEvent(
                                id = cursor.getLong(idIdx),
                                title = title,
                                startMillis = cursor.getLong(beginIdx),
                                endMillis = cursor.getLong(endIdx),
                                allDay = cursor.getInt(allDayIdx) == 1
                            )
                        )
                    }
                    Log.d(TAG, "readEvents: cursor returned ${events.size} events: ${events.map { "'${it.title}'" }}")
                } ?: Log.w(TAG, "readEvents: contentResolver.query returned null cursor")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read calendar events", e)
        }
        return events
    }

    suspend fun sync(): Set<Int> {
        if (!hasCalendarPermission()) {
            Log.w(TAG, "sync() skipped – no calendar permission")
            return emptySet()
        }

        val categoryIds = calendarSyncRuleDao.getCategoryIdsWithRules()
        Log.d(TAG, "sync: categories with rules: $categoryIds")
        if (categoryIds.isEmpty()) {
            syncPrefs.lastSyncMillis = System.currentTimeMillis()
            return emptySet()
        }

        val affectedWidgetIds = mutableSetOf<Int>()

        for (catId in categoryIds) {
            val cat = categoryRepository.getById(catId) ?: continue
            val rules = calendarSyncRuleDao.getRulesForCategory(catId)
            if (rules.isEmpty()) continue

            val dateRanges = rules.mapNotNull { rule ->
                try {
                    computeDateRange(CalendarSyncRuleType.valueOf(rule.ruleType))
                } catch (_: IllegalArgumentException) {
                    null
                }
            }
            if (dateRanges.isEmpty()) continue
            Log.d(TAG, "sync: cat=${cat.name}(id=$catId) rules=${rules.map { it.ruleType }} ranges=$dateRanges")

            val allEvents = mutableListOf<CalendarEvent>()
            for ((start, end) in dateRanges) {
                allEvents.addAll(readEvents(start, end))
            }
            val uniqueEvents = allEvents.distinctBy { it.id }
            Log.d(TAG, "sync: total events=${allEvents.size}, unique=${uniqueEvents.size}")

            val existingTasks = taskRepository.getByCategory(cat.id)
            val existingTitles = existingTasks.map { it.contentHtml.trim() }.toSet()
            val dismissedTitles = calendarDismissedDao.getDismissedTitles(cat.id).toSet()
            Log.d(TAG, "sync: existingTitles=$existingTitles")
            Log.d(TAG, "sync: dismissedTitles=$dismissedTitles")

            val maxOrder = existingTasks.maxOfOrNull { it.sortOrder } ?: -1
            var nextOrder = maxOrder + 1
            var addedCount = 0

            for (event in uniqueEvents) {
                val eventText = event.title.trim()
                if (eventText in existingTitles) {
                    Log.d(TAG, "sync: SKIP '$eventText' – already exists as task")
                    continue
                }
                if (eventText in dismissedTitles) {
                    Log.d(TAG, "sync: SKIP '$eventText' – dismissed")
                    continue
                }

                val scheduledTime = if (event.allDay) null else event.startMillis
                val now = System.currentTimeMillis()
                val bulletCol = if (cat.defaultBulletColor != 0) cat.defaultBulletColor else cat.color
                val newTask = TaskEntity(
                    categoryId = cat.id,
                    contentHtml = eventText,
                    completed = false,
                    bulletColor = bulletCol,
                    textColor = cat.defaultTextColor,
                    scheduledTimeMillis = scheduledTime,
                    sortOrder = nextOrder++,
                    createdAtMillis = now,
                    updatedAtMillis = now,
                    fromCalendarSync = true
                )
                val taskId = taskRepository.insert(newTask)
                notificationScheduler.scheduleForTask(newTask.copy(id = taskId))
                Log.d(TAG, "sync: ADDED '$eventText'")
                addedCount++
            }
            Log.d(TAG, "sync: added $addedCount new tasks for category '${cat.name}'")

            val widgetIds = widgetCategoryRepository.getWidgetIdsForCategory(cat.id)
            affectedWidgetIds.addAll(widgetIds)
        }

        for (presetId in affectedWidgetIds) {
            WidgetUpdateHelper.updateAllForPreset(context, presetId)
        }

        syncPrefs.lastSyncMillis = System.currentTimeMillis()
        Log.d(TAG, "sync complete, affected widgets: $affectedWidgetIds")
        return affectedWidgetIds
    }

    private fun computeDateRange(rule: CalendarSyncRuleType): Pair<Long, Long> {
        val start = Calendar.getInstance()
        val end = Calendar.getInstance()

        when (rule) {
            CalendarSyncRuleType.TODAY -> {
                start.startOfDay()
                end.startOfDay()
                end.add(Calendar.DAY_OF_YEAR, 1)
            }
            CalendarSyncRuleType.TOMORROW -> {
                start.startOfDay()
                start.add(Calendar.DAY_OF_YEAR, 1)
                end.timeInMillis = start.timeInMillis
                end.add(Calendar.DAY_OF_YEAR, 1)
            }
            CalendarSyncRuleType.CLOSEST_MONDAY -> closestDay(start, end, Calendar.MONDAY)
            CalendarSyncRuleType.CLOSEST_TUESDAY -> closestDay(start, end, Calendar.TUESDAY)
            CalendarSyncRuleType.CLOSEST_WEDNESDAY -> closestDay(start, end, Calendar.WEDNESDAY)
            CalendarSyncRuleType.CLOSEST_THURSDAY -> closestDay(start, end, Calendar.THURSDAY)
            CalendarSyncRuleType.CLOSEST_FRIDAY -> closestDay(start, end, Calendar.FRIDAY)
            CalendarSyncRuleType.CLOSEST_SATURDAY -> closestDay(start, end, Calendar.SATURDAY)
            CalendarSyncRuleType.CLOSEST_SUNDAY -> closestDay(start, end, Calendar.SUNDAY)
            CalendarSyncRuleType.THIS_WEEK -> {
                start.startOfDay()
                end.set(Calendar.DAY_OF_WEEK, end.firstDayOfWeek)
                end.startOfDay()
                end.add(Calendar.WEEK_OF_YEAR, 1)
            }
            CalendarSyncRuleType.NEXT_WEEK -> {
                start.set(Calendar.DAY_OF_WEEK, start.firstDayOfWeek)
                start.startOfDay()
                start.add(Calendar.WEEK_OF_YEAR, 1)
                end.timeInMillis = start.timeInMillis
                end.add(Calendar.WEEK_OF_YEAR, 1)
            }
            CalendarSyncRuleType.THIS_MONTH -> {
                start.startOfDay()
                end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
                end.startOfDay()
                end.add(Calendar.DAY_OF_YEAR, 1)
            }
            CalendarSyncRuleType.NEXT_MONTH -> {
                start.add(Calendar.MONTH, 1)
                start.set(Calendar.DAY_OF_MONTH, 1)
                start.startOfDay()
                end.timeInMillis = start.timeInMillis
                end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
                end.startOfDay()
                end.add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return start.timeInMillis to end.timeInMillis
    }

    private fun closestDay(start: Calendar, end: Calendar, targetDayOfWeek: Int) {
        start.startOfDay()
        val today = start.get(Calendar.DAY_OF_WEEK)
        val diff = if (today == targetDayOfWeek) 0
                   else ((targetDayOfWeek - today + 7) % 7)
        start.add(Calendar.DAY_OF_YEAR, diff)
        end.timeInMillis = start.timeInMillis
        end.add(Calendar.DAY_OF_YEAR, 1)
    }

    private fun Calendar.startOfDay() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}
