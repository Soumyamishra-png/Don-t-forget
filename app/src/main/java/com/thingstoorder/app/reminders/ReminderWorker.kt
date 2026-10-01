package com.thingstoorder.app.reminders

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.thingstoorder.app.data.AppDatabase
import com.thingstoorder.app.data.SettingsRepository
import com.thingstoorder.app.widget.DontForgetWidget
import kotlinx.coroutines.flow.first

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val dao = AppDatabase.get(applicationContext).itemDao()
        val remaining = dao.getRemaining()
        Log.i(ReminderScheduler.TAG, "reminder due, ${remaining.size} remaining")
        ReminderNotifier.post(applicationContext, remaining)

        val settings = SettingsRepository(applicationContext)
        settings.setWidgetTakeoverUntil(System.currentTimeMillis() + TAKEOVER_DURATION_MS)
        DontForgetWidget().updateAll(applicationContext)

        ReminderScheduler.schedule(applicationContext, settings.reminder.first(), replace = true)
        return Result.success()
    }

    private companion object {
        const val TAKEOVER_DURATION_MS = 3 * 60 * 60 * 1000L
    }
}
