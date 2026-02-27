package com.klecer.gottado.calendar

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.klecer.gottado.data.db.dao.CalendarDismissedDao
import com.klecer.gottado.data.db.entity.TaskEntity
import com.klecer.gottado.domain.repository.CategoryRepository
import com.klecer.gottado.domain.repository.TaskRepository
import com.klecer.gottado.domain.repository.WidgetCategoryRepository
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
    private val calendarDismissedDao: CalendarDismissedDao
) {

    companion object {
        private const val TAG = "CalendarSync"
    }

    fun hasCalendarPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
                PackageManager.PERMISSION_GRANTED

    /**
     * Read calendar events for [daysAhead] days starting from today.
     * daysAhead=0 means today only.
     */
    fun readEvents(daysAhead: Int = 0): List<CalendarEvent> {
        if (!hasCalendarPermission()) return emptyList()

        val now = Calendar.getInstance()
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endOfRange = Calendar.getInstance().apply {
            timeInMillis = startOfDay.timeInMillis
            add(Calendar.DAY_OF_YEAR, daysAhead + 1)
        }

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY
        )

        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().let {
            ContentUris.appendId(it, startOfDay.timeInMillis)
            ContentUris.appendId(it, endOfRange.timeInMillis)
            it.build()
        }

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
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read calendar events", e)
        }
        return events
    }

    /**
     * Run the full sync: read events, create tasks in eligible categories, update widgets.
     * Returns the set of widget IDs that were affected.
     */
    suspend fun sync(): Set<Int> {
        if (!hasCalendarPermission()) {
            Log.w(TAG, "sync() skipped – no calendar permission")
            return emptySet()
        }

        val daysAhead = syncPrefs.daysAhead
        val events = readEvents(daysAhead)
        Log.d(TAG, "sync: ${events.size} events for daysAhead=$daysAhead")

        val categories = categoryRepository.getAll().filter { it.syncWithCalendarToday }
        if (categories.isEmpty()) {
            syncPrefs.lastSyncMillis = System.currentTimeMillis()
            return emptySet()
        }

        val affectedWidgetIds = mutableSetOf<Int>()

        for (cat in categories) {
            val existingTasks = taskRepository.getByCategory(cat.id)
            val existingTitles = existingTasks.map { it.contentHtml.trim() }.toSet()
            val dismissedTitles = calendarDismissedDao.getDismissedTitles(cat.id).toSet()
            val maxOrder = existingTasks.maxOfOrNull { it.sortOrder } ?: -1
            var nextOrder = maxOrder + 1

            for (event in events) {
                val eventText = event.title.trim()
                if (eventText in existingTitles || eventText in dismissedTitles) continue

                val scheduledTime = if (event.allDay) null else event.startMillis
                val now = System.currentTimeMillis()
                taskRepository.insert(
                    TaskEntity(
                        categoryId = cat.id,
                        contentHtml = eventText,
                        completed = false,
                        bulletColor = cat.color,
                        textColor = 0,
                        scheduledTimeMillis = scheduledTime,
                        sortOrder = nextOrder++,
                        createdAtMillis = now,
                        updatedAtMillis = now,
                        fromCalendarSync = true
                    )
                )
            }

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
}
