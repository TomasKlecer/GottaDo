package com.klecer.gottado.data.repository

import com.klecer.gottado.domain.model.RecordEditOptions
import com.klecer.gottado.domain.repository.RecordEditOptionsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named

class RecordEditOptionsRepositoryImpl @Inject constructor(
    @Named("record_edit_options") private val prefs: android.content.SharedPreferences
) : RecordEditOptionsRepository {

    override fun get(): RecordEditOptions = RecordEditOptions(
        showRichText = prefs.getBoolean(KEY_RICH_TEXT, true),
        showTimeField = prefs.getBoolean(KEY_TIME, true),
        showCategoryDropdown = prefs.getBoolean(KEY_CATEGORY, false),
        showBulletColor = prefs.getBoolean(KEY_BULLET_COLOR, true),
        showTextColor = prefs.getBoolean(KEY_TEXT_COLOR, true),
        useUnifiedColorPicker = prefs.getBoolean(KEY_UNIFIED_COLOR_PICKER, false),
        showCompletedCheckbox = prefs.getBoolean(KEY_COMPLETED_CHECKBOX, false)
    )

    override suspend fun set(options: RecordEditOptions) = withContext(Dispatchers.IO) {
        prefs.edit()
            .putBoolean(KEY_RICH_TEXT, options.showRichText)
            .putBoolean(KEY_TIME, options.showTimeField)
            .putBoolean(KEY_CATEGORY, options.showCategoryDropdown)
            .putBoolean(KEY_BULLET_COLOR, options.showBulletColor)
            .putBoolean(KEY_TEXT_COLOR, options.showTextColor)
            .putBoolean(KEY_UNIFIED_COLOR_PICKER, options.useUnifiedColorPicker)
            .putBoolean(KEY_COMPLETED_CHECKBOX, options.showCompletedCheckbox)
            .apply()
    }

    companion object {
        private const val PREF_NAME = "record_edit_options"
        private const val KEY_RICH_TEXT = "show_rich_text"
        private const val KEY_TIME = "show_time_field"
        private const val KEY_CATEGORY = "show_category_dropdown"
        private const val KEY_BULLET_COLOR = "show_bullet_color"
        private const val KEY_TEXT_COLOR = "show_text_color"
        private const val KEY_UNIFIED_COLOR_PICKER = "use_unified_color_picker"
        private const val KEY_COMPLETED_CHECKBOX = "show_completed_checkbox"
    }
}
