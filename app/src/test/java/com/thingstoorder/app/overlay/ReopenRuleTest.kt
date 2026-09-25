package com.thingstoorder.app.overlay

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReopenRuleTest {
    private val minute = 60_000L

    @Test
    fun staysMinimizedWhenBackWithinFifteenMinutes() {
        assertTrue(ReopenRule.stayMinimized(userMinimized = true, leftAtMs = 0, nowMs = 14 * minute))
    }

    @Test
    fun expandsAgainAfterFifteenMinutes() {
        assertFalse(ReopenRule.stayMinimized(userMinimized = true, leftAtMs = 0, nowMs = 15 * minute))
        assertFalse(ReopenRule.stayMinimized(userMinimized = true, leftAtMs = 0, nowMs = 60 * minute))
    }

    @Test
    fun neverStaysMinimizedIfTheUserDidNotMinimizeIt() {
        assertFalse(ReopenRule.stayMinimized(userMinimized = false, leftAtMs = 0, nowMs = minute))
    }
}
