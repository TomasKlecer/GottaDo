@file:OptIn(ExperimentalMaterial3Api::class)

package com.klecer.gottado.ui.screen.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.klecer.gottado.R
import com.klecer.gottado.calendar.SyncFrequency
import com.klecer.gottado.ui.color.ColorPrefs
import com.klecer.gottado.ui.color.HsvColorPickerDialog
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

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.refreshNotificationPermission()
        if (granted && !state.notificationsEnabled) {
            viewModel.setNotificationsEnabled(true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        SettingsSectionHeader(
            title = stringResource(R.string.settings_calendar_section),
            infoText = stringResource(R.string.info_settings_calendar_sync)
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
        } else {
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
        }

        if (state.hasPermission && state.calendarEnabled) {
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
                    value = syncFrequencyLabel(state.syncFrequency),
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
                            text = { Text(syncFrequencyLabel(freq)) },
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

        Spacer(Modifier.size(24.dp))

        SettingsSectionHeader(
            title = stringResource(R.string.settings_notifications_section),
            infoText = stringResource(R.string.info_settings_notifications)
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !state.hasNotificationPermission) {
            Text(
                stringResource(R.string.settings_notifications_permission),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Button(onClick = { notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                Text(stringResource(R.string.settings_notifications_grant_permission))
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = state.notificationsEnabled,
                    onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                )
                Column {
                    Text(
                        stringResource(R.string.settings_notifications_enable),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        stringResource(R.string.settings_notifications_enable_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.size(24.dp))

        Text(
            stringResource(R.string.settings_language_section),
            style = MaterialTheme.typography.titleMedium
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        var langExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = langExpanded,
            onExpandedChange = { langExpanded = it }
        ) {
            OutlinedTextField(
                value = state.language.displayName,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = langExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = langExpanded,
                onDismissRequest = { langExpanded = false }
            ) {
                AppLanguage.entries.forEach { lang ->
                    DropdownMenuItem(
                        text = { Text(lang.displayName) },
                        onClick = {
                            viewModel.setLanguage(lang)
                            langExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.size(24.dp))
        ColorPickersSection(viewModel)
    }
}

private val PALETTE_ENTRIES = listOf(
    ColorPrefs.KEY_CATEGORY to R.string.settings_palette_category,
    ColorPrefs.KEY_ENTRY to R.string.settings_palette_entry,
    ColorPrefs.KEY_WIDGET_BG to R.string.settings_palette_widget_bg,
    ColorPrefs.KEY_WIDGET_TEXT to R.string.settings_palette_widget_text
)

@Composable
private fun ColorPickersSection(viewModel: SettingsViewModel) {
    val palettes by viewModel.colorPalettes.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    var editingKey by remember { mutableStateOf<String?>(null) }
    var editingIndex by remember { mutableStateOf(0) }

    val colorInfoTooltipState = rememberTooltipState()
    val colorInfoScope = rememberCoroutineScope()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            stringResource(R.string.settings_color_pickers_section),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        TooltipBox(
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
            tooltip = { PlainTooltip { Text(stringResource(R.string.info_settings_color_pickers)) } },
            state = colorInfoTooltipState
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { colorInfoScope.launch { colorInfoTooltipState.show() } }
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null
        )
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

    AnimatedVisibility(visible = expanded) {
        Column {
            PALETTE_ENTRIES.forEach { (key, labelRes) ->
                val colors = palettes[key] ?: emptyList()
                Text(
                    stringResource(labelRes),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    colors.forEachIndexed { index, color ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(Color(color))
                                .border(1.dp, Color.LightGray, CircleShape)
                                .clickable {
                                    editingKey = key
                                    editingIndex = index
                                }
                        )
                    }
                }
            }
        }
    }

    if (editingKey != null) {
        val key = editingKey!!
        val colors = palettes[key] ?: emptyList()
        val defaultColors = when (key) {
            ColorPrefs.KEY_CATEGORY -> ColorPrefs.DEFAULT_CATEGORY
            ColorPrefs.KEY_ENTRY -> ColorPrefs.DEFAULT_ENTRY
            ColorPrefs.KEY_WIDGET_BG -> ColorPrefs.DEFAULT_WIDGET_BG
            ColorPrefs.KEY_WIDGET_TEXT -> ColorPrefs.DEFAULT_WIDGET_TEXT
            else -> ColorPrefs.DEFAULT_CATEGORY
        }
        HsvColorPickerDialog(
            currentColor = colors.getOrElse(editingIndex) { 0xFFFFFFFF.toInt() },
            defaultColors = defaultColors,
            onColorSelected = { color -> viewModel.updatePaletteColor(key, editingIndex, color) },
            onDismiss = { editingKey = null }
        )
    }
}

@Composable
private fun syncFrequencyLabel(f: SyncFrequency): String = when (f) {
    SyncFrequency.MANUAL -> stringResource(R.string.sync_freq_manual)
    SyncFrequency.EVERY_15_MIN -> stringResource(R.string.sync_freq_15min)
    SyncFrequency.EVERY_30_MIN -> stringResource(R.string.sync_freq_30min)
    SyncFrequency.EVERY_1_HOUR -> stringResource(R.string.sync_freq_1hour)
    SyncFrequency.EVERY_2_HOURS -> stringResource(R.string.sync_freq_2hours)
}

@Composable
private fun SettingsSectionHeader(title: String, infoText: String) {
    val tooltipState = rememberTooltipState()
    val scope = rememberCoroutineScope()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        TooltipBox(
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
            tooltip = { PlainTooltip { Text(infoText) } },
            state = tooltipState
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { scope.launch { tooltipState.show() } }
            )
        }
    }
}
