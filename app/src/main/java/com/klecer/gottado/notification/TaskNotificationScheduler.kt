package com.klecer.gottado.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.klecer.gottado.calendar.CalendarSyncPrefs
import com.klecer.gottado.data.db.entity.CategoryEntity
import com.klecer.gottado.data.db.entity.TaskEntity
import com.klecer.gottado.domain.repository.CategoryRepository
import com.klecer.gottado.domain.repository.TaskRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskNotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncPrefs: CalendarSyncPrefs,
    private val categoryRepository: CategoryRepository,
    private val taskRepository: TaskRepository
) {
    companion object {
        private const val TAG = "TaskNotifScheduler"
    }

    suspend fun scheduleForTask(task: TaskEntity) {
        if (!syncPrefs.notificationsEnabled) return
        if (task.completed) return
        val category = categoryRepository.getById(task.categoryId) ?: return
        if (!category.notifyOnTime) return
        val scheduledTime = task.scheduledTimeMillis ?: return

        val notifyAt = scheduledTime - category.notifyMinutesBefore * 60_000L
        if (notifyAt <= System.currentTimeMillis()) return

        val title = "GottaDo – ${category.name}"
        val text = task.contentHtml.replace(Regex("<[^>]*>"), "").trim()
        schedule(context, task.id, notifyAt, title, text)
    }

    suspend fun cancelForTask(taskId: Long) {
        cancel(context, taskId)
    }

    suspend fun rescheduleForCategory(categoryId: Long) {
        if (!syncPrefs.notificationsEnabled) {
            cancelAllForCategory(categoryId)
            return
        }
        val category = categoryRepository.getById(categoryId) ?: return
        val tasks = taskRepository.getByCategory(categoryId)
        for (task in tasks) {
            if (!category.notifyOnTime || task.scheduledTimeMillis == null || task.completed) {
                cancel(context, task.id)
                continue
            }
            val notifyAt = task.scheduledTimeMillis - category.notifyMinutesBefore * 60_000L
            if (notifyAt <= System.currentTimeMillis()) {
                cancel(context, task.id)
                continue
            }
            val title = "GottaDo – ${category.name}"
            val text = task.contentHtml.replace(Regex("<[^>]*>"), "").trim()
            schedule(context, task.id, notifyAt, title, text)
        }
    }

    private suspend fun cancelAllForCategory(categoryId: Long) {
        val tasks = taskRepository.getByCategory(categoryId)
        for (task in tasks) cancel(context, task.id)
    }

    private fun schedule(ctx: Context, taskId: Long, triggerAtMillis: Long, title: String, text: String) {
        val alarmManager = ctx.getSystemService(AlarmManager::class.java) ?: return
        val pi = buildPendingIntent(ctx, taskId, title, text)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            }
            Log.d(TAG, "Scheduled notification for task $taskId at $triggerAtMillis")
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            Log.w(TAG, "Exact alarm denied, used inexact for task $taskId", e)
        }
    }

    private fun cancel(ctx: Context, taskId: Long) {
        val alarmManager = ctx.getSystemService(AlarmManager::class.java) ?: return
        val pi = buildPendingIntent(ctx, taskId, "", "")
        alarmManager.cancel(pi)
    }

    private fun buildPendingIntent(ctx: Context, taskId: Long, title: String, text: String): PendingIntent {
        val intent = Intent(ctx, TaskNotificationReceiver::class.java).apply {
            action = "com.klecer.gottado.TASK_NOTIFICATION_$taskId"
            putExtra(TaskNotificationReceiver.EXTRA_TASK_ID, taskId)
            putExtra(TaskNotificationReceiver.EXTRA_TITLE, title)
            putExtra(TaskNotificationReceiver.EXTRA_TEXT, text)
        }
        return PendingIntent.getBroadcast(
            ctx, taskId.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
