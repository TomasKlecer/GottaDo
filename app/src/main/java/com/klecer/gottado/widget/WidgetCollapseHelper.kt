package com.klecer.gottado.widget

import android.content.Context

object WidgetCollapseHelper {

    private const val PREFS_NAME = "widget_collapse_state"

    private fun key(widgetId: Int, categoryId: Long) = "${widgetId}_${categoryId}"

    fun isCollapsed(context: Context, widgetId: Int, categoryId: Long): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(key(widgetId, categoryId), false)
    }

    fun toggle(context: Context, widgetId: Int, categoryId: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val k = key(widgetId, categoryId)
        val current = prefs.getBoolean(k, false)
        prefs.edit().putBoolean(k, !current).apply()
    }
}
