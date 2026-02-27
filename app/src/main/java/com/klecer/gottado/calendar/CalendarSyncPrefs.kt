package com.klecer.gottado.calendar

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class SyncFrequency(val minutes: Long, val label: String) {
    MANUAL(0, "Manual only"),
    EVERY_15_MIN(15, "Every 15 minutes"),
    EVERY_30_MIN(30, "Every 30 minutes"),
    EVERY_1_HOUR(60, "Every hour"),
    EVERY_2_HOURS(120, "Every 2 hours");

    companion object {
        fun fromMinutes(m: Long): SyncFrequency =
            entries.find { it.minutes == m } ?: MANUAL
    }
}

@Singleton
class CalendarSyncPrefs @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("calendar_sync", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean("enabled", false)
        set(value) = prefs.edit().putBoolean("enabled", value).apply()

    var syncFrequencyMinutes: Long
        get() = prefs.getLong("sync_frequency_minutes", 0L)
        set(value) = prefs.edit().putLong("sync_frequency_minutes", value).apply()

    var lastSyncMillis: Long
        get() = prefs.getLong("last_sync_millis", 0L)
        set(value) = prefs.edit().putLong("last_sync_millis", value).apply()

    /** Days ahead to sync (0 = today only). Prepared for future expansion. */
    var daysAhead: Int
        get() = prefs.getInt("days_ahead", 0)
        set(value) = prefs.edit().putInt("days_ahead", value).apply()

    val frequency: SyncFrequency
        get() = SyncFrequency.fromMinutes(syncFrequencyMinutes)

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean("notifications_enabled", false)
        set(value) = prefs.edit().putBoolean("notifications_enabled", value).apply()
}
