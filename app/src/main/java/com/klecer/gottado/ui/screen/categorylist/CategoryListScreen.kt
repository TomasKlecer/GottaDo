package com.klecer.gottado.ui.screen.categorylist

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Switch
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.draw.alpha
import com.klecer.gottado.R
import com.klecer.gottado.data.db.entity.CalendarSyncRuleType
import com.klecer.gottado.ui.color.ColorPrefs
import com.klecer.gottado.data.db.entity.RoutineEntity
import com.klecer.gottado.data.db.entity.RoutineFrequency
import com.klecer.gottado.ui.screen.categorysettings.CategoryTabViewModel
import kotlinx.coroutines.launch
import java.util.Calendar

private val SYNC_RULE_TYPES = CalendarSyncRuleType.entries.map { it.name to when (it) {
    CalendarSyncRuleType.TODAY -> R.string.sync_rule_TODAY
    CalendarSyncRuleType.TOMORROW -> R.string.sync_rule_TOMORROW
    CalendarSyncRuleType.CLOSEST_MONDAY -> R.string.sync_rule_CLOSEST_MONDAY
    CalendarSyncRuleType.CLOSEST_TUESDAY -> R.string.sync_rule_CLOSEST_TUESDAY
    CalendarSyncRuleType.CLOSEST_WEDNESDAY -> R.string.sync_rule_CLOSEST_WEDNESDAY
    CalendarSyncRuleType.CLOSEST_THURSDAY -> R.string.sync_rule_CLOSEST_THURSDAY
    CalendarSyncRuleType.CLOSEST_FRIDAY -> R.string.sync_rule_CLOSEST_FRIDAY
    CalendarSyncRuleType.CLOSEST_SATURDAY -> R.string.sync_rule_CLOSEST_SATURDAY
    CalendarSyncRuleType.CLOSEST_SUNDAY -> R.string.sync_rule_CLOSEST_SUNDAY
    CalendarSyncRuleType.THIS_WEEK -> R.string.sync_rule_THIS_WEEK
    CalendarSyncRuleType.NEXT_WEEK -> R.string.sync_rule_NEXT_WEEK
    CalendarSyncRuleType.THIS_MONTH -> R.string.sync_rule_THIS_MONTH
    CalendarSyncRuleType.NEXT_MONTH -> R.string.sync_rule_NEXT_MONTH
} }

