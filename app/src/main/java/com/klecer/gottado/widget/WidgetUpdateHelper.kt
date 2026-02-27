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
import com.klecer.gottado.data.db.entity.WidgetInstanceEntity
import kotlinx.coroutines.runBlocking

object WidgetUpdateHelper {

    private const val TAG = "GottaDoWidget"

    fun update(context: Context, appWidgetId: Int) {
        Log.d(TAG, "update() called for appWidgetId=$appWidgetId")
        try {
            updateInternal(context, appWidgetId)
        } catch (e: Throwable) {
            Log.e(TAG, "update() failed for appWidgetId=$appWidgetId", e)
            showError(context, appWidgetId, "Widget error: ${e.message}")
        }
    }

    private fun updateInternal(context: Context, appWidgetId: Int) {
        val mgr = AppWidgetManager.getInstance(context)
        val entryPoint = context.widgetEntryPoint()
        val repo = entryPoint.getWidgetConfigRepository()
        val instanceDao = entryPoint.getWidgetInstanceDao()

        val config = runBlocking {
            val presetId = instanceDao.getPresetId(appWidgetId)
            if (presetId != null) {
                val cfg = repo.getByWidgetId(presetId)
                if (cfg != null) return@runBlocking cfg
            }
            val allPresets = repo.getAll()
            val preset = allPresets.firstOrNull()
            if (preset != null) {
                instanceDao.upsert(WidgetInstanceEntity(appWidgetId, preset.widgetId))
                return@runBlocking preset
            }
            val default = defaultWidgetConfig(appWidgetId)
            repo.insert(default)
            instanceDao.upsert(WidgetInstanceEntity(appWidgetId, default.widgetId))
            default
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
                val pi = WidgetIntents.openReorderPendingIntent(context, appWidgetId, appWidgetId * 100 + 1)
                rv.setOnClickPendingIntent(R.id.widget_edge_left, pi)
            }
            ReorderHandlePosition.RIGHT -> {
                rv.setViewVisibility(R.id.widget_edge_left, View.GONE)
                rv.setViewVisibility(R.id.widget_edge_right, View.VISIBLE)
                rv.setInt(R.id.widget_edge_right, "setBackgroundColor", config.defaultTextColor)
                val pi = WidgetIntents.openReorderPendingIntent(context, appWidgetId, appWidgetId * 100 + 2)
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
            rv.setTextColor(R.id.widget_btn_reorder, config.defaultTextColor)
            val openAppPi = WidgetIntents.openAppPendingIntent(context, config.widgetId, appWidgetId * 100 + 3)
            rv.setOnClickPendingIntent(R.id.widget_btn_open_app, openAppPi)
            val pickerPi = WidgetIntents.openPresetPickerPendingIntent(context, appWidgetId, appWidgetId * 100 + 5)
            rv.setOnClickPendingIntent(R.id.widget_btn_picker, pickerPi)
            val reorderPi = WidgetIntents.openReorderPendingIntent(context, appWidgetId, appWidgetId * 100 + 4)
            rv.setOnClickPendingIntent(R.id.widget_btn_reorder, reorderPi)
        }

        val serviceIntent = Intent(context, GottaDoRemoteViewsService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            putExtra(GottaDoRemoteViewsFactory.EXTRA_WIDGET_ID, appWidgetId)
            data = Uri.parse("gottado://widget/$appWidgetId")
        }
        rv.setRemoteAdapter(R.id.widget_list, serviceIntent)

        val clickTemplate = WidgetIntents.listClickTemplate(context)
        rv.setPendingIntentTemplate(R.id.widget_list, clickTemplate)

        mgr.updateAppWidget(appWidgetId, rv)
        mgr.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list)

        Log.d(TAG, "update() succeeded for appWidgetId=$appWidgetId presetId=${config.widgetId}")
    }

    fun updateAllForPreset(context: Context, presetId: Int) {
        try {
            val instanceDao = context.widgetEntryPoint().getWidgetInstanceDao()
            val instances = runBlocking { instanceDao.getAll() }
            for (inst in instances) {
                if (inst.presetId == presetId) {
                    update(context, inst.appWidgetId)
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "updateAllForPreset($presetId) failed", e)
        }
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
