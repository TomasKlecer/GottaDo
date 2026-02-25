package com.klecer.gottado.widget

import android.annotation.SuppressLint
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.klecer.gottado.R
import com.klecer.gottado.domain.model.WidgetState
import com.klecer.gottado.domain.usecase.GetWidgetStateUseCase
import com.klecer.gottado.domain.usecase.MoveTaskToCategoryUseCase
import com.klecer.gottado.domain.usecase.ReorderTasksUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

private sealed class ReorderRow {
    data class Category(val categoryId: Long, val name: String) : ReorderRow()
    data class Task(val taskId: Long, val categoryId: Long, val sortOrder: Int, val contentPreview: String, val hasScheduledTime: Boolean = false) : ReorderRow()
}

@AndroidEntryPoint
class ReorderOverlayActivity : ComponentActivity() {

    @Inject
    lateinit var getWidgetStateUseCase: GetWidgetStateUseCase

    @Inject
    lateinit var reorderTasksUseCase: ReorderTasksUseCase

    @Inject
    lateinit var moveTaskToCategoryUseCase: MoveTaskToCategoryUseCase

    private var widgetId: Int = -1
    private lateinit var adapter: ReorderAdapter
    private lateinit var touchHelper: ItemTouchHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reorder_overlay)

        widgetId = intent.getIntExtra(WidgetIntents.EXTRA_WIDGET_ID, -1)
        if (widgetId == -1) {
            finish()
            return
        }

        findViewById<View>(R.id.reorder_root).setOnClickListener { finish() }

        val recycler = findViewById<RecyclerView>(R.id.reorder_list)
        recycler.layoutManager = LinearLayoutManager(this)

        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                if (adapter.getItemViewType(viewHolder.bindingAdapterPosition) == 0) {
                    return makeMovementFlags(0, 0)
                }
                return super.getMovementFlags(recyclerView, viewHolder)
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.bindingAdapterPosition
                val to = target.bindingAdapterPosition
                if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return false
                adapter.swapVisual(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                persistReorder()
            }

            override fun isLongPressDragEnabled() = false
        }
        touchHelper = ItemTouchHelper(callback)
        touchHelper.attachToRecyclerView(recycler)

        adapter = ReorderAdapter(
            onMoveUp = { index -> moveTask(index, -1) },
            onMoveDown = { index -> moveTask(index, 1) },
            onStartDrag = { holder -> touchHelper.startDrag(holder) }
        )
        recycler.adapter = adapter

        applyWidgetBackground()

        lifecycleScope.launch {
            loadState()
        }
    }

    private fun applyWidgetBackground() {
        try {
            val cfg = runBlocking {
                applicationContext.widgetEntryPoint().getWidgetConfigRepository().getByWidgetId(widgetId)
            }
            if (cfg != null) {
                val bgColor = 0xFF000000.toInt() or (cfg.backgroundColor and 0x00FFFFFF)
                val panel = findViewById<View>(R.id.reorder_panel)
                panel.background = ColorDrawable(bgColor)
                val textColor = cfg.defaultTextColor
                findViewById<TextView>(R.id.reorder_title_text).setTextColor(textColor)
                adapter.textColor = textColor
            }
        } catch (_: Throwable) {}
    }

    private fun loadState() {
        lifecycleScope.launch {
            val state = getWidgetStateUseCase(widgetId) ?: run {
                finish()
                return@launch
            }
            adapter.setRows(flatten(state))
        }
    }

    private fun flatten(state: WidgetState): List<ReorderRow> {
        val list = mutableListOf<ReorderRow>()
        for (block in state.categoryBlocks) {
            list.add(ReorderRow.Category(block.categoryId, block.name))
            for (task in block.tasks) {
                list.add(
                    ReorderRow.Task(
                        taskId = task.id,
                        categoryId = block.categoryId,
                        sortOrder = task.sortOrder,
                        contentPreview = stripHtmlForWidget(task.contentHtml),
                        hasScheduledTime = task.scheduledTimeMillis != null
                    )
                )
            }
        }
        return list
    }

    private fun moveTask(fromIndex: Int, direction: Int) {
        val rows = adapter.currentRows
        val from = rows.getOrNull(fromIndex) as? ReorderRow.Task ?: return
        var targetIndex = fromIndex + direction
        if (targetIndex !in rows.indices) return

        var target = rows[targetIndex]

        if (target is ReorderRow.Category && target.categoryId == from.categoryId) {
            targetIndex += direction
            if (targetIndex !in rows.indices) return
            target = rows[targetIndex]
        }

        lifecycleScope.launch {
            when (target) {
                is ReorderRow.Task -> {
                    if (from.categoryId == target.categoryId) {
                        reorderTasksUseCase(from.taskId, target.sortOrder)
                        reorderTasksUseCase(target.taskId, from.sortOrder)
                    } else {
                        val maxSort = rows.filterIsInstance<ReorderRow.Task>()
                            .filter { it.categoryId == target.categoryId }
                            .maxOfOrNull { it.sortOrder } ?: -1
                        moveTaskToCategoryUseCase(from.taskId, target.categoryId, maxSort + 1)
                    }
                }
                is ReorderRow.Category -> {
                    val maxSort = rows.filterIsInstance<ReorderRow.Task>()
                        .filter { it.categoryId == target.categoryId }
                        .maxOfOrNull { it.sortOrder } ?: -1
                    moveTaskToCategoryUseCase(from.taskId, target.categoryId, maxSort + 1)
                }
            }
            WidgetUpdateHelper.update(this@ReorderOverlayActivity, widgetId)
            loadState()
        }
    }

    private fun persistReorder() {
        val rows = adapter.currentRows
        lifecycleScope.launch {
            var lastCatId = 0L
            var order = 0
            for (row in rows) {
                when (row) {
                    is ReorderRow.Category -> {
                        lastCatId = row.categoryId
                        order = 0
                    }
                    is ReorderRow.Task -> {
                        if (row.categoryId != lastCatId) {
                            moveTaskToCategoryUseCase(row.taskId, lastCatId, order)
                        } else {
                            reorderTasksUseCase(row.taskId, order)
                        }
                        order++
                    }
                }
            }
            WidgetUpdateHelper.update(this@ReorderOverlayActivity, widgetId)
            loadState()
        }
    }
}

