package com.klecer.gottado.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TaskNotificationReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TITLE = "notif_title"
        const val EXTRA_TEXT = "notif_text"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "GottaDo"
        val text = intent.getStringExtra(EXTRA_TEXT) ?: ""
        if (taskId == -1L) return
        NotificationHelper.show(context, taskId.toInt(), title, text)
    }
}
