@file:OptIn(ExperimentalMaterial3Api::class)

package com.klecer.gottado.ui.screen.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.klecer.gottado.R
import com.klecer.gottado.calendar.SyncFrequency
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val state by viewModel.state.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.refreshPermission()
        if (granted && !state.calendarEnabled) {
            viewModel.setCalendarEnabled(true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            stringResource(R.string.settings_calendar_section),
            style = MaterialTheme.typography.titleMedium
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        if (!state.hasPermission) {
            Text(
                stringResource(R.string.settings_calendar_permission),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Button(onClick = { permissionLauncher.launch(Manifest.permission.READ_CALENDAR) }) {
                Text(stringResource(R.string.settings_calendar_grant_permission))
            }
            return
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = state.calendarEnabled,
                onCheckedChange = { viewModel.setCalendarEnabled(it) }
            )
            Column {
                Text(
                    stringResource(R.string.settings_calendar_enable),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    stringResource(R.string.settings_calendar_enable_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (state.calendarEnabled) {
            Spacer(Modifier.size(12.dp))

            Text(
                stringResource(R.string.settings_calendar_frequency),
                style = MaterialTheme.typography.titleSmall
            )

            var freqExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = freqExpanded,
                onExpandedChange = { freqExpanded = it }
            ) {
                OutlinedTextField(
                    value = state.syncFrequency.label,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = freqExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = freqExpanded,
                    onDismissRequest = { freqExpanded = false }
                ) {
                    SyncFrequency.entries.forEach { freq ->
                        DropdownMenuItem(
                            text = { Text(freq.label) },
                            onClick = {
                                viewModel.setSyncFrequency(freq)
                                freqExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.size(16.dp))

            Button(onClick = { viewModel.syncNow() }) {
                Text(stringResource(R.string.settings_calendar_sync_now))
            }

            Spacer(Modifier.size(8.dp))

            val lastSync = if (state.lastSyncMillis > 0) {
                val fmt = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                stringResource(R.string.settings_calendar_last_sync, fmt.format(Date(state.lastSyncMillis)))
            } else {
                stringResource(R.string.settings_calendar_last_sync_never)
            }
            Text(lastSync, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
