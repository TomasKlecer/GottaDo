package com.klecer.gottado.calendar

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.klecer.gottado.worker.CalendarSyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncPrefs: CalendarSyncPrefs
) {
    fun scheduleFromPrefs() {
        val freq = syncPrefs.frequency
        val wm = WorkManager.getInstance(context)
        if (!syncPrefs.enabled || freq == SyncFrequency.MANUAL) {
            wm.cancelUniqueWork(CalendarSyncWorker.UNIQUE_WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<CalendarSyncWorker>(
            freq.minutes, TimeUnit.MINUTES
        ).build()
        wm.enqueueUniquePeriodicWork(
            CalendarSyncWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun syncNow() {
        WorkManager.getInstance(context)
            .enqueue(OneTimeWorkRequestBuilder<CalendarSyncWorker>().build())
    }
}
