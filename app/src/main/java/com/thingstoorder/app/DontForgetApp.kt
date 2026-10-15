package com.thingstoorder.app

import android.app.Application
import androidx.glance.appwidget.updateAll
import com.thingstoorder.app.data.AppDatabase
import com.thingstoorder.app.data.SettingsRepository
import com.thingstoorder.app.data.shared.SharedLists
import com.thingstoorder.app.overlay.OverlayController
import com.thingstoorder.app.reminders.ReminderScheduler
import com.thingstoorder.app.watcher.WatcherController
import com.thingstoorder.app.widget.DontForgetWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class DontForgetApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @OptIn(FlowPreview::class)
    override fun onCreate() {
        super.onCreate()
        SharedLists.get(this).start()
        WatcherController.start(this, appScope)
        OverlayController.start(this)

        var firstReminder = true
        appScope.launch {
            SettingsRepository(this@DontForgetApp).reminder.distinctUntilChanged().collect { setting ->
                ReminderScheduler.schedule(this@DontForgetApp, setting, replace = !firstReminder)
                firstReminder = false
            }
        }

        val dao = AppDatabase.get(this).itemDao()
        val settings = SettingsRepository(this)
        appScope.launch {
            combine(dao.observeAll(), settings.widgetMinimized) { items, minimized -> items to minimized }
                .debounce(150)
                .collectLatest { DontForgetWidget().updateAll(this@DontForgetApp) }
        }
    }
}
