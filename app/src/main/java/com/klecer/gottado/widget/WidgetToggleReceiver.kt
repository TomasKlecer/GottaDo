package com.klecer.gottado.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.runBlocking

/**
 * Handles long-press (or toggle) on a record row: toggles task completed and refreshes the widget.
 */
class WidgetToggleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != WidgetIntents.ACTION_TOGGLE_TASK_COMPLETED) return
        val widgetId = intent.getIntExtra(WidgetIntents.EXTRA_WIDGET_ID, -1)
        val taskId = intent.getLongExtra(WidgetIntents.EXTRA_TASK_ID, -1L)
        if (widgetId == -1 || taskId == -1L) return
        runBlocking {
            context.widgetEntryPoint().getToggleTaskCompletedUseCase().invoke(taskId)
        }
        WidgetUpdateHelper.update(context, widgetId)
    }
}
