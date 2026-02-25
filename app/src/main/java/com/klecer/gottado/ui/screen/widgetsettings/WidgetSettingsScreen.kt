package com.klecer.gottado.ui.screen.widgetsettings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
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
import kotlinx.coroutines.launch

private val BG_COLORS = listOf(
    0xFF000000.toInt(),
    0xFF333333.toInt(),
    0xFF666666.toInt(),
    0xFF999999.toInt(),
    0xFFFFFFFF.toInt(),
    0xFF1A237E.toInt(),
    0xFF004D40.toInt(),
    0xFF3E2723.toInt()
)

private val TEXT_COLORS = listOf(
    0xFFFFFFFF.toInt(),
    0xFF000000.toInt(),
    0xFF666666.toInt(),
    0xFFE53935.toInt(),
    0xFFFB8C00.toInt(),
    0xFFFDD835.toInt(),
    0xFF43A047.toInt(),
    0xFF1E88E5.toInt(),
    0xFF8E24AA.toInt()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetSettingsScreen(
    viewModel: WidgetSettingsViewModel,
    onBack: () -> Unit
) {
    val config by viewModel.config.collectAsState()
    val categoryJoins by viewModel.categoryJoins.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()
    val assignedIds = categoryJoins.map { it.join.categoryId }.toSet()
    val availableToAdd = allCategories.filter { it.id !in assignedIds }

    if (config == null) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
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
                    TextButton(onClick = { viewModel.moveCategoryUp(item.join.categoryId) }) {
                        Text("↑")
                    }
                    TextButton(onClick = { viewModel.moveCategoryDown(item.join.categoryId) }) {
                        Text("↓")
                    }
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
                ) {
                    Text("+ ${cat.name}")
                }
            }
        }

        LabelWithInfo(
            stringResource(R.string.widget_settings_bg_color),
            stringResource(R.string.info_widget_bg_color),
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            BG_COLORS.forEach { color ->
                val selected = (config!!.backgroundColor and 0x00FFFFFF) == (color and 0x00FFFFFF)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(color))
                        .then(
                            if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            else Modifier.border(1.dp, Color.LightGray, CircleShape)
                        )
                        .clickable { viewModel.updateBackgroundColor(color) }
                )
            }
        }

        LabelWithInfo(
            stringResource(R.string.widget_settings_background),
            stringResource(R.string.info_widget_bg_opacity),
            modifier = Modifier.padding(top = 16.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Slider(
                value = config!!.backgroundAlpha,
                onValueChange = viewModel::updateBackgroundAlpha,
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f)
            )
            Text("%.0f%%".format(config!!.backgroundAlpha * 100))
        }

        LabelWithInfo(
            stringResource(R.string.widget_settings_title_color),
            stringResource(R.string.info_widget_title_color),
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            TEXT_COLORS.forEach { color ->
                val selected = config!!.titleColor == color
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(color))
                        .then(
                            if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            else Modifier.border(1.dp, Color.LightGray, CircleShape)
                        )
                        .clickable { viewModel.updateTitleColor(color) }
                )
            }
        }

        LabelWithInfo(
            stringResource(R.string.widget_settings_subtitle_color),
            stringResource(R.string.info_widget_subtitle_color),
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            TEXT_COLORS.forEach { color ->
                val selected = config!!.subtitleColor == color
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(color))
                        .then(
                            if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            else Modifier.border(1.dp, Color.LightGray, CircleShape)
                        )
                        .clickable { viewModel.updateSubtitleColor(color) }
                )
            }
        }

        LabelWithInfo(
            stringResource(R.string.widget_settings_default_text_color),
            stringResource(R.string.info_widget_default_text_color),
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            TEXT_COLORS.forEach { color ->
                val selected = config!!.defaultTextColor == color
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(color))
                        .then(
                            if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            else Modifier.border(1.dp, Color.LightGray, CircleShape)
                        )
                        .clickable { viewModel.updateDefaultTextColor(color) }
                )
            }
        }

        LabelWithInfo(
            stringResource(R.string.widget_settings_bullet_size),
            stringResource(R.string.info_widget_bullet_size),
            modifier = Modifier.padding(top = 16.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Slider(
                value = config!!.bulletSizeDp.toFloat(),
                onValueChange = { viewModel.updateBulletSizeDp(it.toInt()) },
                valueRange = 4f..24f,
                modifier = Modifier.weight(1f)
            )
            Text("${config!!.bulletSizeDp}dp")
        }

        LabelWithInfo(
            stringResource(R.string.widget_settings_checkbox_size),
            stringResource(R.string.info_widget_checkbox_size),
            modifier = Modifier.padding(top = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Slider(
                value = config!!.checkboxSizeDp.toFloat(),
                onValueChange = { viewModel.updateCheckboxSizeDp(it.toInt()) },
                valueRange = 4f..24f,
                modifier = Modifier.weight(1f)
            )
            Text("${config!!.checkboxSizeDp}dp")
        }

        LabelWithInfo(
            stringResource(R.string.widget_settings_category_font_size),
            stringResource(R.string.info_widget_cat_font_size),
            modifier = Modifier.padding(top = 8.dp)
        )
        Slider(
            value = config!!.categoryFontSizeSp,
            onValueChange = viewModel::updateCategoryFontSizeSp,
            valueRange = 8f..32f,
            modifier = Modifier.fillMaxWidth()
        )
        LabelWithInfo(
            stringResource(R.string.widget_settings_record_font_size),
            stringResource(R.string.info_widget_record_font_size),
            modifier = Modifier.padding(top = 8.dp)
        )
        Slider(
            value = config!!.recordFontSizeSp,
            onValueChange = viewModel::updateRecordFontSizeSp,
            valueRange = 8f..32f,
            modifier = Modifier.fillMaxWidth()
        )

        LabelWithInfo(
            stringResource(R.string.widget_settings_reorder_handle),
            stringResource(R.string.info_widget_reorder_handle),
            modifier = Modifier.padding(top = 16.dp)
        )
        var reorderExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = reorderExpanded,
            onExpandedChange = { reorderExpanded = it }
        ) {
            OutlinedTextField(
                value = config!!.reorderHandlePosition.name,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.widget_settings_reorder_handle)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reorderExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = reorderExpanded,
                onDismissRequest = { reorderExpanded = false }
            ) {
                ReorderHandlePosition.entries.forEach { pos ->
                    DropdownMenuItem(
                        text = { Text(pos.name) },
                        onClick = {
                            viewModel.updateReorderHandlePosition(pos)
                            reorderExpanded = false
                        }
                    )
                }
            }
        }
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
        Icon(
            Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier
                .size(16.dp)
                .clickable { scope.launch { tooltipState.show() } },
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LabelWithInfo(
    label: String,
    info: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.width(4.dp))
        InfoIcon(info)
    }
}
