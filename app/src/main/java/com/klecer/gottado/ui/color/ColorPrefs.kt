package com.klecer.gottado.ui.color

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ColorPrefs @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("color_palettes", Context.MODE_PRIVATE)

    companion object {
        val DEFAULT_CATEGORY = listOf(
            0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFFE53935.toInt(), 0xFFFB8C00.toInt(),
            0xFFFDD835.toInt(), 0xFF43A047.toInt(), 0xFF1E88E5.toInt(), 0xFF8E24AA.toInt()
        )
        val DEFAULT_ENTRY = listOf(
            0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFFE53935.toInt(), 0xFFFB8C00.toInt(),
            0xFFFDD835.toInt(), 0xFF43A047.toInt(), 0xFF1E88E5.toInt(), 0xFF8E24AA.toInt()
        )
        val DEFAULT_WIDGET_BG = listOf(
            0xFF000000.toInt(), 0xFF333333.toInt(), 0xFF666666.toInt(), 0xFF999999.toInt(),
            0xFFFFFFFF.toInt(), 0xFF1A237E.toInt(), 0xFF004D40.toInt(), 0xFF3E2723.toInt()
        )
        val DEFAULT_WIDGET_TEXT = listOf(
            0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFF666666.toInt(),
            0xFFE53935.toInt(), 0xFFFB8C00.toInt(), 0xFFFDD835.toInt(),
            0xFF43A047.toInt(), 0xFF1E88E5.toInt(), 0xFF8E24AA.toInt()
        )

        const val KEY_CATEGORY = "category"
        const val KEY_ENTRY = "entry"
        const val KEY_WIDGET_BG = "widget_bg"
        const val KEY_WIDGET_TEXT = "widget_text"
    }

    fun getPalette(key: String): List<Int> {
        val stored = prefs.getString(key, null) ?: return getDefault(key)
        return stored.split(",").mapNotNull { it.trim().toLongOrNull()?.toInt() }
            .takeIf { it.size == getDefault(key).size } ?: getDefault(key)
    }

    fun setPalette(key: String, colors: List<Int>) {
        prefs.edit().putString(key, colors.joinToString(",") { it.toLong().and(0xFFFFFFFFL).toString() }).apply()
    }

    fun setColor(key: String, index: Int, color: Int) {
        val palette = getPalette(key).toMutableList()
        if (index in palette.indices) {
            palette[index] = color
            setPalette(key, palette)
        }
    }

    private fun getDefault(key: String): List<Int> = when (key) {
        KEY_CATEGORY -> DEFAULT_CATEGORY
        KEY_ENTRY -> DEFAULT_ENTRY
        KEY_WIDGET_BG -> DEFAULT_WIDGET_BG
        KEY_WIDGET_TEXT -> DEFAULT_WIDGET_TEXT
        else -> DEFAULT_CATEGORY
    }
}
