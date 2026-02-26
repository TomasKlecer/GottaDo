package com.klecer.gottado.ui.screen.trash

import android.os.Build
import android.text.Html
import android.text.TextUtils
import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.klecer.gottado.R
import com.klecer.gottado.data.db.entity.TrashEntryEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TrashScreen(
    viewModel: TrashViewModel,
    onBack: () -> Unit
) {
    val historyState by viewModel.state.collectAsState()
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text(stringResource(R.string.history_delete_all_title)) },
            text = { Text(stringResource(R.string.history_delete_all_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.emptyTrash()
                        showDeleteAllDialog = false
                    }
                ) {
                    Text(
                        stringResource(R.string.history_delete_all_confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text(stringResource(R.string.history_delete_all_cancel))
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.history_view_by_category),
                style = MaterialTheme.typography.bodyMedium
            )
            Switch(
                checked = historyState.viewMode == HistoryViewMode.BY_TIME,
                onCheckedChange = { checked ->
                    viewModel.setViewMode(
                        if (checked) HistoryViewMode.BY_TIME else HistoryViewMode.BY_CATEGORY
                    )
                }
            )
            Text(
                stringResource(R.string.history_view_by_time),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        HorizontalDivider()

        if (historyState.entries.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    stringResource(R.string.history_no_entries),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                when (historyState.viewMode) {
                    HistoryViewMode.BY_CATEGORY -> categoryView(historyState, viewModel)
                    HistoryViewMode.BY_TIME -> timeView(historyState, viewModel)
                }

                item(key = "delete_all_spacer") {
                    Spacer(modifier = Modifier.height(24.dp))
                }
                item(key = "delete_all_button") {
                    Button(
                        onClick = { showDeleteAllDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(stringResource(R.string.trash_empty))
                    }
                }
                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.categoryView(
    state: HistoryState,
    viewModel: TrashViewModel
) {
    state.categoryGroups.forEach { group ->
        item(key = "cat_header_${group.categoryName}") {
            CollapsibleHeader(
                title = group.categoryName,
                defaultExpanded = true,
                headerStyle = HeaderStyle.CATEGORY
            ) {
                Column {
                    group.timeGroups.forEachIndexed { tIdx, timeGroup ->
                        if (tIdx > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 6.dp),
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                        Text(
                            text = timeGroup.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        timeGroup.entries.forEach { entry ->
                            TrashEntryRow(
                                entry = entry,
                                onRestore = { viewModel.restore(entry.id) },
                                onDelete = { viewModel.deletePermanently(entry.id) },
                                onSaveContent = { newContent ->
                                    viewModel.updateContent(entry.id, newContent)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.timeView(
    state: HistoryState,
    viewModel: TrashViewModel
) {
    state.yearGroups.forEach { yearGroup ->
        item(key = "year_${yearGroup.year}") {
            CollapsibleHeader(
                title = yearGroup.year.toString(),
                defaultExpanded = true,
                headerStyle = HeaderStyle.YEAR
            ) {
                Column {
                    yearGroup.months.forEach { monthGroup ->
                        CollapsibleHeader(
                            title = monthGroup.label,
                            defaultExpanded = true,
                            headerStyle = HeaderStyle.MONTH
                        ) {
                            Column {
                                monthGroup.weeks.forEachIndexed { wIdx, weekGroup ->
                                    if (wIdx > 0) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 6.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant
                                        )
                                    }
                                    Text(
                                        text = weekGroup.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                    weekGroup.entries.forEach { entry ->
                                        TrashEntryRow(
                                            entry = entry,
                                            onRestore = { viewModel.restore(entry.id) },
                                            onDelete = { viewModel.deletePermanently(entry.id) },
                                            onSaveContent = { newContent ->
                                                viewModel.updateContent(entry.id, newContent)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class HeaderStyle { CATEGORY, YEAR, MONTH }

@Composable
private fun CollapsibleHeader(
    title: String,
    defaultExpanded: Boolean,
    headerStyle: HeaderStyle,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(defaultExpanded) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = when (headerStyle) {
                    HeaderStyle.CATEGORY -> MaterialTheme.typography.titleMedium
                    HeaderStyle.YEAR -> MaterialTheme.typography.titleLarge
                    HeaderStyle.MONTH -> MaterialTheme.typography.titleSmall
                },
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(start = 8.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun TrashEntryRow(
    entry: TrashEntryEntity,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    onSaveContent: (String) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var editText by remember(entry.contentHtml) { mutableStateOf(stripHtml(entry.contentHtml)) }
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(start = 12.dp, top = 8.dp, end = 4.dp, bottom = 4.dp)) {
            if (isEditing) {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.history_edit)) }
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = {
                        onSaveContent(htmlFromPlain(editText))
                        isEditing = false
                    }) {
                        Text(stringResource(R.string.history_save))
                    }
                    TextButton(onClick = {
                        editText = stripHtml(entry.contentHtml)
                        isEditing = false
                    }) {
                        Text(stringResource(R.string.history_delete_all_cancel))
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    AndroidView(
                        modifier = Modifier.weight(1f),
                        factory = { ctx ->
                            TextView(ctx).apply {
                                textSize = 14f
                                setTextColor(textColor)
                            }
                        },
                        update = { tv ->
                            tv.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                Html.fromHtml(entry.contentHtml, Html.FROM_HTML_MODE_LEGACY)
                            } else {
                                @Suppress("DEPRECATION")
                                Html.fromHtml(entry.contentHtml)
                            }
                        }
                    )
                    IconButton(
                        onClick = onRestore,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Restore",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { isEditing = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete permanently",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Text(
                text = "${entry.categoryName} — ${timeFormat.format(Date(entry.deletedAtMillis))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
            )
        }
    }
}

private fun stripHtml(html: String): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString()
    } else {
        @Suppress("DEPRECATION")
        Html.fromHtml(html).toString()
    }.trim()
}

private fun htmlFromPlain(text: String): String {
    return text.lines().joinToString("<br>") { TextUtils.htmlEncode(it) }
}

private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
