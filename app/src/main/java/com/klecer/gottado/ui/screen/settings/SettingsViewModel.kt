package com.klecer.gottado.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klecer.gottado.calendar.CalendarSyncManager
import com.klecer.gottado.calendar.CalendarSyncPrefs
import com.klecer.gottado.calendar.CalendarSyncScheduler
import com.klecer.gottado.calendar.SyncFrequency
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val calendarEnabled: Boolean = false,
    val syncFrequency: SyncFrequency = SyncFrequency.MANUAL,
    val hasPermission: Boolean = false,
    val lastSyncMillis: Long = 0L
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val syncPrefs: CalendarSyncPrefs,
    private val syncScheduler: CalendarSyncScheduler,
    private val syncManager: CalendarSyncManager
) : ViewModel() {

    private val _state = MutableStateFlow(loadState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private fun loadState() = SettingsState(
        calendarEnabled = syncPrefs.enabled,
        syncFrequency = syncPrefs.frequency,
        hasPermission = syncManager.hasCalendarPermission(),
        lastSyncMillis = syncPrefs.lastSyncMillis
    )

    fun refreshPermission() {
        _state.value = _state.value.copy(hasPermission = syncManager.hasCalendarPermission())
    }

    fun setCalendarEnabled(enabled: Boolean) {
        syncPrefs.enabled = enabled
        _state.value = _state.value.copy(calendarEnabled = enabled)
        syncScheduler.scheduleFromPrefs()
    }

    fun setSyncFrequency(freq: SyncFrequency) {
        syncPrefs.syncFrequencyMinutes = freq.minutes
        _state.value = _state.value.copy(syncFrequency = freq)
        syncScheduler.scheduleFromPrefs()
    }

    fun syncNow() {
        viewModelScope.launch {
            syncManager.sync()
            _state.value = _state.value.copy(lastSyncMillis = syncPrefs.lastSyncMillis)
        }
    }
}
