package com.thingstoorder.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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

class SettingsRepository(private val context: Context) {
    private object Keys {
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val reminderMode = stringPreferencesKey("reminder_mode")
        val reminderHours = intPreferencesKey("reminder_hours")
        val apps = stringSetPreferencesKey("watched_apps")
        val widgetRequested = booleanPreferencesKey("widget_requested")
    }

    val onboardingDone: Flow<Boolean> = context.settingsStore.data.map { it[Keys.onboardingDone] ?: false }

    val watchedApps: Flow<Set<QcApp>> = context.settingsStore.data.map { p ->
        (p[Keys.apps] ?: emptySet()).mapNotNull { QcApp.fromId(it) }.toSet()
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
