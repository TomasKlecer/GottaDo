package com.klecer.gottado.ui.screen.widgetlist

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.klecer.gottado.R
import com.klecer.gottado.widget.GottaDoWidgetProvider

@Composable
fun WidgetListScreen(
    viewModel: WidgetListViewModel,
    onWidgetClick: (Int) -> Unit
) {
    val widgets by viewModel.widgets.collectAsState()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Column {
                Text(
                    text = stringResource(R.string.widget_list_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (widgets.isEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Button(
                        modifier = Modifier.padding(top = 8.dp),
                        onClick = {
                            val appWidgetManager = context.getSystemService(Context.APPWIDGET_SERVICE) as AppWidgetManager
                            val provider = ComponentName(context, GottaDoWidgetProvider::class.java)
                            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                                appWidgetManager.requestPinAppWidget(provider, null, null)
                            }
                        }
                    ) {
                        Text(stringResource(R.string.widget_list_add_widget))
                    }
                }
            }
        }
        items(widgets, key = { it.widgetId }) { config ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onWidgetClick(config.widgetId) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val title = config.title?.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.widget_list_item_default, config.widgetId)
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    config.subtitle?.takeIf { it.isNotBlank() }?.let { sub ->
                        Text(
                            text = sub,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
