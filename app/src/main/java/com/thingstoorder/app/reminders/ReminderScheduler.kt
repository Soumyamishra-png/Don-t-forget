package com.thingstoorder.app.reminders

import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.thingstoorder.app.data.ReminderSetting
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private const val WORK_NAME = "reminder"
    const val TAG = "DFReminder"

    // replace = false keeps a pending job (and a job that is running right now) untouched.
    fun schedule(context: Context, setting: ReminderSetting, replace: Boolean) {
        val workManager = WorkManager.getInstance(context)
        val next = ReminderSchedule.next(setting, ZonedDateTime.now())
        if (next == null) {
            workManager.cancelUniqueWork(WORK_NAME)
            Log.i(TAG, "reminders off")
            return
        }
        enqueue(workManager, next, if (replace) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP)
    }

    fun snooze(context: Context) {
        enqueue(WorkManager.getInstance(context), ZonedDateTime.now().plusHours(1), ExistingWorkPolicy.REPLACE)
    }

    private fun enqueue(workManager: WorkManager, at: ZonedDateTime, policy: ExistingWorkPolicy) {
        val delayMs = Duration.between(ZonedDateTime.now(), at).toMillis().coerceAtLeast(0)
        Log.i(TAG, "next reminder at $at (in ${delayMs / 60000} min), policy=$policy")
        workManager.enqueueUniqueWork(
            WORK_NAME, policy,
            OneTimeWorkRequestBuilder<ReminderWorker>().setInitialDelay(delayMs, TimeUnit.MILLISECONDS).build(),
        )
    }
}
