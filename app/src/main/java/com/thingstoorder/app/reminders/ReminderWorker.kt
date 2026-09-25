package com.thingstoorder.app.reminders

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.thingstoorder.app.data.AppDatabase
import com.thingstoorder.app.data.SettingsRepository
import kotlinx.coroutines.flow.first

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val remaining = AppDatabase.get(applicationContext).itemDao().getRemaining()
        Log.i(ReminderScheduler.TAG, "reminder due, ${remaining.size} remaining")
        ReminderNotifier.post(applicationContext, remaining)
        ReminderScheduler.schedule(applicationContext, SettingsRepository(applicationContext).reminder.first(), replace = true)
        return Result.success()
    }
}
