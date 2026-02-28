package com.klecer.gottado.ui.screen.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klecer.gottado.calendar.CalendarSyncManager
import com.klecer.gottado.calendar.CalendarSyncPrefs
import com.klecer.gottado.calendar.CalendarSyncScheduler
import com.klecer.gottado.calendar.SyncFrequency
import com.klecer.gottado.ui.color.ColorPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AppLanguage(val code: String, val displayName: String) {
    SYSTEM("", "System default"),
    ENGLISH("en", "English"),
    CZECH("cs", "Čeština"),
    GERMAN("de", "Deutsch");
}

data class SettingsState(
    val calendarEnabled: Boolean = false,
    val syncFrequency: SyncFrequency = SyncFrequency.MANUAL,
    val hasPermission: Boolean = false,
    val lastSyncMillis: Long = 0L,
    val notificationsEnabled: Boolean = false,
    val hasNotificationPermission: Boolean = false,
    val language: AppLanguage = AppLanguage.SYSTEM
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val syncPrefs: CalendarSyncPrefs,
    private val syncScheduler: CalendarSyncScheduler,
    private val syncManager: CalendarSyncManager,
    val colorPrefs: ColorPrefs
) : ViewModel() {

    private val _colorPalettes = MutableStateFlow(loadPalettes())
    val colorPalettes: StateFlow<Map<String, List<Int>>> = _colorPalettes.asStateFlow()

    private fun loadPalettes() = mapOf(
        ColorPrefs.KEY_CATEGORY to colorPrefs.getPalette(ColorPrefs.KEY_CATEGORY),
        ColorPrefs.KEY_ENTRY to colorPrefs.getPalette(ColorPrefs.KEY_ENTRY),
        ColorPrefs.KEY_WIDGET_BG to colorPrefs.getPalette(ColorPrefs.KEY_WIDGET_BG),
        ColorPrefs.KEY_WIDGET_TEXT to colorPrefs.getPalette(ColorPrefs.KEY_WIDGET_TEXT)
    )

    fun updatePaletteColor(key: String, index: Int, color: Int) {
        colorPrefs.setColor(key, index, color)
        _colorPalettes.value = loadPalettes()
    }

    private val _state = MutableStateFlow(loadState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    private fun loadState(): SettingsState {
        val locales = AppCompatDelegate.getApplicationLocales()
        val currentLang = if (locales.isEmpty) {
            AppLanguage.SYSTEM
        } else {
            val tag = locales.get(0)?.language ?: ""
            AppLanguage.entries.find { it.code == tag } ?: AppLanguage.SYSTEM
        }
        return SettingsState(
            calendarEnabled = syncPrefs.enabled,
            syncFrequency = syncPrefs.frequency,
            hasPermission = syncManager.hasCalendarPermission(),
            lastSyncMillis = syncPrefs.lastSyncMillis,
            notificationsEnabled = syncPrefs.notificationsEnabled,
            hasNotificationPermission = hasNotificationPermission(),
            language = currentLang
        )
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        } else true
    }

    fun refreshPermission() {
        _state.value = _state.value.copy(
            hasPermission = syncManager.hasCalendarPermission(),
            hasNotificationPermission = hasNotificationPermission()
        )
    }

    fun refreshNotificationPermission() {
        _state.value = _state.value.copy(hasNotificationPermission = hasNotificationPermission())
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        syncPrefs.notificationsEnabled = enabled
        _state.value = _state.value.copy(notificationsEnabled = enabled)
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

    fun setLanguage(lang: AppLanguage) {
        _state.value = _state.value.copy(language = lang)
        val localeList = if (lang == AppLanguage.SYSTEM) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(lang.code)
        }
        AppCompatDelegate.setApplicationLocales(localeList)
    }
}
