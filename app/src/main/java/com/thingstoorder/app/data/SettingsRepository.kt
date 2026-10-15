package com.thingstoorder.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore(name = "settings")

data class OnboardingResult(
    val reminder: ReminderMode?,
    val reminderHours: Int,
    val apps: Set<QcApp>,
    val widgetRequested: Boolean,
)

data class ReminderSetting(val mode: ReminderMode?, val hours: Int) {
    val label: String
        get() = when (mode) {
            null -> "Off"
            ReminderMode.Once -> "Once a day"
            ReminderMode.Twice -> "Twice a day"
            ReminderMode.Custom -> "Every $hours hours"
        }

    val times: String
        get() = when (mode) {
            null -> "None"
            ReminderMode.Once -> "9:00 am"
            ReminderMode.Twice -> "9:00 am · 6:00 pm"
            ReminderMode.Custom -> "8:00 am to 10:00 pm"
        }
}

class SettingsRepository(private val context: Context) {
    private object Keys {
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val reminderMode = stringPreferencesKey("reminder_mode")
        val reminderHours = intPreferencesKey("reminder_hours")
        val apps = stringSetPreferencesKey("watched_apps")
        val widgetRequested = booleanPreferencesKey("widget_requested")
        val watcherPaused = booleanPreferencesKey("watcher_paused")
        val widgetMinimized = booleanPreferencesKey("widget_minimized")
        val pillX = intPreferencesKey("pill_x")
        val pillY = intPreferencesKey("pill_y")
        val widgetTakeoverUntil = longPreferencesKey("widget_takeover_until")
        val sharedListId = stringPreferencesKey("shared_list_id")
        val sharedCode = stringPreferencesKey("shared_code")
        val myName = stringPreferencesKey("my_name")
        val orderBannerSeen = longPreferencesKey("order_banner_seen")
    }

    val sharedListId: Flow<String?> = context.settingsStore.data.map { it[Keys.sharedListId] }
    val sharedCode: Flow<String?> = context.settingsStore.data.map { it[Keys.sharedCode] }
    val myName: Flow<String?> = context.settingsStore.data.map { it[Keys.myName] }
    val orderBannerSeen: Flow<Long> = context.settingsStore.data.map { it[Keys.orderBannerSeen] ?: 0L }

    suspend fun setShared(listId: String?, code: String?) {
        context.settingsStore.edit { p: androidx.datastore.preferences.core.MutablePreferences ->
            if (listId == null) p.remove(Keys.sharedListId) else p[Keys.sharedListId] = listId
            if (code == null) p.remove(Keys.sharedCode) else p[Keys.sharedCode] = code
        }
    }

    suspend fun setMyName(name: String) {
        context.settingsStore.edit { it[Keys.myName] = name }
    }

    suspend fun setOrderBannerSeen(at: Long) {
        context.settingsStore.edit { it[Keys.orderBannerSeen] = at }
    }

    val widgetTakeoverUntil: Flow<Long?> = context.settingsStore.data.map { it[Keys.widgetTakeoverUntil] }

    suspend fun setWidgetTakeoverUntil(until: Long?) {
        context.settingsStore.edit { p: androidx.datastore.preferences.core.MutablePreferences ->
            if (until == null) p.remove(Keys.widgetTakeoverUntil) else p[Keys.widgetTakeoverUntil] = until
        }
    }

    val pillPosition: Flow<Pair<Int, Int>?> = context.settingsStore.data.map { p ->
        val x = p[Keys.pillX]
        val y = p[Keys.pillY]
        if (x != null && y != null) x to y else null
    }

    suspend fun setPillPosition(x: Int, y: Int) {
        context.settingsStore.edit {
            it[Keys.pillX] = x
            it[Keys.pillY] = y
        }
    }

    val widgetMinimized: Flow<Boolean> = context.settingsStore.data.map { it[Keys.widgetMinimized] ?: false }

    suspend fun setWidgetMinimized(minimized: Boolean) {
        context.settingsStore.edit { it[Keys.widgetMinimized] = minimized }
    }

    val watcherPaused: Flow<Boolean> = context.settingsStore.data.map { it[Keys.watcherPaused] ?: false }

    suspend fun setWatcherPaused(paused: Boolean) {
        context.settingsStore.edit { it[Keys.watcherPaused] = paused }
    }

    val onboardingDone: Flow<Boolean> = context.settingsStore.data.map { it[Keys.onboardingDone] ?: false }

    val watchedApps: Flow<Set<QcApp>> = context.settingsStore.data.map { p ->
        (p[Keys.apps] ?: emptySet()).mapNotNull { QcApp.fromId(it) }.toSet()
    }

    val reminder: Flow<ReminderSetting> = context.settingsStore.data.map { p ->
        ReminderSetting(
            mode = p[Keys.reminderMode]?.let { runCatching { ReminderMode.valueOf(it) }.getOrNull() },
            hours = p[Keys.reminderHours] ?: 4,
        )
    }

    suspend fun setReminder(mode: ReminderMode?, hours: Int) {
        context.settingsStore.edit { p: androidx.datastore.preferences.core.MutablePreferences ->
            if (mode == null) p.remove(Keys.reminderMode) else p[Keys.reminderMode] = mode.name
            p[Keys.reminderHours] = hours
        }
    }

    suspend fun setWatched(app: QcApp, watched: Boolean) {
        context.settingsStore.edit { p: androidx.datastore.preferences.core.MutablePreferences ->
            val current = (p[Keys.apps] ?: emptySet()).toMutableSet()
            if (watched) current.add(app.id) else current.remove(app.id)
            p[Keys.apps] = current
        }
    }

    suspend fun completeOnboarding(result: OnboardingResult) {
        context.settingsStore.edit { p: androidx.datastore.preferences.core.MutablePreferences ->
            p[Keys.onboardingDone] = true
            if (result.reminder != null) p[Keys.reminderMode] = result.reminder.name else p.remove(Keys.reminderMode)
            p[Keys.reminderHours] = result.reminderHours
            p[Keys.apps] = result.apps.map { it.id }.toSet()
            p[Keys.widgetRequested] = result.widgetRequested
        }
    }
}
