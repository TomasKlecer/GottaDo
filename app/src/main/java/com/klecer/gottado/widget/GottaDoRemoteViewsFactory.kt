package com.klecer.gottado.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import android.widget.RemoteViews
import com.klecer.gottado.R
import com.klecer.gottado.domain.model.WidgetState
import kotlinx.coroutines.runBlocking

class GottaDoRemoteViewsFactory(
    private val context: Context,
    private val intent: Intent
) : android.widget.RemoteViewsService.RemoteViewsFactory {

    private var state: WidgetState? = null
    private var items: List<WidgetListItem> = emptyList()

    private val widgetId: Int
        get() = intent.getIntExtra(EXTRA_WIDGET_ID, -1)

    override fun onCreate() {
        Log.d(TAG, "onCreate() widgetId=$widgetId")
        loadState()
    }

    override fun onDataSetChanged() {
        Log.d(TAG, "onDataSetChanged() widgetId=$widgetId")
        loadState()
    }

    private fun loadState() {
        try {
            val id = widgetId
            if (id == -1) {
                Log.w(TAG, "loadState: widgetId == -1")
                state = null
                items = emptyList()
                return
            }
            val entryPoint = context.widgetEntryPoint()
            state = runBlocking {
                entryPoint.getWidgetStateUseCase().invoke(id)
            }
            items = state?.let { flatten(it) } ?: emptyList()
            Log.d(TAG, "loadState: items.size=${items.size}, state=${if (state != null) "loaded" else "null"}")
        } catch (e: Throwable) {
            Log.e(TAG, "loadState() crashed", e)
            state = null
            items = emptyList()
        }
    }

    private fun flatten(state: WidgetState): List<WidgetListItem> {
        val list = mutableListOf<WidgetListItem>()
        val pendingUndo = WidgetUndoHelper.getPendingUndo(context)
        if (pendingUndo != null && pendingUndo.widgetId == widgetId) {
            list.add(WidgetListItem.UndoDelete(pendingUndo.trashId))
        }
        if (state.categoryBlocks.isEmpty()) {
            list.add(WidgetListItem.HintText(context.getString(R.string.widget_hint_no_categories)))
            return list
        }
        for (block in state.categoryBlocks) {
            list.add(WidgetListItem.CategoryRow(block))
            for (task in block.tasks) {
                list.add(WidgetListItem.RecordRow(task, block.showCheckboxInsteadOfBullet, block.showCalendarIcon, block.showDeleteButton))
            }
        }
        if (state.config.buttonsAtBottom) {
            list.add(WidgetListItem.Footer)
        }
        return list
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        try {
            if (position !in items.indices) {
                return RemoteViews(context.packageName, R.layout.widget_item_footer)
            }
            val config = state?.config
            val id = widgetId
            return when (val item = items[position]) {
                is WidgetListItem.CategoryRow -> {
                    if (config != null) buildCategoryRow(config, item.block, id)
                    else RemoteViews(context.packageName, R.layout.widget_item_footer)
                }
                is WidgetListItem.RecordRow -> {
                    if (config != null) buildRecordRow(config, item.task, item.showCheckbox, id, item.showCalendarIcon, item.showDeleteButton)
                    else RemoteViews(context.packageName, R.layout.widget_item_footer)
                }
                is WidgetListItem.Footer -> buildFooter(id)
                is WidgetListItem.UndoDelete -> buildUndoRow(item.trashId, id)
                is WidgetListItem.HintText -> buildHintRow(item.message)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "getViewAt($position) crashed", e)
            return RemoteViews(context.packageName, R.layout.widget_item_footer)
        }
    }

    private fun buildCategoryRow(
        config: com.klecer.gottado.data.db.entity.WidgetConfigEntity,
        block: com.klecer.gottado.domain.model.CategoryBlock,
        widgetId: Int
    ): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.widget_item_category)
        rv.setTextViewText(R.id.widget_category_name, block.name)
        rv.setFloat(R.id.widget_category_name, "setTextSize", config.categoryFontSizeSp)
        val catColor = if (block.color != 0) block.color else config.defaultTextColor
        rv.setTextColor(R.id.widget_category_name, catColor)
        rv.setTextColor(R.id.widget_category_edit, catColor)

        val addFillIn = Intent().apply {
            putExtra(WidgetIntents.EXTRA_WIDGET_ID, widgetId)
            putExtra(WidgetIntents.EXTRA_CATEGORY_ID, block.categoryId)
            putExtra("action", WidgetIntents.ACTION_ADD_TASK)
        }
        rv.setOnClickFillInIntent(R.id.widget_category_name, addFillIn)

        val settingsFillIn = Intent().apply {
            putExtra(WidgetIntents.EXTRA_WIDGET_ID, widgetId)
            putExtra(WidgetIntents.EXTRA_CATEGORY_ID, block.categoryId)
            putExtra("action", WidgetIntents.ACTION_OPEN_CATEGORY_SETTINGS)
        }
        rv.setOnClickFillInIntent(R.id.widget_category_edit, settingsFillIn)
        return rv
    }

    private fun buildRecordRow(
        config: com.klecer.gottado.data.db.entity.WidgetConfigEntity,
        task: com.klecer.gottado.domain.model.TaskItem,
        showCheckbox: Boolean,
        widgetId: Int,
        showCalendarIcon: Boolean = false,
        showDeleteButton: Boolean = false
    ): RemoteViews {
        val layoutId = if (showCheckbox) R.layout.widget_item_record_checkbox else R.layout.widget_item_record
        val rv = RemoteViews(context.packageName, layoutId)
        rv.setTextViewText(R.id.widget_record_text, stripHtmlForWidget(task.contentHtml))
        rv.setFloat(R.id.widget_record_text, "setTextSize", config.recordFontSizeSp)
        val baseTextColor = if (task.textColor != 0) task.textColor else config.defaultTextColor
        val textColor = if (task.completed) Color.GRAY else baseTextColor
        rv.setTextColor(R.id.widget_record_text, textColor)
        if (task.completed) {
            rv.setInt(
                R.id.widget_record_text, "setPaintFlags",
                android.graphics.Paint.STRIKE_THRU_TEXT_FLAG or android.graphics.Paint.ANTI_ALIAS_FLAG
            )
        } else {
            rv.setInt(
                R.id.widget_record_text, "setPaintFlags",
                android.graphics.Paint.ANTI_ALIAS_FLAG
            )
        }

        if (task.scheduledTimeMillis != null) {
            rv.setViewVisibility(R.id.widget_record_time, android.view.View.VISIBLE)
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = task.scheduledTimeMillis }
            rv.setTextViewText(
                R.id.widget_record_time,
                "%02d:%02d".format(cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE))
            )
        } else {
            rv.setViewVisibility(R.id.widget_record_time, android.view.View.GONE)
        }

        val bulletPx = (config.bulletSizeDp * context.resources.displayMetrics.density).toInt().coerceAtLeast(4)
        val checkboxPx = (config.checkboxSizeDp * context.resources.displayMetrics.density).toInt().coerceAtLeast(4)

        if (showCheckbox) {
            rv.setImageViewBitmap(
                R.id.widget_record_checkbox,
                createCheckboxBitmap(checkboxPx, task.bulletColor, task.completed)
            )

            val toggleFillIn = Intent().apply {
                putExtra(WidgetIntents.EXTRA_WIDGET_ID, widgetId)
                putExtra(WidgetIntents.EXTRA_TASK_ID, task.id)
                putExtra("action", WidgetIntents.ACTION_TOGGLE_TASK_COMPLETED)
            }
            rv.setOnClickFillInIntent(R.id.widget_record_checkbox, toggleFillIn)
        } else {
            rv.setImageViewBitmap(R.id.widget_record_bullet, createCircleBitmap(bulletPx, task.bulletColor))

            val toggleFillIn = Intent().apply {
                putExtra(WidgetIntents.EXTRA_WIDGET_ID, widgetId)
                putExtra(WidgetIntents.EXTRA_TASK_ID, task.id)
                putExtra("action", WidgetIntents.ACTION_TOGGLE_TASK_COMPLETED)
            }
            rv.setOnClickFillInIntent(R.id.widget_record_bullet, toggleFillIn)
        }

        if (task.fromCalendarSync && showCalendarIcon) {
            rv.setViewVisibility(R.id.widget_record_calendar_icon, android.view.View.VISIBLE)
        } else {
            rv.setViewVisibility(R.id.widget_record_calendar_icon, android.view.View.GONE)
        }

        if (showDeleteButton) {
            rv.setViewVisibility(R.id.widget_record_delete, android.view.View.VISIBLE)
            val deleteFillIn = Intent().apply {
                putExtra(WidgetIntents.EXTRA_WIDGET_ID, widgetId)
                putExtra(WidgetIntents.EXTRA_TASK_ID, task.id)
                putExtra("action", WidgetIntents.ACTION_DELETE_TASK)
            }
            rv.setOnClickFillInIntent(R.id.widget_record_delete, deleteFillIn)
        } else {
            rv.setViewVisibility(R.id.widget_record_delete, android.view.View.GONE)
        }

        val editFillIn = Intent().apply {
            putExtra(WidgetIntents.EXTRA_WIDGET_ID, widgetId)
            putExtra(WidgetIntents.EXTRA_TASK_ID, task.id)
            putExtra("action", WidgetIntents.ACTION_EDIT_TASK)
        }
        rv.setOnClickFillInIntent(R.id.widget_record_text_area, editFillIn)
        return rv
    }

    private fun buildHintRow(message: String): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.widget_item_undo)
        val textColor = state?.config?.defaultTextColor ?: Color.WHITE
        rv.setTextColor(R.id.widget_undo_btn, textColor)
        rv.setTextViewText(R.id.widget_undo_btn, message)
        return rv
    }

    private fun buildUndoRow(trashId: Long, widgetId: Int): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.widget_item_undo)
        rv.setTextViewText(R.id.widget_undo_btn, context.getString(R.string.widget_undo_delete))
        rv.setTextColor(R.id.widget_undo_btn, 0xFF4CAF50.toInt())
        val fillIn = Intent().apply {
            putExtra(WidgetIntents.EXTRA_WIDGET_ID, widgetId)
            putExtra(WidgetIntents.EXTRA_TRASH_ID, trashId)
            putExtra("action", WidgetIntents.ACTION_UNDO_DELETE)
        }
        rv.setOnClickFillInIntent(R.id.widget_undo_btn, fillIn)
        return rv
    }

    private fun buildFooter(widgetId: Int): RemoteViews {
        val rv = RemoteViews(context.packageName, R.layout.widget_item_footer)
        val textColor = state?.config?.defaultTextColor ?: Color.WHITE
        rv.setTextColor(R.id.widget_footer_open_app, textColor)
        rv.setTextColor(R.id.widget_footer_reorder, textColor)
        val openFillIn = Intent().apply {
            putExtra("action", "OPEN_APP")
        }
        rv.setOnClickFillInIntent(R.id.widget_footer_open_app, openFillIn)
        val reorderFillIn = Intent().apply {
            putExtra(WidgetIntents.EXTRA_WIDGET_ID, widgetId)
            putExtra("action", WidgetIntents.ACTION_OPEN_REORDER)
        }
        rv.setOnClickFillInIntent(R.id.widget_footer_reorder, reorderFillIn)
        return rv
    }

    private fun createCircleBitmap(sizePx: Int, color: Int): Bitmap {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        val r = sizePx / 2f
        canvas.drawCircle(r, r, r, paint)
        return bmp
    }

    private fun createCheckboxBitmap(sizePx: Int, color: Int, checked: Boolean): Bitmap {
        val s = sizePx.coerceAtLeast(16)
        val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val stroke = (s * 0.1f).coerceAtLeast(2f)
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = if (checked) Paint.Style.FILL_AND_STROKE else Paint.Style.STROKE
            strokeWidth = stroke
        }
        val inset = stroke / 2f
        canvas.drawRoundRect(inset, inset, s - inset, s - inset, stroke * 2, stroke * 2, borderPaint)
        if (checked) {
            val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = stroke * 1.5f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
            val path = android.graphics.Path().apply {
                moveTo(s * 0.22f, s * 0.50f)
                lineTo(s * 0.42f, s * 0.72f)
                lineTo(s * 0.78f, s * 0.28f)
            }
            canvas.drawPath(path, checkPaint)
        }
        return bmp
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 5

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
    }

    companion object {
        private const val TAG = "GottaDoWidget"
        const val EXTRA_WIDGET_ID = "appWidgetId"
    }
}
