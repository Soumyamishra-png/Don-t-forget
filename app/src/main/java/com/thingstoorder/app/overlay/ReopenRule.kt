package com.thingstoorder.app.overlay

object ReopenRule {
    const val WINDOW_MS = 15 * 60 * 1000L

    fun stayMinimized(userMinimized: Boolean, leftAtMs: Long, nowMs: Long): Boolean =
        userMinimized && nowMs - leftAtMs < WINDOW_MS
}
