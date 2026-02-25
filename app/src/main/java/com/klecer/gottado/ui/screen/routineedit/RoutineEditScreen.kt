@file:OptIn(ExperimentalMaterial3Api::class)

package com.klecer.gottado.ui.screen.routineedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.klecer.gottado.R
import com.klecer.gottado.data.db.entity.RoutineFrequency
import com.klecer.gottado.data.db.entity.RoutineTaskAction
import com.klecer.gottado.data.db.entity.RoutineVisibilityMode
import java.util.Calendar

@Composable
fun RoutineEditScreen(
    viewModel: RoutineEditViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }

    if (state.isLoading) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = state.name,
            onValueChange = viewModel::updateName,
            label = { Text(stringResource(R.string.routine_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        SectionTitle(stringResource(R.string.routine_frequency))
        FrequencyDropdown(state.frequency, viewModel::updateFrequency)

        SectionTitle(stringResource(R.string.routine_schedule_time))
        TimeEntryField(
            hour = state.hour,
            minute = state.minute,
            onHourChange = viewModel::updateHour,
            onMinuteChange = viewModel::updateMinute
        )

        when (state.frequency) {
            RoutineFrequency.WEEKLY -> {
                SectionTitle(stringResource(R.string.routine_day_of_week))
                DayOfWeekDropdown(state.dayOfWeek ?: Calendar.MONDAY, viewModel::updateDayOfWeek)
            }
            RoutineFrequency.MONTHLY -> {
                SectionTitle(stringResource(R.string.routine_day_of_month))
                OutlinedTextField(
                    value = (state.dayOfMonth ?: 1).toString(),
                    onValueChange = { v -> v.toIntOrNull()?.coerceIn(1, 31)?.let { viewModel.updateDayOfMonth(it) } },
                    label = { Text("Day (1–31)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            RoutineFrequency.YEARLY -> {
                SectionTitle(stringResource(R.string.routine_month))
                OutlinedTextField(
                    value = (state.month ?: Calendar.JANUARY).toString(),
                    onValueChange = { v -> v.toIntOrNull()?.coerceIn(Calendar.JANUARY, Calendar.DECEMBER)?.let { viewModel.updateMonth(it) } },
                    label = { Text("Month (0–11)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                SectionTitle(stringResource(R.string.routine_day_of_month))
                OutlinedTextField(
                    value = (state.day ?: 1).toString(),
                    onValueChange = { v -> v.toIntOrNull()?.coerceIn(1, 31)?.let { viewModel.updateDay(it) } },
                    label = { Text("Day (1–31)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            RoutineFrequency.DAILY -> { /* no extra schedule fields */ }
        }

        SectionTitle(stringResource(R.string.routine_visibility))
        VisibilityDropdown(state.visibilityMode, viewModel::updateVisibilityMode)

        SectionTitle(stringResource(R.string.routine_incomplete_tasks))
        TaskActionSection(
            action = state.incompleteAction,
            onActionChange = viewModel::updateIncompleteAction,
            moveToCategoryId = state.incompleteMoveToCategoryId,
            onMoveToChange = viewModel::updateIncompleteMoveTo,
            categories = state.categories
        )

        SectionTitle(stringResource(R.string.routine_completed_tasks))
        TaskActionSection(
            action = state.completedAction,
            onActionChange = viewModel::updateCompletedAction,
            moveToCategoryId = state.completedMoveToCategoryId,
            onMoveToChange = viewModel::updateCompletedMoveTo,
            categories = state.categories
        )

        Row(
            modifier = Modifier.padding(top = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { viewModel.save() }) {
                Text(stringResource(R.string.routine_save))
            }
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.routine_cancel))
            }
            if (!state.isNew) {
                TextButton(onClick = { viewModel.delete() }) {
                    Text(stringResource(R.string.routine_delete))
                }
            }
        }
    }
}

@Composable
private fun TimeEntryField(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit
) {
    var text by remember(hour, minute) { mutableStateOf("%02d:%02d".format(hour, minute)) }
    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            text = raw
            val parts = raw.split(":")
            val h = parts.getOrNull(0)?.trim()?.toIntOrNull()
            val m = parts.getOrNull(1)?.trim()?.toIntOrNull()
            if (h != null && h in 0..23) onHourChange(h)
            if (m != null && m in 0..59) onMinuteChange(m)
        },
        label = { Text("HH:MM") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun FrequencyDropdown(current: RoutineFrequency, onChange: (RoutineFrequency) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = current.name,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            RoutineFrequency.entries.forEach { f ->
                DropdownMenuItem(text = { Text(f.name) }, onClick = { onChange(f); expanded = false })
            }
        }
    }
}

@Composable
private fun DayOfWeekDropdown(current: Int, onChange: (Int?) -> Unit) {
    val days = listOf(
        Calendar.MONDAY to "Monday", Calendar.TUESDAY to "Tuesday",
        Calendar.WEDNESDAY to "Wednesday", Calendar.THURSDAY to "Thursday",
        Calendar.FRIDAY to "Friday", Calendar.SATURDAY to "Saturday",
        Calendar.SUNDAY to "Sunday"
    )
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = days.find { it.first == current }?.second ?: "",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            days.forEach { (dow, name) ->
                DropdownMenuItem(text = { Text(name) }, onClick = { onChange(dow); expanded = false })
            }
        }
    }
}

@Composable
private fun VisibilityDropdown(current: RoutineVisibilityMode, onChange: (RoutineVisibilityMode) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = current.name,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            RoutineVisibilityMode.entries.forEach { m ->
                DropdownMenuItem(text = { Text(m.name) }, onClick = { onChange(m); expanded = false })
            }
        }
    }
}

@Composable
private fun TaskActionSection(
    action: RoutineTaskAction,
    onActionChange: (RoutineTaskAction) -> Unit,
    moveToCategoryId: Long?,
    onMoveToChange: (Long?) -> Unit,
    categories: List<com.klecer.gottado.data.db.entity.CategoryEntity>
) {
    var actionExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = actionExpanded, onExpandedChange = { actionExpanded = it }) {
        OutlinedTextField(
            value = action.name,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(actionExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = actionExpanded, onDismissRequest = { actionExpanded = false }) {
            RoutineTaskAction.entries.forEach { a ->
                DropdownMenuItem(text = { Text(a.name) }, onClick = { onActionChange(a); actionExpanded = false })
            }
        }
    }
    if (action != RoutineTaskAction.DELETE) {
        var moveExpanded by remember { mutableStateOf(false) }
        val options = listOf(null to "Don't move") + categories.map { it.id to it.name }
        val selectedName = options.find { it.first == moveToCategoryId }?.second ?: "Don't move"
        ExposedDropdownMenuBox(expanded = moveExpanded, onExpandedChange = { moveExpanded = it }) {
            OutlinedTextField(
                value = selectedName,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.routine_move_to)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(moveExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor().padding(top = 4.dp)
            )
            ExposedDropdownMenu(expanded = moveExpanded, onDismissRequest = { moveExpanded = false }) {
                options.forEach { (id, name) ->
                    DropdownMenuItem(text = { Text(name) }, onClick = { onMoveToChange(id); moveExpanded = false })
                }
            }
        }
    }
}
