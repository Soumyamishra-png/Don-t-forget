package com.thingstoorder.app.watcher

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

class ForegroundDetector(context: Context) {
    private val usageStats = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private var foregroundPackage: String? = null
    private var lastQueryEnd = 0L

    @Suppress("DEPRECATION")
    fun poll(now: Long = System.currentTimeMillis()): String? {
        val begin = if (lastQueryEnd == 0L) now - INITIAL_LOOKBACK_MS else lastQueryEnd - OVERLAP_MS
        val events = usageStats.queryEvents(begin, now)
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) foregroundPackage = event.packageName
        }
        lastQueryEnd = now
        return foregroundPackage
    }

    private companion object {
        const val INITIAL_LOOKBACK_MS = 10 * 60 * 1000L
        const val OVERLAP_MS = 1000L
    }
}
