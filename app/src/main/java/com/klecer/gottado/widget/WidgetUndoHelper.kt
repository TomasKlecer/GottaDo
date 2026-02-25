package com.klecer.gottado.widget

import android.content.Context
import android.os.Handler
import android.os.Looper

data class PendingUndo(val trashId: Long, val widgetId: Int, val timestamp: Long)

object WidgetUndoHelper {

    private const val PREFS = "widget_undo"
    private const val KEY_TRASH_ID = "trash_id"
    private const val KEY_WIDGET_ID = "widget_id"
    private const val KEY_TIMESTAMP = "timestamp"
    private const val UNDO_WINDOW_MS = 5000L

    fun storePendingUndo(context: Context, trashId: Long, widgetId: Int) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(KEY_TRASH_ID, trashId)
            .putInt(KEY_WIDGET_ID, widgetId)
            .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            .apply()

        val appContext = context.applicationContext
        Handler(Looper.getMainLooper()).postDelayed({
            clearPendingUndo(appContext)
            WidgetUpdateHelper.update(appContext, widgetId)
        }, UNDO_WINDOW_MS)
    }

    fun getPendingUndo(context: Context): PendingUndo? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val trashId = prefs.getLong(KEY_TRASH_ID, -1L)
        val widgetId = prefs.getInt(KEY_WIDGET_ID, -1)
        val timestamp = prefs.getLong(KEY_TIMESTAMP, 0L)
        if (trashId == -1L || widgetId == -1) return null
        if (System.currentTimeMillis() - timestamp > UNDO_WINDOW_MS) {
            clearPendingUndo(context)
            return null
        }
        return PendingUndo(trashId, widgetId, timestamp)
    }

    fun clearPendingUndo(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().clear().apply()
    }
}
