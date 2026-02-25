package com.klecer.gottado.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.klecer.gottado.domain.usecase.ExecuteRoutinesDueUseCase
import com.klecer.gottado.widget.WidgetUpdateHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class RoutineWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val executeRoutinesDueUseCase: ExecuteRoutinesDueUseCase
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val affectedWidgetIds = executeRoutinesDueUseCase()
            if (affectedWidgetIds.isNotEmpty()) {
                for (widgetId in affectedWidgetIds) {
                    WidgetUpdateHelper.update(appContext, widgetId)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "routine_worker"
    }
}
