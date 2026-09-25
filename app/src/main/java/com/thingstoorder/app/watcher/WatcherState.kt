package com.thingstoorder.app.watcher

import com.thingstoorder.app.data.QcApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object WatcherState {
    private val _foregroundApp = MutableStateFlow<QcApp?>(null)
    val foregroundApp: StateFlow<QcApp?> = _foregroundApp

    fun set(app: QcApp?) {
        _foregroundApp.value = app
    }
}
