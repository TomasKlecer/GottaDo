package com.klecer.gottado.ui.screen.categorysettings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
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
import kotlinx.coroutines.launch

private val CATEGORY_TYPES = listOf(
    "normal" to R.string.category_type_normal,
    "today" to R.string.category_type_today,
    "tomorrow" to R.string.category_type_tomorrow,
    "monday" to R.string.category_type_monday,
    "tuesday" to R.string.category_type_tuesday,
    "wednesday" to R.string.category_type_wednesday,
    "thursday" to R.string.category_type_thursday,
    "friday" to R.string.category_type_friday,
    "saturday" to R.string.category_type_saturday,
    "sunday" to R.string.category_type_sunday
)

private val PRESET_COLORS = listOf(
    0xFFFFFFFF.toInt(),
    0xFF000000.toInt(),
    0xFFE53935.toInt(),
    0xFFFB8C00.toInt(),
    0xFFFDD835.toInt(),
    0xFF43A047.toInt(),
    0xFF1E88E5.toInt(),
    0xFF8E24AA.toInt()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySettingsScreen(
    viewModel: CategorySettingsViewModel,
    onBack: () -> Unit,
    onManageRoutines: (Long) -> Unit = {}
) {
    val category by viewModel.category.collectAsState()
    var typeExpanded by remember { mutableStateOf(false) }

    if (category == null) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = category!!.name,
            onValueChange = viewModel::updateName,
            label = { Text(stringResource(R.string.category_settings_name)) },
            modifier = Modifier.fillMaxWidth()
        )

        LabelWithInfo(
            stringResource(R.string.category_settings_color),
            stringResource(R.string.info_cat_color),
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            PRESET_COLORS.forEach { color ->
                val selected = category!!.color == color
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(color))
                        .then(
                            if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            else Modifier.border(1.dp, Color.LightGray, CircleShape)
                        )
                        .clickable { viewModel.updateColor(color) }
                )
            }
        }

        CheckboxWithInfo(
            checked = category!!.showCheckboxInsteadOfBullet,
            onCheckedChange = viewModel::updateShowCheckboxInsteadOfBullet,
            label = stringResource(R.string.category_settings_show_checkbox),
            info = stringResource(R.string.info_cat_checkbox)
        )
        CheckboxWithInfo(
            checked = category!!.tasksWithTimeFirst,
            onCheckedChange = viewModel::updateTasksWithTimeFirst,
            label = stringResource(R.string.category_settings_tasks_time_first),
            info = stringResource(R.string.info_cat_time_first)
        )
        LabelWithInfo(
            stringResource(R.string.category_settings_type),
            stringResource(R.string.info_cat_type),
            modifier = Modifier.padding(top = 8.dp)
        )
        ExposedDropdownMenuBox(
            expanded = typeExpanded,
            onExpandedChange = { typeExpanded = it }
        ) {
            OutlinedTextField(
                value = CATEGORY_TYPES.find { it.first == category!!.categoryType }?.let {
                    stringResource(it.second)
                } ?: category!!.categoryType,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.category_settings_type)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = typeExpanded,
                onDismissRequest = { typeExpanded = false }
            ) {
                CATEGORY_TYPES.forEach { (type, labelRes) ->
                    DropdownMenuItem(
                        text = { Text(stringResource(labelRes)) },
                        onClick = {
                            viewModel.updateCategoryType(type)
                            typeExpanded = false
                        }
                    )
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Button(onClick = { onManageRoutines(category!!.id) }) {
                Text(stringResource(R.string.category_settings_manage_routines))
            }
            Spacer(Modifier.width(8.dp))
            InfoIcon(stringResource(R.string.info_cat_routines))
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CheckboxWithInfo(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    info: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.width(4.dp))
        InfoIcon(info)
    }
}
