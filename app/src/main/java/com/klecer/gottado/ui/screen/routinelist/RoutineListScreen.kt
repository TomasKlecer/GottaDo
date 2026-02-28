package com.klecer.gottado.ui.screen.routinelist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.klecer.gottado.R
import com.klecer.gottado.data.db.entity.RoutineEntity
import com.klecer.gottado.data.db.entity.RoutineFrequency
import java.util.Calendar

@Composable
fun RoutineListScreen(
    viewModel: RoutineListViewModel,
    onAddRoutine: (Long) -> Unit,
    onEditRoutine: (Long) -> Unit,
    onBack: () -> Unit
) {
    val routines by viewModel.routines.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = { onAddRoutine(viewModel.categoryId) },
            modifier = Modifier.padding(16.dp)
        ) {
            Text(stringResource(R.string.routine_add))
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(routines, key = { it.id }) { routine ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(routineSummaryText(routine), style = MaterialTheme.typography.bodyLarge)
                        }
                        Row {
                            TextButton(onClick = { onEditRoutine(routine.id) }) {
                                Text(stringResource(R.string.routine_edit))
                            }
                            TextButton(onClick = { viewModel.delete(routine.id) }) {
                                Text(stringResource(R.string.routine_delete))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun routineSummaryText(r: RoutineEntity): String {
    val time = "%02d:%02d".format(r.scheduleTimeHour, r.scheduleTimeMinute)
    return when (r.frequency) {
        RoutineFrequency.DAILY -> "${stringResource(R.string.routine_freq_daily)} $time"
        RoutineFrequency.WEEKLY -> {
            val day = dayNameLocalized(r.scheduleDayOfWeek ?: Calendar.MONDAY)
            "${stringResource(R.string.routine_freq_weekly)} $day $time"
        }
        RoutineFrequency.MONTHLY -> "${stringResource(R.string.routine_freq_monthly)} ${r.scheduleDayOfMonth ?: 1}. $time"
        RoutineFrequency.YEARLY -> "${stringResource(R.string.routine_freq_yearly)} ${monthNameLocalized(r.scheduleMonth ?: Calendar.JANUARY)} ${r.scheduleDay ?: 1} $time"
    }
}

@Composable
private fun dayNameLocalized(dow: Int): String = when (dow) {
    Calendar.SUNDAY -> stringResource(R.string.day_short_sun)
    Calendar.MONDAY -> stringResource(R.string.day_short_mon)
    Calendar.TUESDAY -> stringResource(R.string.day_short_tue)
    Calendar.WEDNESDAY -> stringResource(R.string.day_short_wed)
    Calendar.THURSDAY -> stringResource(R.string.day_short_thu)
    Calendar.FRIDAY -> stringResource(R.string.day_short_fri)
    Calendar.SATURDAY -> stringResource(R.string.day_short_sat)
    else -> "?"
}

@Composable
private fun monthNameLocalized(m: Int): String = when (m) {
    Calendar.JANUARY -> stringResource(R.string.month_short_jan)
    Calendar.FEBRUARY -> stringResource(R.string.month_short_feb)
    Calendar.MARCH -> stringResource(R.string.month_short_mar)
    Calendar.APRIL -> stringResource(R.string.month_short_apr)
    Calendar.MAY -> stringResource(R.string.month_short_may)
    Calendar.JUNE -> stringResource(R.string.month_short_jun)
    Calendar.JULY -> stringResource(R.string.month_short_jul)
    Calendar.AUGUST -> stringResource(R.string.month_short_aug)
    Calendar.SEPTEMBER -> stringResource(R.string.month_short_sep)
    Calendar.OCTOBER -> stringResource(R.string.month_short_oct)
    Calendar.NOVEMBER -> stringResource(R.string.month_short_nov)
    Calendar.DECEMBER -> stringResource(R.string.month_short_dec)
    else -> "?"
}
