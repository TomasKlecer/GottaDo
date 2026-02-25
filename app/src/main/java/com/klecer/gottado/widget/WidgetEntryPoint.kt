package com.klecer.gottado.widget

import android.content.Context
import android.graphics.Color
import com.klecer.gottado.data.db.entity.ReorderHandlePosition
import com.klecer.gottado.data.db.entity.WidgetConfigEntity
import com.klecer.gottado.domain.repository.WidgetConfigRepository
import com.klecer.gottado.domain.usecase.GetWidgetStateUseCase
import com.klecer.gottado.domain.usecase.RestoreFromTrashUseCase
import com.klecer.gottado.domain.usecase.ToggleTaskCompletedUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Entry point for widget process to obtain use cases / repositories (no Activity/Fragment).
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun getWidgetStateUseCase(): GetWidgetStateUseCase
    fun getWidgetConfigRepository(): WidgetConfigRepository
    fun getWidgetCategoryRepository(): com.klecer.gottado.domain.repository.WidgetCategoryRepository
    fun getToggleTaskCompletedUseCase(): ToggleTaskCompletedUseCase
    fun getRestoreFromTrashUseCase(): RestoreFromTrashUseCase
}

fun Context.widgetEntryPoint(): WidgetEntryPoint =
    EntryPointAccessors.fromApplication(applicationContext, WidgetEntryPoint::class.java)

/** Default config when user adds a new widget instance. */
fun defaultWidgetConfig(widgetId: Int): WidgetConfigEntity = WidgetConfigEntity(
    widgetId = widgetId,
    title = null,
    subtitle = null,
    note = null,
    backgroundColor = Color.parseColor("#80000000"),
    backgroundAlpha = 0.9f,
    categoryFontSizeSp = 18f,
    recordFontSizeSp = 14f,
    defaultTextColor = Color.WHITE,
    reorderHandlePosition = ReorderHandlePosition.NONE,
    titleColor = Color.WHITE,
    subtitleColor = Color.WHITE,
    bulletSizeDp = 16,
    checkboxSizeDp = 16,
    showTitleOnWidget = true
)
