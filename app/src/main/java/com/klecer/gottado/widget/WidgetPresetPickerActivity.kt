package com.klecer.gottado.widget

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import com.klecer.gottado.data.db.entity.WidgetInstanceEntity
import kotlinx.coroutines.runBlocking

class WidgetPresetPickerActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appWidgetId = intent.getIntExtra(WidgetIntents.EXTRA_WIDGET_ID, -1)
        if (appWidgetId == -1) {
            finish()
            return
        }

        try {
            showPicker(appWidgetId)
        } catch (e: Throwable) {
            Log.e("GottaDoWidget", "Preset picker error", e)
            finish()
        }
    }

    private fun showPicker(appWidgetId: Int) {
        val entryPoint = applicationContext.widgetEntryPoint()
        val configRepo = entryPoint.getWidgetConfigRepository()
        val instanceDao = entryPoint.getWidgetInstanceDao()

        val allPresets = runBlocking { configRepo.getAll() }
        val currentPresetId = runBlocking { instanceDao.getPresetId(appWidgetId) }

        if (allPresets.isEmpty()) {
            finish()
            return
        }

        val names = allPresets.map { preset ->
            val label = preset.title?.takeIf { it.isNotBlank() } ?: "Widget ${preset.widgetId}"
            if (preset.widgetId == currentPresetId) "$label  ✓" else label
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Select widget")
            .setItems(names) { _, which ->
                val selected = allPresets[which]
                runBlocking {
                    instanceDao.upsert(WidgetInstanceEntity(appWidgetId, selected.widgetId))
                }
                WidgetUpdateHelper.update(applicationContext, appWidgetId)
                finish()
            }
            .setOnCancelListener { finish() }
            .show()
    }
}
