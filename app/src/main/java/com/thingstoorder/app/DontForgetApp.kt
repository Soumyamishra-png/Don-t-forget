package com.thingstoorder.app

import android.app.Application
import com.thingstoorder.app.watcher.WatcherController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class DontForgetApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        WatcherController.start(this, appScope)
    }
}
