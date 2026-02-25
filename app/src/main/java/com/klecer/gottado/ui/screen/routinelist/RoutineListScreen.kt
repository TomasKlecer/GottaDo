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
                            Text(routineSummary(routine), style = MaterialTheme.typography.bodyLarge)
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

private fun routineSummary(r: RoutineEntity): String {
    val time = "%02d:%02d".format(r.scheduleTimeHour, r.scheduleTimeMinute)
    return when (r.frequency) {
        RoutineFrequency.DAILY -> "Daily at $time"
        RoutineFrequency.WEEKLY -> {
            val day = dayName(r.scheduleDayOfWeek ?: Calendar.MONDAY)
            "Weekly $day at $time"
        }
        RoutineFrequency.MONTHLY -> "Monthly day ${r.scheduleDayOfMonth ?: 1} at $time"
        RoutineFrequency.YEARLY -> "Yearly ${r.scheduleMonth?.let { monthName(it) } ?: ""} ${r.scheduleDay ?: 1} at $time"
    }
}

private fun dayName(dow: Int): String = when (dow) {
    Calendar.SUNDAY -> "Sun"
    Calendar.MONDAY -> "Mon"
    Calendar.TUESDAY -> "Tue"
    Calendar.WEDNESDAY -> "Wed"
    Calendar.THURSDAY -> "Thu"
    Calendar.FRIDAY -> "Fri"
    Calendar.SATURDAY -> "Sat"
    else -> "?"
}

private fun monthName(m: Int): String = when (m) {
    Calendar.JANUARY -> "Jan"; Calendar.FEBRUARY -> "Feb"; Calendar.MARCH -> "Mar"
    Calendar.APRIL -> "Apr"; Calendar.MAY -> "May"; Calendar.JUNE -> "Jun"
    Calendar.JULY -> "Jul"; Calendar.AUGUST -> "Aug"; Calendar.SEPTEMBER -> "Sep"
    Calendar.OCTOBER -> "Oct"; Calendar.NOVEMBER -> "Nov"; Calendar.DECEMBER -> "Dec"
    else -> "?"
}
