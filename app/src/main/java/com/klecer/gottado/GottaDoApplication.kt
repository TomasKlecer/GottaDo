package com.klecer.gottado

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.klecer.gottado.calendar.CalendarSyncScheduler
import com.klecer.gottado.worker.RoutineWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class GottaDoApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var calendarSyncScheduler: CalendarSyncScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        enqueueRoutineWorker()
        calendarSyncScheduler.scheduleFromPrefs()
    }

    private fun enqueueRoutineWorker() {
        val wm = WorkManager.getInstance(this)
        val periodic = PeriodicWorkRequestBuilder<RoutineWorker>(
            15, TimeUnit.MINUTES
        ).build()
        wm.enqueueUniquePeriodicWork(
            RoutineWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodic
        )
        wm.enqueue(OneTimeWorkRequestBuilder<RoutineWorker>().build())
    }
}
