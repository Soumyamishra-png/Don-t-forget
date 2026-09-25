package com.thingstoorder.app.watcher

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.thingstoorder.app.data.AppDatabase
import com.thingstoorder.app.data.SettingsRepository
import com.thingstoorder.app.permissions.Permissions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

object WatcherController {
    private val recheck = MutableStateFlow(0)

    fun recheckPermissions() {
        recheck.value++
    }

    fun start(context: Context, scope: CoroutineScope) {
        val app = context.applicationContext
        val settings = SettingsRepository(app)
        val dao = AppDatabase.get(app).itemDao()

        scope.launch {
            combine(settings.watchedApps, dao.observeRemainingCount(), settings.watcherPaused, recheck) { apps, remaining, paused, _ ->
                apps.isNotEmpty() && remaining > 0 && !paused && Permissions.usageAccessGranted(app)
            }.distinctUntilChanged { old, new -> old == new }.collect { shouldRun ->
                val intent = Intent(app, WatcherService::class.java)
                if (shouldRun) {
                    try {
                        ContextCompat.startForegroundService(app, intent)
                    } catch (e: Exception) {
                        Log.w(WatcherService.TAG, "could not start watcher from background", e)
                    }
                } else {
                    app.stopService(intent)
                }
            }
        }
    }
}
