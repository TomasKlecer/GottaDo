package com.klecer.gottado.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.klecer.gottado.MainActivity
import com.klecer.gottado.record.RecordEditActivity

object WidgetIntents {
    const val ACTION_EDIT_TASK = "com.klecer.gottado.widget.ACTION_EDIT_TASK"
    const val ACTION_ADD_TASK = "com.klecer.gottado.widget.ACTION_ADD_TASK"
    const val ACTION_TOGGLE_TASK_COMPLETED = "com.klecer.gottado.widget.ACTION_TOGGLE_TASK_COMPLETED"
    const val ACTION_OPEN_REORDER = "com.klecer.gottado.widget.ACTION_OPEN_REORDER"
    const val ACTION_OPEN_CATEGORY_SETTINGS = "com.klecer.gottado.widget.ACTION_OPEN_CATEGORY_SETTINGS"
    const val ACTION_UNDO_DELETE = "com.klecer.gottado.widget.ACTION_UNDO_DELETE"
    const val ACTION_DELETE_TASK = "com.klecer.gottado.widget.ACTION_DELETE_TASK"
    const val ACTION_MOVE_TASK_UP = "com.klecer.gottado.widget.ACTION_MOVE_TASK_UP"
    const val ACTION_MOVE_TASK_DOWN = "com.klecer.gottado.widget.ACTION_MOVE_TASK_DOWN"
    const val ACTION_PICK_PRESET = "com.klecer.gottado.widget.ACTION_PICK_PRESET"
    const val ACTION_TOGGLE_COLLAPSE = "com.klecer.gottado.widget.ACTION_TOGGLE_COLLAPSE"
    const val ACTION_LIST_CLICK = "com.klecer.gottado.widget.ACTION_LIST_CLICK"

    const val EXTRA_WIDGET_ID = "widgetId"
    const val EXTRA_TASK_ID = "taskId"
    const val EXTRA_CATEGORY_ID = "categoryId"
    const val EXTRA_TRASH_ID = "trashId"

    private fun pendingIntentFlags() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

    private fun immutableFlags() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

    fun listClickTemplate(context: Context): PendingIntent {
        val intent = Intent(context, WidgetTrampolineActivity::class.java).apply {
            action = ACTION_LIST_CLICK
        }
        return PendingIntent.getActivity(context, 0, intent, pendingIntentFlags())
    }

    fun editTaskPendingIntent(context: Context, widgetId: Int, taskId: Long, requestCode: Int): PendingIntent {
        val intent = Intent(context, RecordEditActivity::class.java).apply {
            action = ACTION_EDIT_TASK
            putExtra(EXTRA_WIDGET_ID, widgetId)
            putExtra(EXTRA_TASK_ID, taskId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(context, requestCode, intent, immutableFlags())
    }

    fun addTaskPendingIntent(context: Context, widgetId: Int, categoryId: Long, requestCode: Int): PendingIntent {
        val intent = Intent(context, RecordEditActivity::class.java).apply {
            action = ACTION_ADD_TASK
            putExtra(EXTRA_WIDGET_ID, widgetId)
            putExtra(EXTRA_CATEGORY_ID, categoryId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(context, requestCode, intent, immutableFlags())
    }

    fun toggleTaskCompletedPendingIntent(context: Context, widgetId: Int, taskId: Long, requestCode: Int): PendingIntent {
        val intent = Intent(context, WidgetToggleReceiver::class.java).apply {
            action = ACTION_TOGGLE_TASK_COMPLETED
            putExtra(EXTRA_WIDGET_ID, widgetId)
            putExtra(EXTRA_TASK_ID, taskId)
        }
        return PendingIntent.getBroadcast(context, requestCode, intent, immutableFlags())
    }

    fun openReorderPendingIntent(context: Context, widgetId: Int, requestCode: Int): PendingIntent {
        val intent = Intent(context, ReorderOverlayActivity::class.java).apply {
            action = ACTION_OPEN_REORDER
            putExtra(EXTRA_WIDGET_ID, widgetId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY
        }
        return PendingIntent.getActivity(context, requestCode, intent, immutableFlags())
    }

    fun openAppPendingIntent(context: Context, presetId: Int, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "widget_settings/$presetId")
        }
        return PendingIntent.getActivity(context, requestCode, intent, immutableFlags())
    }

    fun openPresetPickerPendingIntent(context: Context, appWidgetId: Int, requestCode: Int): PendingIntent {
        val intent = Intent(context, WidgetPresetPickerActivity::class.java).apply {
            putExtra(EXTRA_WIDGET_ID, appWidgetId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY
        }
        return PendingIntent.getActivity(context, requestCode, intent, immutableFlags())
    }
}
