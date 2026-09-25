package com.thingstoorder.app.watcher

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.thingstoorder.app.R
import com.thingstoorder.app.data.QcApp
import com.thingstoorder.app.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class WatcherService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loop: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            scope.launch {
                SettingsRepository(applicationContext).setWatcherPaused(true)
                stopSelf()
            }
            return START_NOT_STICKY
        }

        createChannel()
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, buildNotification(emptySet()),
            if (android.os.Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
        )
        if (loop?.isActive != true) loop = scope.launch { runLoop() }
        return START_STICKY
    }

    private suspend fun runLoop() {
        val settings = SettingsRepository(applicationContext)
        val watched = settings.watchedApps.stateIn(scope, SharingStarted.Eagerly, emptySet())
        val detector = ForegroundDetector(applicationContext)
        var current: QcApp? = null
        var lastShownApps: Set<QcApp>? = null
        Log.i(TAG, "watcher started")

        while (scope.isActive) {
            val apps = watched.value
            if (apps != lastShownApps) {
                getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(apps))
                lastShownApps = apps
            }
            val pkg = try { detector.poll() } catch (e: SecurityException) { Log.w(TAG, "usage access missing", e); null }
            val match = QcApp.matching(pkg, apps)
            if (match != current) {
                if (current != null) Log.i(TAG, "leave ${current.label}")
                if (match != null) Log.i(TAG, "enter ${match.label} ($pkg)")
                current = match
                WatcherState.set(match)
            }
            delay(POLL_INTERVAL_MS)
        }
    }

    override fun onDestroy() {
        Log.i(TAG, "watcher stopped")
        WatcherState.set(null)
        scope.cancel()
        super.onDestroy()
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Watching shopping apps", NotificationManager.IMPORTANCE_MIN).apply {
                    description = "Shown while Don't Forget waits for you to open a shopping app."
                    setShowBadge(false)
                },
            )
        }
    }

    private fun buildNotification(apps: Set<QcApp>): Notification {
        val stop = PendingIntent.getService(
            this, 0, Intent(this, WatcherService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE,
        )
        val title = if (apps.isEmpty()) "Watching your shopping apps" else "Watching ${apps.joinToString { it.label }}"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_watch)
            .setContentTitle(title)
            .setContentText("Your list pops up when they open.")
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .addAction(0, "Stop", stop)
            .build()
    }

    companion object {
        const val ACTION_STOP = "com.thingstoorder.app.action.STOP_WATCHER"
        const val TAG = "DFWatcher"
        private const val CHANNEL_ID = "watcher"
        private const val NOTIFICATION_ID = 1
        private const val POLL_INTERVAL_MS = 1000L
    }
}
