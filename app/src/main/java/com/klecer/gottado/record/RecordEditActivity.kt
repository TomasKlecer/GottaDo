package com.klecer.gottado.record

import android.graphics.drawable.ColorDrawable

import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.TextUtils
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.klecer.gottado.R
import com.klecer.gottado.domain.usecase.GetRecordEditOptionsUseCase
import com.klecer.gottado.widget.WidgetIntents
import com.klecer.gottado.widget.WidgetUpdateHelper
import com.klecer.gottado.widget.widgetEntryPoint
import com.klecer.gottado.ui.color.ColorPrefs
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class RecordEditActivity : ComponentActivity() {

    private val viewModel: RecordEditViewModel by viewModels()

    @Inject
    lateinit var getRecordEditOptionsUseCase: GetRecordEditOptionsUseCase

    @Inject
    lateinit var colorPrefs: ColorPrefs

    private lateinit var contentEdit: EditText
    private lateinit var categorySpinner: android.widget.Spinner
    private lateinit var bulletColorsContainer: LinearLayout
    private lateinit var textColorsContainer: LinearLayout
    private lateinit var cancelButton: View
    private lateinit var saveButton: View
    private lateinit var deleteButton: View
    private lateinit var timeEntry: EditText
    private lateinit var timeClearButton: android.widget.Button
    private lateinit var completedCheckbox: android.widget.CheckBox

    private var initialContentSet = false
    private var unifiedColorMode = false

    private val presetColors: List<Int> by lazy {
        colorPrefs.getPalette(ColorPrefs.KEY_ENTRY)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_record_edit)

        contentEdit = findViewById(R.id.record_edit_content)
        categorySpinner = findViewById(R.id.record_edit_category)
        bulletColorsContainer = findViewById(R.id.record_edit_bullet_colors)
        textColorsContainer = findViewById(R.id.record_edit_text_colors)
        cancelButton = findViewById(R.id.record_edit_cancel)
        saveButton = findViewById(R.id.record_edit_save)
        deleteButton = findViewById(R.id.record_edit_delete)
        timeEntry = findViewById(R.id.record_edit_time_entry)
        timeClearButton = findViewById(R.id.record_edit_time_clear)
        completedCheckbox = findViewById(R.id.record_edit_completed)

        val action = intent?.action ?: ""
        val widgetId = intent?.getIntExtra(WidgetIntents.EXTRA_WIDGET_ID, -1) ?: -1
        val taskId = if (intent?.hasExtra(WidgetIntents.EXTRA_TASK_ID) == true) {
            intent.getLongExtra(WidgetIntents.EXTRA_TASK_ID, -1L).takeIf { it != -1L }
        } else null
        val categoryId = intent?.getLongExtra(WidgetIntents.EXTRA_CATEGORY_ID, 0L) ?: 0L

        var widgetTextColor = 0xFF000000.toInt()
        if (widgetId != -1) {
            try {
                val entryPoint = applicationContext.widgetEntryPoint()
                val presetId = runBlocking {
                    entryPoint.getWidgetInstanceDao().getPresetId(widgetId) ?: widgetId
                }
                val cfg = runBlocking {
                    entryPoint.getWidgetConfigRepository().getByWidgetId(presetId)
                }
                if (cfg != null) {
                    val a = (cfg.backgroundAlpha * 255).toInt().coerceIn(0, 255)
                    val bgColor = (a shl 24) or (cfg.backgroundColor and 0x00FFFFFF)
                    window.setBackgroundDrawable(ColorDrawable(bgColor))
                    widgetTextColor = cfg.defaultTextColor
                }
            } catch (_: Throwable) {}
        }
        applyTextColorToLabels(widgetTextColor)

        viewModel.init(taskId, categoryId, widgetId, widgetTextColor)

        val options = getRecordEditOptionsUseCase()
        unifiedColorMode = options.useUnifiedColorPicker
        if (unifiedColorMode) {
            findViewById<View>(R.id.record_edit_bullet_section).visibility = View.VISIBLE
            findViewById<View>(R.id.record_edit_text_color_section).visibility = View.GONE
            findViewById<android.widget.TextView>(R.id.record_edit_bullet_label).text = getString(R.string.record_edit_color)
        } else {
            findViewById<View>(R.id.record_edit_bullet_section).visibility = if (options.showBulletColor) View.VISIBLE else View.GONE
            findViewById<View>(R.id.record_edit_text_color_section).visibility = if (options.showTextColor) View.VISIBLE else View.GONE
        }
        findViewById<View>(R.id.record_edit_time_section).visibility = if (options.showTimeField) View.VISIBLE else View.GONE
        findViewById<View>(R.id.record_edit_category_section).visibility = if (options.showCategoryDropdown) View.VISIBLE else View.GONE
        completedCheckbox.visibility = if (options.showCompletedCheckbox) View.VISIBLE else View.GONE

        setupBulletColors()
        setupTextColors()
        setupTimeEntry()
        timeClearButton.setOnClickListener {
            viewModel.updateScheduledTime(null)
            timeEntry.setText("")
        }
        saveButton.setOnClickListener {
            viewModel.updateContent(htmlFromPlain(contentEdit.text.toString()))
            viewModel.updateCompleted(completedCheckbox.isChecked)
            viewModel.save()
        }
        cancelButton.setOnClickListener { finish() }
        deleteButton.setOnClickListener {
            viewModel.updateContent(htmlFromPlain(contentEdit.text.toString()))
            viewModel.updateCompleted(completedCheckbox.isChecked)
            viewModel.delete()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    if (!state.isLoading && !initialContentSet) {
                        contentEdit.setText(plainFromHtml(state.contentHtml))
                        completedCheckbox.isChecked = state.completed
                        initialContentSet = true
                        if (state.isAddMode) {
                            contentEdit.requestFocus()
                            contentEdit.post {
                                val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                                imm.showSoftInput(contentEdit, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
                            }
                        }
                    }
                    updateColorSelection(bulletColorsContainer, presetColors, state.bulletColor)
                    updateColorSelection(textColorsContainer, presetColors, state.textColor.takeIf { it != 0 } ?: presetColors[0])
                    cancelButton.visibility = if (state.isAddMode) View.GONE else View.VISIBLE
                    deleteButton.visibility = if (state.isAddMode) View.GONE else View.VISIBLE
                    if (!state.isLoading && !timeInitialized) {
                        populateTimeEntry(state.scheduledTimeMillis)
                    }
                    if (state.categories.isNotEmpty() && categorySpinner.adapter == null) {
                        val names = state.categories.map { it.name }
                        val txtColor = savedWidgetTextColor
                        val adapter = object : ArrayAdapter<String>(this@RecordEditActivity, android.R.layout.simple_spinner_item, names) {
                            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                                val v = super.getView(position, convertView, parent)
                                (v as? android.widget.TextView)?.setTextColor(txtColor)
                                return v
                            }
                            override fun getDropDownView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                                return super.getDropDownView(position, convertView, parent)
                            }
                        }
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        categorySpinner.adapter = adapter
                        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                                state.categories.getOrNull(pos)?.let { viewModel.updateCategoryId(it.id) }
                            }
                            override fun onNothingSelected(parent: AdapterView<*>?) {}
                        }
                    }
                    state.categories.indexOfFirst { it.id == state.categoryId }.takeIf { it >= 0 }?.let { idx ->
                        if (categorySpinner.selectedItemPosition != idx) {
                            categorySpinner.setSelection(idx)
                        }
                    }
                    if (state.finishWithRefresh) {
                        val wId = state.widgetId
                        val trashId = state.deletedTrashIdForUndo
                        viewModel.clearFinishAndUndoFlag()
                        if (trashId != null && wId != -1) {
                            com.klecer.gottado.widget.WidgetUndoHelper.storePendingUndo(this@RecordEditActivity, trashId, wId)
                        }
                        if (wId != -1) {
                            WidgetUpdateHelper.update(this@RecordEditActivity, wId)
                        }
                        finish()
                    }
                }
            }
        }
    }

    private fun setupBulletColors() {
        presetColors.forEach { color ->
            val sizePx = (32 * resources.displayMetrics.density).toInt()
            val view = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                    setMargins(4, 0, 4, 0)
                }
                background = makeColorSwatch(color, false)
                setOnClickListener {
                    viewModel.updateBulletColor(color)
                    if (unifiedColorMode) viewModel.updateTextColor(color)
                }
            }
            bulletColorsContainer.addView(view)
        }
    }

    private fun setupTextColors() {
        presetColors.forEach { color ->
            val sizePx = (32 * resources.displayMetrics.density).toInt()
            val view = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                    setMargins(4, 0, 4, 0)
                }
                background = makeColorSwatch(color, false)
                setOnClickListener { viewModel.updateTextColor(color) }
            }
            textColorsContainer.addView(view)
        }
    }

    private fun makeColorSwatch(color: Int, selected: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            if (selected) {
                setStroke((3 * resources.displayMetrics.density).toInt(), 0xFF333333.toInt())
            } else {
                setStroke((1 * resources.displayMetrics.density).toInt(), 0xFFCCCCCC.toInt())
            }
        }
    }

    private fun updateColorSelection(container: LinearLayout, colors: List<Int>, selectedColor: Int) {
        for (i in 0 until container.childCount) {
            if (i < colors.size) {
                container.getChildAt(i).background = makeColorSwatch(colors[i], colors[i] == selectedColor)
            }
        }
    }

    private fun applyTextColorToLabels(color: Int) {
        val hintColor = (color and 0x00FFFFFF) or 0x80000000.toInt()
        contentEdit.setTextColor(color)
        contentEdit.setHintTextColor(hintColor)
        completedCheckbox.setTextColor(color)
        completedCheckbox.buttonTintList = android.content.res.ColorStateList.valueOf(color)
        fun colorAll(vg: android.view.ViewGroup) {
            for (i in 0 until vg.childCount) {
                when (val child = vg.getChildAt(i)) {
                    is android.widget.Button -> child.setTextColor(color)
                    is android.widget.CheckBox -> {
                        child.setTextColor(color)
                        child.buttonTintList = android.content.res.ColorStateList.valueOf(color)
                    }
                    is android.widget.EditText -> {
                        child.setTextColor(color)
                        child.setHintTextColor(hintColor)
                    }
                    is android.widget.TextView -> child.setTextColor(color)
                    is android.view.ViewGroup -> colorAll(child)
                }
            }
        }
        val content = window.decorView.findViewById<android.view.ViewGroup>(android.R.id.content)
        colorAll(content)
        savedWidgetTextColor = color
    }

    private var savedWidgetTextColor: Int = 0xFF000000.toInt()

    private var timeInitialized = false

    private fun setupTimeEntry() {
        timeEntry.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (!timeInitialized) return
                val parts = s.toString().split(":")
                val hour = parts.getOrNull(0)?.trim()?.toIntOrNull()
                val minute = parts.getOrNull(1)?.trim()?.toIntOrNull()
                if (hour != null && hour in 0..23 && minute != null && minute in 0..59) {
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    viewModel.updateScheduledTime(cal.timeInMillis)
                }
            }
        })
    }

    private fun populateTimeEntry(millis: Long?) {
        if (millis != null) {
            val c = Calendar.getInstance().apply { timeInMillis = millis }
            timeEntry.setText("%02d:%02d".format(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)))
        }
        timeInitialized = true
    }

    companion object {
        private fun plainFromHtml(html: String): String {
            val spanned = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
            } else {
                @Suppress("DEPRECATION")
                Html.fromHtml(html)
            }
            return spanned.toString().trimEnd()
        }

        private fun htmlFromPlain(text: String): String {
            return text.lines().joinToString("<br>") { TextUtils.htmlEncode(it) }
        }
    }
}