private class ReorderAdapter(
    private val onMoveUp: (Int) -> Unit,
    private val onMoveDown: (Int) -> Unit,
    private val onStartDrag: (RecyclerView.ViewHolder) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var list = mutableListOf<ReorderRow>()
    val currentRows: List<ReorderRow> get() = list
    var textColor: Int = 0xFF000000.toInt()

    fun setRows(rows: List<ReorderRow>) {
        list = rows.toMutableList()
        notifyDataSetChanged()
    }

    fun swapVisual(from: Int, to: Int) {
        if (from == to) return
        val item = list.removeAt(from)
        list.add(to, item)
        notifyItemMoved(from, to)
    }

    override fun getItemViewType(position: Int): Int =
        when (list[position]) {
            is ReorderRow.Category -> 0
            is ReorderRow.Task -> 1
        }

    override fun getItemCount(): Int = list.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            0 -> {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.item_reorder_category, parent, false)
                object : RecyclerView.ViewHolder(v) {}
            }
            else -> {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.item_reorder_task, parent, false)
                object : RecyclerView.ViewHolder(v) {}
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = list[position]) {
            is ReorderRow.Category -> {
                val tv = holder.itemView as TextView
                tv.text = row.name
                tv.setTextColor(textColor)
            }
            is ReorderRow.Task -> {
                val text = holder.itemView.findViewById<TextView>(R.id.reorder_task_text)
                text.text = row.contentPreview
                text.setTextColor(textColor)
                val timeIcon = holder.itemView.findViewById<TextView>(R.id.reorder_task_time_icon)
                timeIcon.visibility = if (row.hasScheduledTime) View.VISIBLE else View.GONE
                val drag = holder.itemView.findViewById<TextView>(R.id.reorder_task_drag)
                drag.setTextColor(textColor)
                val up = holder.itemView.findViewById<TextView>(R.id.reorder_task_up)
                up.setTextColor(textColor)
                up.setOnClickListener { onMoveUp(holder.bindingAdapterPosition) }
                val down = holder.itemView.findViewById<TextView>(R.id.reorder_task_down)
                down.setTextColor(textColor)
                down.setOnClickListener { onMoveDown(holder.bindingAdapterPosition) }
                drag.setOnTouchListener { _, event ->
                    if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                        onStartDrag(holder)
                    }
                    false
                }
            }
        }
    }
}
