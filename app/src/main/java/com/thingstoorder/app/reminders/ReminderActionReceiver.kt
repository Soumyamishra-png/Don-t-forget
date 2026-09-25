package com.thingstoorder.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ReminderNotifier.ACTION_SNOOZE) {
            ReminderNotifier.cancel(context)
            ReminderScheduler.snooze(context)
        }
    }
}
