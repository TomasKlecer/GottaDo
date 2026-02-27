package com.klecer.gottado.ui.screen.widgetlist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.klecer.gottado.R
import com.klecer.gottado.data.db.entity.ReorderHandlePosition
import com.klecer.gottado.ui.color.ColorPrefs
import com.klecer.gottado.ui.screen.widgetsettings.WidgetTabViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetListScreen(
    viewModel: WidgetTabViewModel
) {
    val widgets by viewModel.widgets.collectAsState()
    val selectedId by viewModel.selectedWidgetId.collectAsState()
    val config by viewModel.config.collectAsState()
    val categoryJoins by viewModel.categoryJoins.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }

    if (widgets.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.widget_list_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = { viewModel.addPreset() },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(stringResource(R.string.widget_add_preset))
            }
        }
        return
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    if (showDeleteDialog && selectedId != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.widget_delete_preset_title)) },
            text = { Text(stringResource(R.string.widget_delete_preset_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePreset(selectedId!!)
                    showDeleteDialog = false
                }) {
                    Text(stringResource(R.string.widget_delete_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.widget_delete_cancel))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        var selectorExpanded by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExposedDropdownMenuBox(
                expanded = selectorExpanded,
                onExpandedChange = { selectorExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = widgets.find { it.widgetId == selectedId }?.let {
                        it.title?.takeIf { t -> t.isNotBlank() }
                            ?: stringResource(R.string.widget_list_item_default, it.widgetId)
                    } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.tab_widgets)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = selectorExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = selectorExpanded,
                    onDismissRequest = { selectorExpanded = false }
                ) {
                    widgets.forEach { w ->
                        val label = w.title?.takeIf { it.isNotBlank() }
                            ?: stringResource(R.string.widget_list_item_default, w.widgetId)
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                viewModel.selectWidget(w.widgetId)
                                selectorExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { viewModel.addPreset() }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.widget_add_preset))
            }
        }

        if (config == null) return

        val assignedIds = categoryJoins.map { it.join.categoryId }.toSet()
        val availableToAdd = allCategories.filter { it.id !in assignedIds }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        OutlinedTextField(
            value = config!!.title ?: "",
            onValueChange = viewModel::updateTitle,
            label = { Text(stringResource(R.string.widget_settings_title)) },
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Checkbox(
                checked = config!!.showTitleOnWidget,
                onCheckedChange = { viewModel.updateShowTitleOnWidget(it) }
            )
            Text(stringResource(R.string.widget_settings_show_title_on_widget))
            Spacer(Modifier.width(4.dp))
            InfoIcon(stringResource(R.string.info_widget_show_title))
        }
        OutlinedTextField(
            value = config!!.subtitle ?: "",
            onValueChange = viewModel::updateSubtitle,
            label = { Text(stringResource(R.string.widget_settings_subtitle)) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )
        OutlinedTextField(
            value = config!!.note ?: "",
            onValueChange = viewModel::updateNote,
            label = { Text(stringResource(R.string.widget_settings_note)) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )

        LabelWithInfo(
            stringResource(R.string.widget_settings_categories),
            stringResource(R.string.info_widget_categories),
            modifier = Modifier.padding(top = 16.dp)
        )
        categoryJoins.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = item.join.visible,
                        onCheckedChange = { viewModel.setCategoryVisible(item.join.categoryId, it) }
                    )
                    Text(item.categoryName)
                }
                Row {
                    TextButton(onClick = { viewModel.moveCategoryUp(item.join.categoryId) }) { Text("↑") }
                    TextButton(onClick = { viewModel.moveCategoryDown(item.join.categoryId) }) { Text("↓") }
                    TextButton(onClick = { viewModel.removeCategoryFromWidget(item.join.categoryId) }) {
                        Text(stringResource(R.string.widget_settings_remove))
                    }
                }
            }
        }
        if (availableToAdd.isNotEmpty()) {
            Text(
                stringResource(R.string.widget_settings_add_category),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 8.dp)
            )
            availableToAdd.forEach { cat ->
                TextButton(
                    onClick = { viewModel.addCategoryToWidget(cat.id) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("+ ${cat.name}") }
            }
        }

        val bgColors = viewModel.colorPrefs.getPalette(ColorPrefs.KEY_WIDGET_BG)
        val textColors = viewModel.colorPrefs.getPalette(ColorPrefs.KEY_WIDGET_TEXT)

        LabelWithInfo(stringResource(R.string.widget_settings_bg_color), stringResource(R.string.info_widget_bg_color), Modifier.padding(top = 16.dp, bottom = 8.dp))
        ColorRow(bgColors, config!!.backgroundColor) { viewModel.updateBackgroundColor(it) }

        LabelWithInfo(stringResource(R.string.widget_settings_background), stringResource(R.string.info_widget_bg_opacity), Modifier.padding(top = 16.dp))
        SliderRow(config!!.backgroundAlpha, 0f..1f, { viewModel.updateBackgroundAlpha(it) }) { "%.0f%%".format(it * 100) }

        LabelWithInfo(stringResource(R.string.widget_settings_title_color), stringResource(R.string.info_widget_title_color), Modifier.padding(top = 16.dp, bottom = 8.dp))
        ColorRow(textColors, config!!.titleColor) { viewModel.updateTitleColor(it) }

        LabelWithInfo(stringResource(R.string.widget_settings_subtitle_color), stringResource(R.string.info_widget_subtitle_color), Modifier.padding(top = 16.dp, bottom = 8.dp))
        ColorRow(textColors, config!!.subtitleColor) { viewModel.updateSubtitleColor(it) }

        LabelWithInfo(stringResource(R.string.widget_settings_default_text_color), stringResource(R.string.info_widget_default_text_color), Modifier.padding(top = 16.dp, bottom = 8.dp))
        ColorRow(textColors, config!!.defaultTextColor) { viewModel.updateDefaultTextColor(it) }

        LabelWithInfo(stringResource(R.string.widget_settings_bullet_size), stringResource(R.string.info_widget_bullet_size), Modifier.padding(top = 16.dp))
        SliderRow(config!!.bulletSizeDp.toFloat(), 4f..24f, { viewModel.updateBulletSizeDp(it.toInt()) }) { "${it.toInt()}dp" }

        LabelWithInfo(stringResource(R.string.widget_settings_checkbox_size), stringResource(R.string.info_widget_checkbox_size), Modifier.padding(top = 8.dp))
        SliderRow(config!!.checkboxSizeDp.toFloat(), 4f..24f, { viewModel.updateCheckboxSizeDp(it.toInt()) }) { "${it.toInt()}dp" }

        LabelWithInfo(stringResource(R.string.widget_settings_category_font_size), stringResource(R.string.info_widget_cat_font_size), Modifier.padding(top = 8.dp))
        Slider(value = config!!.categoryFontSizeSp, onValueChange = viewModel::updateCategoryFontSizeSp, valueRange = 8f..32f, modifier = Modifier.fillMaxWidth())

        LabelWithInfo(stringResource(R.string.widget_settings_record_font_size), stringResource(R.string.info_widget_record_font_size), Modifier.padding(top = 8.dp))
        Slider(value = config!!.recordFontSizeSp, onValueChange = viewModel::updateRecordFontSizeSp, valueRange = 8f..32f, modifier = Modifier.fillMaxWidth())

        LabelWithInfo(stringResource(R.string.widget_settings_reorder_handle), stringResource(R.string.info_widget_reorder_handle), Modifier.padding(top = 16.dp))
        var reorderExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = reorderExpanded, onExpandedChange = { reorderExpanded = it }) {
            OutlinedTextField(
                value = config!!.reorderHandlePosition.name,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.widget_settings_reorder_handle)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reorderExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = reorderExpanded, onDismissRequest = { reorderExpanded = false }) {
                ReorderHandlePosition.entries.forEach { pos ->
                    DropdownMenuItem(
                        text = { Text(pos.name) },
                        onClick = { viewModel.updateReorderHandlePosition(pos); reorderExpanded = false }
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 12.dp)
        ) {
            Checkbox(
                checked = config!!.buttonsAtBottom,
                onCheckedChange = { viewModel.updateButtonsAtBottom(it) }
            )
            Text(stringResource(R.string.widget_settings_buttons_at_bottom))
            Spacer(Modifier.width(4.dp))
            InfoIcon(stringResource(R.string.info_widget_buttons_at_bottom))
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { showDeleteDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text(stringResource(R.string.widget_delete_preset))
        }
    }
}

@Composable
private fun ColorRow(colors: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        colors.forEach { color ->
            val isSel = (selected and 0x00FFFFFF) == (color and 0x00FFFFFF)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(Color(color))
                    .then(
                        if (isSel) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        else Modifier.border(1.dp, Color.LightGray, CircleShape)
                    )
                    .clickable { onSelect(color) }
            )
        }
    }
}

@Composable
private fun SliderRow(value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit, label: (Float) -> String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Slider(value = value, onValueChange = onChange, valueRange = range, modifier = Modifier.weight(1f))
        Text(label(value))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InfoIcon(info: String) {
    val tooltipState = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(info) } },
        state = tooltipState,
        enableUserInput = false
    ) {
        Icon(Icons.Default.Info, contentDescription = null,
            modifier = Modifier.size(16.dp).clickable { scope.launch { tooltipState.show() } },
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
    }
}

@Composable
private fun LabelWithInfo(label: String, info: String, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.width(4.dp))
        InfoIcon(info)
    }
}
