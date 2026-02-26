package com.klecer.gottado.ui.navigation

object NavRoutes {
    const val WIDGET_LIST = "widget_list"
    const val WIDGET_SETTINGS = "widget_settings/{widgetId}"
    const val CATEGORY_LIST = "category_list"
    const val CATEGORY_SETTINGS = "category_settings/{categoryId}"
    const val RECORD_EDIT_OPTIONS = "record_edit_options"
    const val ROUTINE_LIST = "routine_list/{categoryId}"
    const val ROUTINE_EDIT = "routine_edit/{categoryId}/{routineId}"
    const val TRASH = "trash"
    const val SETTINGS = "settings"

    fun widgetSettings(widgetId: Int) = "widget_settings/$widgetId"
    fun categorySettings(categoryId: Long) = "category_settings/$categoryId"
    fun routineList(categoryId: Long) = "routine_list/$categoryId"
    fun routineEdit(categoryId: Long, routineId: Long = 0) = "routine_edit/$categoryId/$routineId"
}
