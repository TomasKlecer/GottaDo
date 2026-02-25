package com.klecer.gottado.ui.screen.recordeditoptions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.klecer.gottado.R
import kotlinx.coroutines.launch

@Composable
fun RecordEditOptionsScreen(
    viewModel: RecordEditOptionsViewModel,
    onBack: () -> Unit
) {
    val options by viewModel.options.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            stringResource(R.string.record_edit_options_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RowWithCheckboxAndInfo(
                checked = options.showRichText,
                onCheckedChange = viewModel::updateShowRichText,
                label = stringResource(R.string.record_edit_options_rich_text),
                info = stringResource(R.string.info_opt_rich_text)
            )
            RowWithCheckboxAndInfo(
                checked = options.showTimeField,
                onCheckedChange = viewModel::updateShowTimeField,
                label = stringResource(R.string.record_edit_options_time),
                info = stringResource(R.string.info_opt_time)
            )
            RowWithCheckboxAndInfo(
                checked = options.showCategoryDropdown,
                onCheckedChange = viewModel::updateShowCategoryDropdown,
                label = stringResource(R.string.record_edit_options_category),
                info = stringResource(R.string.info_opt_category)
            )
            RowWithCheckboxAndInfo(
                checked = options.showBulletColor,
                onCheckedChange = viewModel::updateShowBulletColor,
                label = stringResource(R.string.record_edit_options_bullet_color),
                info = stringResource(R.string.info_opt_bullet_color)
            )
            RowWithCheckboxAndInfo(
                checked = options.useUnifiedColorPicker,
                onCheckedChange = viewModel::updateUseUnifiedColorPicker,
                label = stringResource(R.string.record_edit_options_unified_color),
                info = stringResource(R.string.info_opt_unified_color)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RowWithCheckboxAndInfo(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    info: String
) {
    val tooltipState = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.width(4.dp))
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
}
