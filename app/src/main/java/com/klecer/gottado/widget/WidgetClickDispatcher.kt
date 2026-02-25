package com.klecer.gottado.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class WidgetClickDispatcher : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.getStringExtra("action") ?: return
        val widgetId = intent.getIntExtra(WidgetIntents.EXTRA_WIDGET_ID, -1)
        Log.d("GottaDoWidget", "WidgetClickDispatcher: action=$action widgetId=$widgetId")

        when (action) {
            WidgetIntents.ACTION_EDIT_TASK -> {
                val taskId = intent.getLongExtra(WidgetIntents.EXTRA_TASK_ID, -1L)
                if (taskId != -1L) {
                    val activityIntent = Intent(context, com.klecer.gottado.record.RecordEditActivity::class.java).apply {
                        this.action = WidgetIntents.ACTION_EDIT_TASK
                        putExtra(WidgetIntents.EXTRA_WIDGET_ID, widgetId)
                        putExtra(WidgetIntents.EXTRA_TASK_ID, taskId)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    context.startActivity(activityIntent)
                }
            }
            WidgetIntents.ACTION_ADD_TASK -> {
                val categoryId = intent.getLongExtra(WidgetIntents.EXTRA_CATEGORY_ID, -1L)
                if (categoryId != -1L) {
                    val activityIntent = Intent(context, com.klecer.gottado.record.RecordEditActivity::class.java).apply {
                        this.action = WidgetIntents.ACTION_ADD_TASK
                        putExtra(WidgetIntents.EXTRA_WIDGET_ID, widgetId)
                        putExtra(WidgetIntents.EXTRA_CATEGORY_ID, categoryId)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    context.startActivity(activityIntent)
                }
            }
            WidgetIntents.ACTION_TOGGLE_TASK_COMPLETED -> {
                val taskId = intent.getLongExtra(WidgetIntents.EXTRA_TASK_ID, -1L)
                if (widgetId != -1 && taskId != -1L) {
                    kotlinx.coroutines.runBlocking {
                        context.widgetEntryPoint().getToggleTaskCompletedUseCase().invoke(taskId)
                    }
                    WidgetUpdateHelper.update(context, widgetId)
                }
            }
            WidgetIntents.ACTION_OPEN_REORDER -> {
                if (widgetId != -1) {
                    val activityIntent = Intent(context, ReorderOverlayActivity::class.java).apply {
                        this.action = WidgetIntents.ACTION_OPEN_REORDER
                        putExtra(WidgetIntents.EXTRA_WIDGET_ID, widgetId)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY
                    }
                    context.startActivity(activityIntent)
                }
            }
            "OPEN_APP" -> {
                val activityIntent = Intent(context, com.klecer.gottado.MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(activityIntent)
            }
        }
    }
}
