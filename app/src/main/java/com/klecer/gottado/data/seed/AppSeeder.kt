package com.klecer.gottado.data.seed

import android.content.Context
import android.graphics.Color
import com.klecer.gottado.data.db.dao.CalendarSyncRuleDao
import com.klecer.gottado.data.db.dao.CategoryDao
import com.klecer.gottado.data.db.dao.RoutineDao
import com.klecer.gottado.data.db.dao.TaskDao
import com.klecer.gottado.data.db.dao.WidgetCategoryJoinDao
import com.klecer.gottado.data.db.dao.WidgetConfigDao
import com.klecer.gottado.data.db.entity.CalendarSyncRuleEntity
import com.klecer.gottado.data.db.entity.CategoryEntity
import com.klecer.gottado.data.db.entity.ReorderHandlePosition
import com.klecer.gottado.data.db.entity.RoutineEntity
import com.klecer.gottado.data.db.entity.RoutineFrequency
import com.klecer.gottado.data.db.entity.RoutineTaskAction
import com.klecer.gottado.data.db.entity.RoutineVisibilityMode
import com.klecer.gottado.data.db.entity.TaskEntity
import com.klecer.gottado.data.db.entity.WidgetCategoryJoinEntity
import com.klecer.gottado.data.db.entity.WidgetConfigEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSeeder @Inject constructor(
    private val categoryDao: CategoryDao,
    private val widgetConfigDao: WidgetConfigDao,
    private val widgetCategoryJoinDao: WidgetCategoryJoinDao,
    private val routineDao: RoutineDao,
    private val calendarSyncRuleDao: CalendarSyncRuleDao,
    private val taskDao: TaskDao
) {
    private companion object {
        const val PREFS_NAME = "app_seeder"
        const val KEY_SEEDED = "seeded"
        val RED = Color.parseColor("#FFD32F2F")
        val ORANGE = Color.parseColor("#FFFF9800")
        val BLUE = Color.parseColor("#FF1976D2")
        val GREEN = Color.parseColor("#FF388E3C")
    }

    suspend fun seedIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_SEEDED, false)) return
        val existingCats = categoryDao.getAll()
        if (existingCats.isNotEmpty()) {
            prefs.edit().putBoolean(KEY_SEEDED, true).apply()
            return
        }

        val todayId = categoryDao.insert(CategoryEntity(
            name = "TODAY",
            sortOrder = 0,
            showCheckboxInsteadOfBullet = true,
            defaultBulletColor = RED,
            notifyOnTime = true
        ))

        val tomorrowId = categoryDao.insert(CategoryEntity(
            name = "TOMORROW",
            sortOrder = 1,
            showCheckboxInsteadOfBullet = true,
            defaultBulletColor = ORANGE
        ))

        val dailyId = categoryDao.insert(CategoryEntity(
            name = "DAILY",
            sortOrder = 2,
            showCheckboxInsteadOfBullet = true,
            defaultBulletColor = RED
        ))

        val weeklyId = categoryDao.insert(CategoryEntity(
            name = "WEEKLY",
            sortOrder = 3,
            showCheckboxInsteadOfBullet = true,
            defaultBulletColor = GREEN
        ))

        val somedayId = categoryDao.insert(CategoryEntity(
            name = "SOMEDAY",
            sortOrder = 4,
            showCheckboxInsteadOfBullet = true,
            defaultBulletColor = ORANGE
        ))

        val notesId = categoryDao.insert(CategoryEntity(
            name = "NOTES",
            sortOrder = 5,
            showCheckboxInsteadOfBullet = false,
            defaultBulletColor = BLUE
        ))

        calendarSyncRuleDao.insert(CalendarSyncRuleEntity(categoryId = todayId, ruleType = "TODAY"))
        calendarSyncRuleDao.insert(CalendarSyncRuleEntity(categoryId = tomorrowId, ruleType = "TOMORROW"))

        routineDao.insert(RoutineEntity(
            categoryId = tomorrowId,
            name = "Move all to TODAY",
            frequency = RoutineFrequency.DAILY,
            scheduleTimeHour = 0,
            scheduleTimeMinute = 0,
            visibilityMode = RoutineVisibilityMode.VISIBLE,
            incompleteAction = RoutineTaskAction.MOVE,
            incompleteMoveToCategoryId = todayId,
            completedAction = RoutineTaskAction.MOVE,
            completedMoveToCategoryId = todayId
        ))

        routineDao.insert(RoutineEntity(
            categoryId = weeklyId,
            name = "Uncheck entries weekly",
            frequency = RoutineFrequency.WEEKLY,
            scheduleTimeHour = 23,
            scheduleTimeMinute = 59,
            scheduleDayOfWeek = java.util.Calendar.SUNDAY,
            visibilityMode = RoutineVisibilityMode.VISIBLE,
            incompleteAction = RoutineTaskAction.UNCOMPLETE,
            completedAction = RoutineTaskAction.UNCOMPLETE
        ))

        routineDao.insert(RoutineEntity(
            categoryId = dailyId,
            name = "Uncheck entries",
            frequency = RoutineFrequency.DAILY,
            scheduleTimeHour = 0,
            scheduleTimeMinute = 0,
            visibilityMode = RoutineVisibilityMode.VISIBLE,
            incompleteAction = RoutineTaskAction.UNCOMPLETE,
            completedAction = RoutineTaskAction.UNCOMPLETE
        ))

        val presetId = -1
        widgetConfigDao.insert(WidgetConfigEntity(
            widgetId = presetId,
            title = "DEFAULT",
            subtitle = null,
            note = null,
            backgroundColor = Color.parseColor("#80000000"),
            backgroundAlpha = 0.8f,
            categoryFontSizeSp = 15f,
            recordFontSizeSp = 14f,
            defaultTextColor = Color.WHITE,
            reorderHandlePosition = ReorderHandlePosition.NONE,
            titleColor = Color.WHITE,
            subtitleColor = Color.WHITE,
            bulletSizeDp = 5,
            checkboxSizeDp = 13,
            showTitleOnWidget = false,
            buttonsAtBottom = true
        ))

        val categoryOrder = listOf(todayId, dailyId, tomorrowId, somedayId, notesId)
        categoryOrder.forEachIndexed { index, catId ->
            widgetCategoryJoinDao.insert(WidgetCategoryJoinEntity(
                widgetId = presetId,
                categoryId = catId,
                sortOrder = index,
                visible = true
            ))
        }

        val now = System.currentTimeMillis()
        seedEntries(todayId, RED, now, listOf(
            "Your tasks for today",
            "Calendar events sync here automatically",
            "To enable sync: open ⚙ Settings in the app → enable Calendar sync",
            "Timed entries trigger notifications — enable in ⚙ Settings → Notifications, then per category"
        ))
        seedEntries(tomorrowId, ORANGE, now, listOf(
            "Tasks for tomorrow",
            "Calendar events for tomorrow sync here",
            "All tasks move to TODAY daily (including completed)"
        ))
        seedEntries(dailyId, RED, now, listOf(
            "Recurring daily tasks",
            "Checked entries automatically uncheck every day",
            "Add tasks you repeat daily"
        ))
        seedEntries(weeklyId, GREEN, now, listOf(
            "Weekly recurring tasks",
            "Checked entries automatically uncheck every Sunday at 23:59",
            "Add tasks you repeat each week"
        ))
        seedEntries(somedayId, ORANGE, now, listOf(
            "Category for tasks you SURELY will finish... someday."
        ))
        seedEntries(notesId, BLUE, now, listOf(
            "Welcome to GottaDo!",
            "<b>Adding widget:</b> long-press home screen → Widgets → GottaDo",
            "<b>Widget buttons</b> (bottom of widget):",
            "↔ Switch widget template",
            "⚙ Open app settings",
            "↑↓ Reorder entries",
            "<b>Adding entries:</b> tap a category name",
            "<b>Editing entries:</b> tap the entry text",
            "<b>Completing:</b> tap the checkbox or bullet",
            "<b>Deleting:</b> via Edit, or enable delete button in category settings",
            "<b>Categories:</b> manage in the Categories tab",
            "<b>Calendar sync:</b> enable in ⚙ Settings (in app) → set sync rules per category",
            "<b>Notifications:</b> enable in ⚙ Settings (in app) → enable per category",
            "<b>Routines:</b> automate tasks (move, uncheck, delete) on a schedule"
        ))

        prefs.edit().putBoolean(KEY_SEEDED, true).apply()
    }

    private suspend fun seedEntries(categoryId: Long, bulletColor: Int, now: Long, texts: List<String>) {
        texts.forEachIndexed { index, text ->
            taskDao.insert(TaskEntity(
                categoryId = categoryId,
                contentHtml = text,
                bulletColor = bulletColor,
                sortOrder = index,
                createdAtMillis = now,
                updatedAtMillis = now
            ))
        }
    }
}