private val NOTIFY_MINUTES_OPTIONS = listOf(
    0 to R.string.notify_at_time,
    5 to R.string.notify_5_min,
    10 to R.string.notify_10_min,
    15 to R.string.notify_15_min,
    30 to R.string.notify_30_min,
    60 to R.string.notify_1_hour,
    120 to R.string.notify_2_hours
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListScreen(
    viewModel: CategoryTabViewModel,
    onAddRoutine: (Long) -> Unit,
    onEditRoutine: (Long, Long) -> Unit
) {
    val categories by viewModel.categories.collectAsState()
    val selectedId by viewModel.selectedCategoryId.collectAsState()
    val category by viewModel.category.collectAsState()
    val routines by viewModel.routines.collectAsState()
    val syncRules by viewModel.syncRules.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshRoutines()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    if (showAddDialog) {
        val focusRequester = remember { FocusRequester() }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(stringResource(R.string.category_list_add)) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(stringResource(R.string.category_settings_name)) },
                    singleLine = true,
                    modifier = Modifier.focusRequester(focusRequester)
                )
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.addCategory(newName)
                    newName = ""
                    showAddDialog = false
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    if (categories.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No categories yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.padding(top = 16.dp)
            ) { Text(stringResource(R.string.category_list_add)) }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var selectorExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = selectorExpanded,
                onExpandedChange = { selectorExpanded = it },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = categories.find { it.id == selectedId }?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.tab_categories)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = selectorExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = selectorExpanded,
                    onDismissRequest = { selectorExpanded = false }
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.name) },
                            onClick = {
                                viewModel.selectCategory(cat.id)
                                selectorExpanded = false
                            }
                        )
                    }
                }
            }
            IconButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.category_list_add))
            }
        }

        if (category == null) return

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        OutlinedTextField(
            value = category!!.name,
            onValueChange = viewModel::updateName,
            label = { Text(stringResource(R.string.category_settings_name)) },
            modifier = Modifier.fillMaxWidth()
        )

        val categoryColors = viewModel.colorPrefs.getPalette(ColorPrefs.KEY_CATEGORY)

        LabelWithInfo(
            stringResource(R.string.category_settings_color),
            stringResource(R.string.info_cat_color),
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            categoryColors.forEach { color ->
                val selected = category!!.color == color
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
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
            checked = category!!.autoSortTimedEntries,
            onCheckedChange = viewModel::updateAutoSortTimedEntries,
            label = stringResource(R.string.category_settings_auto_sort_timed),
            info = stringResource(R.string.info_cat_auto_sort_timed)
        )
        if (category!!.autoSortTimedEntries) {
            SwitchWithInfo(
                checked = category!!.tasksWithTimeFirst,
                onCheckedChange = viewModel::updateTasksWithTimeFirst,
                labelOn = stringResource(R.string.category_settings_timed_position_top),
                labelOff = stringResource(R.string.category_settings_timed_position_bottom),
                info = stringResource(R.string.info_cat_timed_position)
            )
            SwitchWithInfo(
                checked = category!!.timedEntriesAscending,
                onCheckedChange = viewModel::updateTimedEntriesAscending,
                labelOn = stringResource(R.string.category_settings_timed_order_asc),
                labelOff = stringResource(R.string.category_settings_timed_order_desc),
                info = stringResource(R.string.info_cat_timed_order)
            )
        }
        CheckboxWithInfo(
            checked = category!!.showCalendarIcon,
            onCheckedChange = viewModel::updateShowCalendarIcon,
            label = stringResource(R.string.category_settings_show_calendar_icon),
            info = stringResource(R.string.info_cat_show_calendar_icon)
        )
        CheckboxWithInfo(
            checked = category!!.showDeleteButton,
            onCheckedChange = viewModel::updateShowDeleteButton,
            label = stringResource(R.string.category_settings_show_delete_button),
            info = stringResource(R.string.info_cat_show_delete_button)
        )

        val calendarSyncEnabled = viewModel.syncPrefs.enabled
        val notificationsEnabled = viewModel.syncPrefs.notificationsEnabled

        DisabledSectionWrapper(
            enabled = calendarSyncEnabled,
            disabledTooltip = stringResource(R.string.disabled_calendar_sync_tooltip)
        ) {
            LabelWithInfo(
                stringResource(R.string.calendar_sync_rules_label),
                stringResource(R.string.info_cat_sync_rules),
                modifier = Modifier.padding(top = 12.dp)
            )

            val existingRuleTypes = syncRules.map { it.ruleType }.toSet()
            val availableRules = SYNC_RULE_TYPES.filter { it.first !in existingRuleTypes }

            var rulesExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = if (calendarSyncEnabled) rulesExpanded else false,
                onExpandedChange = { if (calendarSyncEnabled) rulesExpanded = it }
            ) {
                OutlinedTextField(
                    value = stringResource(R.string.calendar_sync_rules_add),
                    onValueChange = {},
                    readOnly = true,
                    enabled = calendarSyncEnabled,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rulesExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = rulesExpanded, onDismissRequest = { rulesExpanded = false }) {
                    availableRules.forEach { (type, labelRes) ->
                        DropdownMenuItem(
                            text = { Text(stringResource(labelRes)) },
                            onClick = {
                                viewModel.addSyncRule(type)
                                rulesExpanded = false
                            }
                        )
                    }
                }
            }

            syncRules.forEach { rule ->
                val labelRes = SYNC_RULE_TYPES.find { it.first == rule.ruleType }?.second
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (labelRes != null) stringResource(labelRes) else rule.ruleType,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f).padding(start = 8.dp)
                    )
                    IconButton(
                        onClick = { if (calendarSyncEnabled) viewModel.removeSyncRule(rule.id) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("✕", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        DisabledSectionWrapper(
            enabled = notificationsEnabled,
            disabledTooltip = stringResource(R.string.disabled_notifications_tooltip)
        ) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            CheckboxWithInfo(
                checked = category!!.notifyOnTime,
                onCheckedChange = { if (notificationsEnabled) viewModel.updateNotifyOnTime(it) },
                label = stringResource(R.string.category_settings_notify_on_time),
                info = stringResource(R.string.info_cat_notify_on_time)
            )

            if (category!!.notifyOnTime && notificationsEnabled) {
                LabelWithInfo(
                    stringResource(R.string.category_settings_notify_minutes_before),
                    stringResource(R.string.info_cat_notify_minutes_before),
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )

                var minutesExpanded by remember { mutableStateOf(false) }
                val currentLabel = NOTIFY_MINUTES_OPTIONS.find { it.first == category!!.notifyMinutesBefore }?.second
                    ?: R.string.notify_15_min

                ExposedDropdownMenuBox(
                    expanded = minutesExpanded,
                    onExpandedChange = { minutesExpanded = it }
                ) {
                    OutlinedTextField(
                        value = stringResource(currentLabel),
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = minutesExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = minutesExpanded,
                        onDismissRequest = { minutesExpanded = false }
                    ) {
                        NOTIFY_MINUTES_OPTIONS.forEach { (minutes, labelRes) ->
                            DropdownMenuItem(
                                text = { Text(stringResource(labelRes)) },
                                onClick = {
                                    viewModel.updateNotifyMinutesBefore(minutes)
                                    minutesExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        LabelWithInfo(
            stringResource(R.string.category_settings_manage_routines),
            stringResource(R.string.info_cat_routines)
        )

        routines.forEach { routine ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (!routine.name.isNullOrBlank()) {
                            Text(routine.name, style = MaterialTheme.typography.bodyLarge)
                        }
                        Text(
                            routineSummary(routine),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row {
                        TextButton(onClick = { selectedId?.let { catId -> onEditRoutine(catId, routine.id) } }) {
                            Text(stringResource(R.string.routine_edit))
                        }
                        TextButton(onClick = { viewModel.deleteRoutine(routine.id) }) {
                            Text(stringResource(R.string.routine_delete))
                        }
                    }
                }
            }
        }

        Button(
            onClick = { selectedId?.let { onAddRoutine(it) } },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(stringResource(R.string.routine_add))
        }
    }
}

private fun routineSummary(r: RoutineEntity): String {
    val time = "%02d:%02d".format(r.scheduleTimeHour, r.scheduleTimeMinute)
    return when (r.frequency) {
        RoutineFrequency.DAILY -> "Daily at $time"
        RoutineFrequency.WEEKLY -> {
            val day = when (r.scheduleDayOfWeek) {
                Calendar.SUNDAY -> "Sun"; Calendar.MONDAY -> "Mon"; Calendar.TUESDAY -> "Tue"
                Calendar.WEDNESDAY -> "Wed"; Calendar.THURSDAY -> "Thu"; Calendar.FRIDAY -> "Fri"
                Calendar.SATURDAY -> "Sat"; else -> "?"
            }
            "Weekly $day at $time"
        }
        RoutineFrequency.MONTHLY -> "Monthly day ${r.scheduleDayOfMonth ?: 1} at $time"
        RoutineFrequency.YEARLY -> "Yearly ${r.scheduleDay ?: 1}. ${r.scheduleMonth?.let { m ->
            when (m) {
                Calendar.JANUARY -> "Jan"; Calendar.FEBRUARY -> "Feb"; Calendar.MARCH -> "Mar"
                Calendar.APRIL -> "Apr"; Calendar.MAY -> "May"; Calendar.JUNE -> "Jun"
                Calendar.JULY -> "Jul"; Calendar.AUGUST -> "Aug"; Calendar.SEPTEMBER -> "Sep"
                Calendar.OCTOBER -> "Oct"; Calendar.NOVEMBER -> "Nov"; Calendar.DECEMBER -> "Dec"
                else -> "?"
            }
        } ?: ""} at $time"
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CheckboxWithInfo(checked: Boolean, onCheckedChange: (Boolean) -> Unit, label: String, info: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.width(4.dp))
        InfoIcon(info)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwitchWithInfo(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    labelOn: String,
    labelOff: String,
    info: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
    ) {
        Switch(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(Modifier.width(8.dp))
        Text(
            if (checked) labelOn else labelOff,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.width(4.dp))
        InfoIcon(info)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DisabledSectionWrapper(
    enabled: Boolean,
    disabledTooltip: String,
    content: @Composable () -> Unit
) {
    if (enabled) {
        content()
    } else {
        val tooltipState = rememberTooltipState(isPersistent = true)
        val scope = rememberCoroutineScope()
        TooltipBox(
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
            tooltip = { PlainTooltip { Text(disabledTooltip) } },
            state = tooltipState,
            enableUserInput = false
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(0.4f)
                    .clickable { scope.launch { tooltipState.show() } }
            ) {
                content()
            }
        }
    }
}
