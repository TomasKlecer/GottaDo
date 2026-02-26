package com.klecer.gottado.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.klecer.gottado.R
import com.klecer.gottado.data.db.entity.ReorderHandlePosition
import kotlinx.coroutines.runBlocking

object WidgetUpdateHelper {

    private const val TAG = "GottaDoWidget"

    fun update(context: Context, widgetId: Int) {
        Log.d(TAG, "update() called for widgetId=$widgetId")
        try {
            updateInternal(context, widgetId)
        } catch (e: Throwable) {
            Log.e(TAG, "update() failed for widgetId=$widgetId", e)
            showError(context, widgetId, "Widget error: ${e.message}")
        }
    }

    private fun updateInternal(context: Context, widgetId: Int) {
        val mgr = AppWidgetManager.getInstance(context)
        val entryPoint = context.widgetEntryPoint()
        val repo = entryPoint.getWidgetConfigRepository()

        val config = runBlocking {
            val existing = repo.getByWidgetId(widgetId)
            if (existing != null) {
                existing
            } else {
                val default = defaultWidgetConfig(widgetId)
                repo.insert(default)
                default
            }
        }

        val rv = RemoteViews(context.packageName, R.layout.widget_gotta_do)

        rv.setViewVisibility(R.id.widget_content, View.VISIBLE)
        rv.setViewVisibility(R.id.widget_error_text, View.GONE)

        val bgColor = adjustAlpha(config.backgroundColor, config.backgroundAlpha)
        rv.setInt(R.id.widget_root, "setBackgroundColor", bgColor)

        val title = config.title?.takeIf { it.isNotBlank() }
        if (title != null && config.showTitleOnWidget) {
            rv.setViewVisibility(R.id.widget_title, View.VISIBLE)
            rv.setTextViewText(R.id.widget_title, title)
            rv.setTextColor(R.id.widget_title, config.titleColor)
        } else {
            rv.setViewVisibility(R.id.widget_title, View.GONE)
        }

        val subtitle = config.subtitle?.takeIf { it.isNotBlank() }
        if (subtitle != null) {
            rv.setViewVisibility(R.id.widget_subtitle, View.VISIBLE)
            rv.setTextViewText(R.id.widget_subtitle, subtitle)
            rv.setTextColor(R.id.widget_subtitle, config.subtitleColor)
        } else {
            rv.setViewVisibility(R.id.widget_subtitle, View.GONE)
        }

        when (config.reorderHandlePosition) {
            ReorderHandlePosition.LEFT -> {
                rv.setViewVisibility(R.id.widget_edge_left, View.VISIBLE)
                rv.setViewVisibility(R.id.widget_edge_right, View.GONE)
                rv.setInt(R.id.widget_edge_left, "setBackgroundColor", config.defaultTextColor)
                val pi = WidgetIntents.openReorderPendingIntent(context, widgetId, widgetId * 100 + 1)
                rv.setOnClickPendingIntent(R.id.widget_edge_left, pi)
            }
            ReorderHandlePosition.RIGHT -> {
                rv.setViewVisibility(R.id.widget_edge_left, View.GONE)
                rv.setViewVisibility(R.id.widget_edge_right, View.VISIBLE)
                rv.setInt(R.id.widget_edge_right, "setBackgroundColor", config.defaultTextColor)
                val pi = WidgetIntents.openReorderPendingIntent(context, widgetId, widgetId * 100 + 2)
                rv.setOnClickPendingIntent(R.id.widget_edge_right, pi)
            }
            ReorderHandlePosition.NONE -> {
                rv.setViewVisibility(R.id.widget_edge_left, View.GONE)
                rv.setViewVisibility(R.id.widget_edge_right, View.GONE)
            }
        }

        if (config.buttonsAtBottom) {
            rv.setViewVisibility(R.id.widget_footer_bar, View.GONE)
        } else {
            rv.setViewVisibility(R.id.widget_footer_bar, View.VISIBLE)
            rv.setTextColor(R.id.widget_btn_open_app, config.defaultTextColor)
            rv.setTextColor(R.id.widget_btn_reorder, config.defaultTextColor)
            val openAppPi = WidgetIntents.openAppPendingIntent(context, widgetId, widgetId * 100 + 3)
            rv.setOnClickPendingIntent(R.id.widget_btn_open_app, openAppPi)
            val reorderPi = WidgetIntents.openReorderPendingIntent(context, widgetId, widgetId * 100 + 4)
            rv.setOnClickPendingIntent(R.id.widget_btn_reorder, reorderPi)
        }

        val serviceIntent = Intent(context, GottaDoRemoteViewsService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            putExtra(GottaDoRemoteViewsFactory.EXTRA_WIDGET_ID, widgetId)
            data = Uri.parse("gottado://widget/$widgetId")
        }
        rv.setRemoteAdapter(R.id.widget_list, serviceIntent)

        val clickTemplate = WidgetIntents.listClickTemplate(context)
        rv.setPendingIntentTemplate(R.id.widget_list, clickTemplate)

        mgr.updateAppWidget(widgetId, rv)
        mgr.notifyAppWidgetViewDataChanged(widgetId, R.id.widget_list)

        Log.d(TAG, "update() succeeded for widgetId=$widgetId")
    }

    fun showError(context: Context, widgetId: Int, message: String) {
        try {
            val mgr = AppWidgetManager.getInstance(context)
            val rv = RemoteViews(context.packageName, R.layout.widget_gotta_do)
            rv.setViewVisibility(R.id.widget_content, View.GONE)
            rv.setViewVisibility(R.id.widget_error_text, View.VISIBLE)
            rv.setTextViewText(R.id.widget_error_text, message)
            mgr.updateAppWidget(widgetId, rv)
        } catch (inner: Throwable) {
            Log.e(TAG, "showError() also failed", inner)
        }
    }

    private fun adjustAlpha(color: Int, alpha: Float): Int {
        val a = (alpha * 255).toInt().coerceIn(0, 255)
        return (a shl 24) or (color and 0x00FFFFFF)
    }
}
