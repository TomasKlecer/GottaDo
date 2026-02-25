package com.klecer.gottado.domain.model

/**
 * Global options for which fields appear in the record edit screen.
 * Stored in app preferences; RecordEditActivity reads and shows/hides sections accordingly.
 */
data class RecordEditOptions(
    val showRichText: Boolean = true,
    val showTimeField: Boolean = true,
    val showCategoryDropdown: Boolean = false,
    val showBulletColor: Boolean = true,
    val useUnifiedColorPicker: Boolean = true
)
