package com.thingstoorder.app.watcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// The Application class starts WatcherController when this broadcast wakes the process.
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Unit
}
