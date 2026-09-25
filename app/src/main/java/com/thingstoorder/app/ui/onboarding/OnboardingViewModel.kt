package com.thingstoorder.app.ui.onboarding

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.thingstoorder.app.data.AppDatabase
import com.thingstoorder.app.data.Item
import com.thingstoorder.app.data.OnboardingResult
import com.thingstoorder.app.data.QcApp
import com.thingstoorder.app.data.ReminderMode
import com.thingstoorder.app.data.SettingsRepository
import kotlinx.coroutines.launch

enum class Step { Welcome, HowItWorks, AddItems, Reminder, Widget, Notifications, ConnectApps, UsageAccess, Overlay, AllSet }

class OnboardingViewModel(app: Application) : AndroidViewModel(app) {
    private val history = mutableStateListOf<Step>()

    var step by mutableStateOf(Step.Welcome)
        private set
    val canGoBack: Boolean get() = history.isNotEmpty()

    val draftItems = mutableStateListOf<String>()
    var listSaved by mutableStateOf(false)

    var reminder by mutableStateOf<ReminderMode?>(null)
    var reminderHours by mutableIntStateOf(4)
    var reminderSet by mutableStateOf(false)

    var widgetRequested by mutableStateOf(false)

    val apps = mutableStateListOf<QcApp>()

    fun goTo(next: Step) {
        history.add(step)
        step = next
    }

    fun next() = goTo(Step.entries[step.ordinal + 1])

    fun back() {
        if (history.isNotEmpty()) step = history.removeAt(history.lastIndex)
    }

    fun addItem(name: String): Boolean {
        val clean = name.trim()
        if (clean.isEmpty()) return false
        draftItems.add(clean)
        return true
    }

    fun removeItem(index: Int) {
        draftItems.removeAt(index)
    }

    fun saveList() {
        listSaved = true
        next()
    }

    fun skipList() {
        listSaved = false
        next()
    }

    fun setReminder() {
        reminderSet = true
        next()
    }

    fun skipReminder() {
        reminderSet = false
        next()
    }

    fun selectReminder(mode: ReminderMode) {
        reminder = mode
    }

    fun toggleApp(app: QcApp) {
        if (app.comingSoon) return
        if (app in apps) apps.remove(app) else apps.add(app)
    }

    fun connectApps() = goTo(Step.UsageAccess)

    fun skipApps() {
        apps.clear()
        goTo(Step.AllSet)
    }

    fun finish(onDone: () -> Unit) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            if (listSaved && draftItems.isNotEmpty()) {
                val dao = AppDatabase.get(context).itemDao()
                draftItems.forEach { dao.insert(Item(name = it)) }
            }
            SettingsRepository(context).completeOnboarding(
                OnboardingResult(
                    reminder = if (reminderSet) reminder else null,
                    reminderHours = reminderHours,
                    apps = apps.toSet(),
                    widgetRequested = widgetRequested,
                ),
            )
            onDone()
        }
    }
}
