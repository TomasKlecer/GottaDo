package com.klecer.gottado.widget

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.runBlocking

class WidgetTrampolineActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            handleAction()
        } catch (e: Throwable) {
            Log.e("GottaDoWidget", "Trampoline error", e)
        }
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    private fun handleAction() {
        val action = intent.getStringExtra("action") ?: return
        val widgetId = intent.getIntExtra(WidgetIntents.EXTRA_WIDGET_ID, -1)
        Log.d("GottaDoWidget", "Trampoline: action=$action widgetId=$widgetId")

        when (action) {
            WidgetIntents.ACTION_EDIT_TASK -> {
                val taskId = intent.getLongExtra(WidgetIntents.EXTRA_TASK_ID, -1L)
                if (taskId != -1L) {
                    startActivity(Intent(this, com.klecer.gottado.record.RecordEditActivity::class.java).apply {
                        this.action = WidgetIntents.ACTION_EDIT_TASK
                        putExtra(WidgetIntents.EXTRA_WIDGET_ID, widgetId)
                        putExtra(WidgetIntents.EXTRA_TASK_ID, taskId)
                    })
                }
            }
            WidgetIntents.ACTION_ADD_TASK -> {
                val categoryId = intent.getLongExtra(WidgetIntents.EXTRA_CATEGORY_ID, -1L)
                if (categoryId != -1L) {
                    startActivity(Intent(this, com.klecer.gottado.record.RecordEditActivity::class.java).apply {
                        this.action = WidgetIntents.ACTION_ADD_TASK
                        putExtra(WidgetIntents.EXTRA_WIDGET_ID, widgetId)
                        putExtra(WidgetIntents.EXTRA_CATEGORY_ID, categoryId)
                    })
                }
            }
            WidgetIntents.ACTION_TOGGLE_TASK_COMPLETED -> {
                val taskId = intent.getLongExtra(WidgetIntents.EXTRA_TASK_ID, -1L)
                if (widgetId != -1 && taskId != -1L) {
                    runBlocking {
                        applicationContext.widgetEntryPoint().getToggleTaskCompletedUseCase().invoke(taskId)
                    }
                    WidgetUpdateHelper.update(applicationContext, widgetId)
                }
            }
            WidgetIntents.ACTION_OPEN_REORDER -> {
                if (widgetId != -1) {
                    startActivity(Intent(this, ReorderOverlayActivity::class.java).apply {
                        this.action = WidgetIntents.ACTION_OPEN_REORDER
                        putExtra(WidgetIntents.EXTRA_WIDGET_ID, widgetId)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                    })
                }
            }
            WidgetIntents.ACTION_OPEN_CATEGORY_SETTINGS -> {
                val categoryId = intent.getLongExtra(WidgetIntents.EXTRA_CATEGORY_ID, 0L)
                val route = if (categoryId > 0) "category_settings/$categoryId" else "category_list"
                startActivity(Intent(this, com.klecer.gottado.MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("navigate_to", route)
                })
            }
            WidgetIntents.ACTION_DELETE_TASK -> {
                val taskId = intent.getLongExtra(WidgetIntents.EXTRA_TASK_ID, -1L)
                if (widgetId != -1 && taskId != -1L) {
                    val trashId = runBlocking {
                        applicationContext.widgetEntryPoint().getDeleteTaskUseCase().invoke(taskId)
                    }
                    if (trashId != null) {
                        WidgetUndoHelper.storePendingUndo(applicationContext, trashId, widgetId)
                    }
                    WidgetUpdateHelper.update(applicationContext, widgetId)
                }
            }
            WidgetIntents.ACTION_UNDO_DELETE -> {
                val trashId = intent.getLongExtra(WidgetIntents.EXTRA_TRASH_ID, -1L)
                if (trashId != -1L && widgetId != -1) {
                    runBlocking {
                        applicationContext.widgetEntryPoint().getRestoreFromTrashUseCase().invoke(trashId)
                    }
                    WidgetUndoHelper.clearPendingUndo(applicationContext)
                    WidgetUpdateHelper.update(applicationContext, widgetId)
                }
            }
            WidgetIntents.ACTION_PICK_PRESET -> {
                if (widgetId != -1) {
                    startActivity(Intent(this, WidgetPresetPickerActivity::class.java).apply {
                        putExtra(WidgetIntents.EXTRA_WIDGET_ID, widgetId)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                    })
                }
            }
            WidgetIntents.ACTION_TOGGLE_COLLAPSE -> {
                val categoryId = intent.getLongExtra(WidgetIntents.EXTRA_CATEGORY_ID, -1L)
                if (widgetId != -1 && categoryId != -1L) {
                    WidgetCollapseHelper.toggle(applicationContext, widgetId, categoryId)
                    WidgetUpdateHelper.update(applicationContext, widgetId)
                }
            }
            "OPEN_APP" -> {
                if (widgetId != -1) {
                    val presetId = runBlocking {
                        applicationContext.widgetEntryPoint().getWidgetInstanceDao().getPresetId(widgetId) ?: widgetId
                    }
                    startActivity(Intent(this, com.klecer.gottado.MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("navigate_to", "widget_settings/$presetId")
                    })
                } else {
                    startActivity(Intent(this, com.klecer.gottado.MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    })
                }
            }
        }
    }
}
