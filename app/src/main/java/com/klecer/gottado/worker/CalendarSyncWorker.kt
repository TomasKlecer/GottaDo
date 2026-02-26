package com.klecer.gottado.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.klecer.gottado.calendar.CalendarSyncManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class CalendarSyncWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val calendarSyncManager: CalendarSyncManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            calendarSyncManager.sync()
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "CalendarSyncWorker failed", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "CalendarSync"
        const val UNIQUE_WORK_NAME = "calendar_sync_periodic"
    }
}
