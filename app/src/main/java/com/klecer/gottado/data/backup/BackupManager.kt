package com.klecer.gottado.data.backup

import android.content.Context
import android.net.Uri
import com.klecer.gottado.data.db.dao.CalendarSyncRuleDao
import com.klecer.gottado.data.db.dao.CategoryDao
import com.klecer.gottado.data.db.dao.RoutineDao
import com.klecer.gottado.data.db.dao.TaskDao
import com.klecer.gottado.data.db.dao.TrashEntryDao
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
import com.klecer.gottado.data.db.entity.TrashEntryEntity
import com.klecer.gottado.data.db.entity.WidgetCategoryJoinEntity
import com.klecer.gottado.data.db.entity.WidgetConfigEntity
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    private val categoryDao: CategoryDao,
    private val taskDao: TaskDao,
    private val routineDao: RoutineDao,
    private val trashDao: TrashEntryDao,
    private val widgetConfigDao: WidgetConfigDao,
    private val widgetCategoryJoinDao: WidgetCategoryJoinDao,
    private val calendarSyncRuleDao: CalendarSyncRuleDao
) {
    companion object {
        private const val VERSION = 1
    }

    suspend fun exportToUri(context: Context, uri: Uri) {
        val json = buildExportJson()
        context.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(json.toString(2).toByteArray(Charsets.UTF_8))
        } ?: throw IllegalStateException("Cannot open output stream")
    }

    suspend fun importFromUri(context: Context, uri: Uri) {
        val text = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader(Charsets.UTF_8).readText()
        } ?: throw IllegalStateException("Cannot open input stream")
        val json = JSONObject(text)
        restoreFromJson(json)
    }

    private suspend fun buildExportJson(): JSONObject {
        val root = JSONObject()
        root.put("version", VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        root.put("categories", JSONArray().apply {
            categoryDao.getAll().forEach { put(categoryToJson(it)) }
        })
        root.put("tasks", JSONArray().apply {
            taskDao.getAll().forEach { put(taskToJson(it)) }
        })
        root.put("routines", JSONArray().apply {
            routineDao.getAll().forEach { put(routineToJson(it)) }
        })
        root.put("trash", JSONArray().apply {
            trashDao.getAll().forEach { put(trashToJson(it)) }
        })
        root.put("widgetConfigs", JSONArray().apply {
            widgetConfigDao.getAll().forEach { put(widgetConfigToJson(it)) }
        })
        root.put("widgetCategoryJoins", JSONArray().apply {
            widgetCategoryJoinDao.getAll().forEach { put(widgetJoinToJson(it)) }
        })
        root.put("calendarSyncRules", JSONArray().apply {
            calendarSyncRuleDao.getAll().forEach { put(syncRuleToJson(it)) }
        })

        return root
    }

    private suspend fun restoreFromJson(root: JSONObject) {
        widgetCategoryJoinDao.deleteAll()
        calendarSyncRuleDao.deleteAll()
        taskDao.deleteAll()
        routineDao.deleteAll()
        trashDao.deleteAll()
        widgetConfigDao.deleteAll()
        categoryDao.deleteAll()

        val catArray = root.getJSONArray("categories")
        for (i in 0 until catArray.length()) {
            categoryDao.insert(jsonToCategory(catArray.getJSONObject(i)))
        }

        val taskArray = root.getJSONArray("tasks")
        for (i in 0 until taskArray.length()) {
            taskDao.insert(jsonToTask(taskArray.getJSONObject(i)))
        }

        val routineArray = root.getJSONArray("routines")
        for (i in 0 until routineArray.length()) {
            routineDao.insert(jsonToRoutine(routineArray.getJSONObject(i)))
        }

        val trashArray = root.getJSONArray("trash")
        for (i in 0 until trashArray.length()) {
            trashDao.insert(jsonToTrash(trashArray.getJSONObject(i)))
        }

        val wcArray = root.getJSONArray("widgetConfigs")
        for (i in 0 until wcArray.length()) {
            widgetConfigDao.insert(jsonToWidgetConfig(wcArray.getJSONObject(i)))
        }

        val wjArray = root.getJSONArray("widgetCategoryJoins")
        for (i in 0 until wjArray.length()) {
            widgetCategoryJoinDao.insert(jsonToWidgetJoin(wjArray.getJSONObject(i)))
        }

        val srArray = root.getJSONArray("calendarSyncRules")
        for (i in 0 until srArray.length()) {
            calendarSyncRuleDao.insert(jsonToSyncRule(srArray.getJSONObject(i)))
        }
    }

    // ── Serializers ──

    private fun categoryToJson(c: CategoryEntity) = JSONObject().apply {
        put("id", c.id)
        put("name", c.name)
        put("sortOrder", c.sortOrder)
        put("showCheckboxInsteadOfBullet", c.showCheckboxInsteadOfBullet)
        put("tasksWithTimeFirst", c.tasksWithTimeFirst)
        put("categoryType", c.categoryType)
        put("color", c.color)
        put("syncWithCalendarToday", c.syncWithCalendarToday)
        put("showCalendarIcon", c.showCalendarIcon)
        put("showDeleteButton", c.showDeleteButton)
        put("notifyOnTime", c.notifyOnTime)
        put("notifyMinutesBefore", c.notifyMinutesBefore)
        put("autoSortTimedEntries", c.autoSortTimedEntries)
        put("timedEntriesAscending", c.timedEntriesAscending)
        put("defaultBulletColor", c.defaultBulletColor)
        put("defaultTextColor", c.defaultTextColor)
    }

    private fun taskToJson(t: TaskEntity) = JSONObject().apply {
        put("id", t.id)
        put("categoryId", t.categoryId)
        put("contentHtml", t.contentHtml)
        put("completed", t.completed)
        put("bulletColor", t.bulletColor)
        put("textColor", t.textColor)
        put("scheduledTimeMillis", t.scheduledTimeMillis ?: JSONObject.NULL)
        put("sortOrder", t.sortOrder)
        put("createdAtMillis", t.createdAtMillis)
        put("updatedAtMillis", t.updatedAtMillis)
        put("fromCalendarSync", t.fromCalendarSync)
    }

    private fun routineToJson(r: RoutineEntity) = JSONObject().apply {
        put("id", r.id)
        put("categoryId", r.categoryId)
        put("name", r.name ?: JSONObject.NULL)
        put("frequency", r.frequency.name)
        put("scheduleTimeHour", r.scheduleTimeHour)
        put("scheduleTimeMinute", r.scheduleTimeMinute)
        put("scheduleDayOfWeek", r.scheduleDayOfWeek ?: JSONObject.NULL)
        put("scheduleDayOfMonth", r.scheduleDayOfMonth ?: JSONObject.NULL)
        put("scheduleMonth", r.scheduleMonth ?: JSONObject.NULL)
        put("scheduleDay", r.scheduleDay ?: JSONObject.NULL)
        put("visibilityMode", r.visibilityMode.name)
        put("visibilityFrom", r.visibilityFrom ?: JSONObject.NULL)
        put("visibilityTo", r.visibilityTo ?: JSONObject.NULL)
        put("incompleteAction", r.incompleteAction.name)
        put("incompleteMoveToCategoryId", r.incompleteMoveToCategoryId ?: JSONObject.NULL)
        put("completedAction", r.completedAction.name)
        put("completedMoveToCategoryId", r.completedMoveToCategoryId ?: JSONObject.NULL)
    }

    private fun trashToJson(t: TrashEntryEntity) = JSONObject().apply {
        put("id", t.id)
        put("originalCategoryId", t.originalCategoryId)
        put("contentHtml", t.contentHtml)
        put("completed", t.completed)
        put("bulletColor", t.bulletColor)
        put("scheduledTimeMillis", t.scheduledTimeMillis ?: JSONObject.NULL)
        put("sortOrder", t.sortOrder)
        put("deletedAtMillis", t.deletedAtMillis)
        put("categoryName", t.categoryName)
    }

    private fun widgetConfigToJson(w: WidgetConfigEntity) = JSONObject().apply {
        put("widgetId", w.widgetId)
        put("title", w.title ?: JSONObject.NULL)
        put("subtitle", w.subtitle ?: JSONObject.NULL)
        put("note", w.note ?: JSONObject.NULL)
        put("backgroundColor", w.backgroundColor)
        put("backgroundAlpha", w.backgroundAlpha.toDouble())
        put("categoryFontSizeSp", w.categoryFontSizeSp.toDouble())
        put("recordFontSizeSp", w.recordFontSizeSp.toDouble())
        put("defaultTextColor", w.defaultTextColor)
        put("reorderHandlePosition", w.reorderHandlePosition.name)
        put("titleColor", w.titleColor)
        put("subtitleColor", w.subtitleColor)
        put("bulletSizeDp", w.bulletSizeDp)
        put("checkboxSizeDp", w.checkboxSizeDp)
        put("showTitleOnWidget", w.showTitleOnWidget)
        put("buttonsAtBottom", w.buttonsAtBottom)
        put("collapsibleCategories", w.collapsibleCategories)
        put("widgetStyle", w.widgetStyle)
        put("fontFamily", w.fontFamily)
    }

    private fun widgetJoinToJson(j: WidgetCategoryJoinEntity) = JSONObject().apply {
        put("widgetId", j.widgetId)
        put("categoryId", j.categoryId)
        put("sortOrder", j.sortOrder)
        put("visible", j.visible)
    }

    private fun syncRuleToJson(r: CalendarSyncRuleEntity) = JSONObject().apply {
        put("id", r.id)
        put("categoryId", r.categoryId)
        put("ruleType", r.ruleType)
    }

    // ── Deserializers ──

    private fun jsonToCategory(j: JSONObject) = CategoryEntity(
        id = j.getLong("id"),
        name = j.getString("name"),
        sortOrder = j.getInt("sortOrder"),
        showCheckboxInsteadOfBullet = j.optBoolean("showCheckboxInsteadOfBullet"),
        tasksWithTimeFirst = j.optBoolean("tasksWithTimeFirst", true),
        categoryType = j.optString("categoryType", "NONE"),
        color = j.optInt("color", 0),
        syncWithCalendarToday = j.optBoolean("syncWithCalendarToday"),
        showCalendarIcon = j.optBoolean("showCalendarIcon", true),
        showDeleteButton = j.optBoolean("showDeleteButton"),
        notifyOnTime = j.optBoolean("notifyOnTime"),
        notifyMinutesBefore = j.optInt("notifyMinutesBefore", 5),
        autoSortTimedEntries = j.optBoolean("autoSortTimedEntries", true),
        timedEntriesAscending = j.optBoolean("timedEntriesAscending", true),
        defaultBulletColor = j.optInt("defaultBulletColor", 0),
        defaultTextColor = j.optInt("defaultTextColor", 0)
    )

    private fun jsonToTask(j: JSONObject) = TaskEntity(
        id = j.getLong("id"),
        categoryId = j.getLong("categoryId"),
        contentHtml = j.getString("contentHtml"),
        completed = j.optBoolean("completed"),
        bulletColor = j.optInt("bulletColor", 0),
        textColor = j.optInt("textColor", 0),
        scheduledTimeMillis = if (j.isNull("scheduledTimeMillis")) null else j.getLong("scheduledTimeMillis"),
        sortOrder = j.optInt("sortOrder", 0),
        createdAtMillis = j.optLong("createdAtMillis", 0L),
        updatedAtMillis = j.optLong("updatedAtMillis", 0L),
        fromCalendarSync = j.optBoolean("fromCalendarSync")
    )

    private fun jsonToRoutine(j: JSONObject) = RoutineEntity(
        id = j.getLong("id"),
        categoryId = j.getLong("categoryId"),
        name = if (j.isNull("name")) null else j.getString("name"),
        frequency = RoutineFrequency.valueOf(j.getString("frequency")),
        scheduleTimeHour = j.optInt("scheduleTimeHour", 0),
        scheduleTimeMinute = j.optInt("scheduleTimeMinute", 0),
        scheduleDayOfWeek = if (j.isNull("scheduleDayOfWeek")) null else j.getInt("scheduleDayOfWeek"),
        scheduleDayOfMonth = if (j.isNull("scheduleDayOfMonth")) null else j.getInt("scheduleDayOfMonth"),
        scheduleMonth = if (j.isNull("scheduleMonth")) null else j.getInt("scheduleMonth"),
        scheduleDay = if (j.isNull("scheduleDay")) null else j.getInt("scheduleDay"),
        visibilityMode = RoutineVisibilityMode.valueOf(j.optString("visibilityMode", "VISIBLE")),
        visibilityFrom = if (j.isNull("visibilityFrom")) null else j.getLong("visibilityFrom"),
        visibilityTo = if (j.isNull("visibilityTo")) null else j.getLong("visibilityTo"),
        incompleteAction = RoutineTaskAction.valueOf(j.optString("incompleteAction", "NONE")),
        incompleteMoveToCategoryId = if (j.isNull("incompleteMoveToCategoryId")) null else j.getLong("incompleteMoveToCategoryId"),
        completedAction = RoutineTaskAction.valueOf(j.optString("completedAction", "NONE")),
        completedMoveToCategoryId = if (j.isNull("completedMoveToCategoryId")) null else j.getLong("completedMoveToCategoryId")
    )

    private fun jsonToTrash(j: JSONObject) = TrashEntryEntity(
        id = j.getLong("id"),
        originalCategoryId = j.getLong("originalCategoryId"),
        contentHtml = j.getString("contentHtml"),
        completed = j.optBoolean("completed"),
        bulletColor = j.optInt("bulletColor", 0),
        scheduledTimeMillis = if (j.isNull("scheduledTimeMillis")) null else j.getLong("scheduledTimeMillis"),
        sortOrder = j.optInt("sortOrder", 0),
        deletedAtMillis = j.getLong("deletedAtMillis"),
        categoryName = j.optString("categoryName", "")
    )

    private fun jsonToWidgetConfig(j: JSONObject) = WidgetConfigEntity(
        widgetId = j.getInt("widgetId"),
        title = if (j.isNull("title")) null else j.getString("title"),
        subtitle = if (j.isNull("subtitle")) null else j.getString("subtitle"),
        note = if (j.isNull("note")) null else j.getString("note"),
        backgroundColor = j.getInt("backgroundColor"),
        backgroundAlpha = j.getDouble("backgroundAlpha").toFloat(),
        categoryFontSizeSp = j.getDouble("categoryFontSizeSp").toFloat(),
        recordFontSizeSp = j.getDouble("recordFontSizeSp").toFloat(),
        defaultTextColor = j.getInt("defaultTextColor"),
        reorderHandlePosition = ReorderHandlePosition.valueOf(j.optString("reorderHandlePosition", "NONE")),
        titleColor = j.optInt("titleColor", -1),
        subtitleColor = j.optInt("subtitleColor", -1),
        bulletSizeDp = j.optInt("bulletSizeDp", 5),
        checkboxSizeDp = j.optInt("checkboxSizeDp", 13),
        showTitleOnWidget = j.optBoolean("showTitleOnWidget", true),
        buttonsAtBottom = j.optBoolean("buttonsAtBottom"),
        collapsibleCategories = j.optBoolean("collapsibleCategories"),
        widgetStyle = j.optString("widgetStyle", "DEFAULT"),
        fontFamily = j.optString("fontFamily", "sans-serif")
    )

    private fun jsonToWidgetJoin(j: JSONObject) = WidgetCategoryJoinEntity(
        widgetId = j.getInt("widgetId"),
        categoryId = j.getLong("categoryId"),
        sortOrder = j.optInt("sortOrder", 0),
        visible = j.optBoolean("visible", true)
    )

    private fun jsonToSyncRule(j: JSONObject) = CalendarSyncRuleEntity(
        id = j.optLong("id", 0L),
        categoryId = j.getLong("categoryId"),
        ruleType = j.getString("ruleType")
    )
}
