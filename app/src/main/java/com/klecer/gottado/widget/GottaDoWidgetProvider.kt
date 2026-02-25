package com.klecer.gottado.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.runBlocking

class GottaDoWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive action=${intent.action}")
        try {
            super.onReceive(context, intent)
        } catch (e: Throwable) {
            Log.e(TAG, "onReceive crashed", e)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Log.d(TAG, "onUpdate widgetIds=${appWidgetIds.toList()}")
        for (widgetId in appWidgetIds) {
            try {
                WidgetUpdateHelper.update(context, widgetId)
            } catch (e: Throwable) {
                Log.e(TAG, "onUpdate failed for widgetId=$widgetId", e)
                WidgetUpdateHelper.showError(context, widgetId, "Update error: ${e.message}")
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        Log.d(TAG, "onDeleted widgetIds=${appWidgetIds.toList()}")
        try {
            val entryPoint = context.widgetEntryPoint()
            val configRepo = entryPoint.getWidgetConfigRepository()
            val categoryRepo = entryPoint.getWidgetCategoryRepository()
            runBlocking {
                for (widgetId in appWidgetIds) {
                    categoryRepo.removeAllForWidget(widgetId)
                    configRepo.deleteByWidgetId(widgetId)
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "onDeleted cleanup failed", e)
        }
    }

    override fun onEnabled(context: Context) {
        Log.d(TAG, "onEnabled")
    }

    override fun onDisabled(context: Context) {
        Log.d(TAG, "onDisabled")
    }

    companion object {
        private const val TAG = "GottaDoWidget"
    }
}
