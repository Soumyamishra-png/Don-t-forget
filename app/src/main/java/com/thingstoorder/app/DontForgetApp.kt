package com.thingstoorder.app

import android.app.Application
import androidx.glance.appwidget.updateAll
import com.thingstoorder.app.data.AppDatabase
import com.thingstoorder.app.data.SettingsRepository
import com.thingstoorder.app.overlay.OverlayController
import com.thingstoorder.app.watcher.WatcherController
import com.thingstoorder.app.widget.DontForgetWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

class DontForgetApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @OptIn(FlowPreview::class)
    override fun onCreate() {
        super.onCreate()
        WatcherController.start(this, appScope)
        OverlayController.start(this)

        val dao = AppDatabase.get(this).itemDao()
        val settings = SettingsRepository(this)
        appScope.launch {
            combine(dao.observeAll(), settings.widgetMinimized) { items, minimized -> items to minimized }
                .debounce(150)
                .collectLatest { DontForgetWidget().updateAll(this@DontForgetApp) }
        }
    }
}
