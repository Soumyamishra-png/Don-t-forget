package com.thingstoorder.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StaleTierTest {
    private val day = 24 * 60 * 60 * 1000L
    private val now = 10 * day // arbitrary epoch far enough from 0 to subtract freely

    private fun item(id: Long, ageMs: Long, checked: Boolean = false, staleResetAt: Long? = null) =
        Item(id = id, name = "item$id", checked = checked, createdAt = now - ageMs, staleResetAt = staleResetAt)

    @Test
    fun freshItemHasNoTier() {
        assertNull(tier(item(1, ageMs = 0)))
        assertNull(tier(item(2, ageMs = day - 1)))
    }

    @Test
    fun tiersMatchDesignBoundaries() {
        assertEquals(StaleTier.Day, tier(item(1, ageMs = day)))
        assertEquals(StaleTier.Day, tier(item(2, ageMs = 2 * day)))
        assertEquals(StaleTier.ThreeDays, tier(item(3, ageMs = 3 * day)))
        assertEquals(StaleTier.ThreeDays, tier(item(4, ageMs = 6 * day)))
        assertEquals(StaleTier.Week, tier(item(5, ageMs = 7 * day)))
        assertEquals(StaleTier.Week, tier(item(6, ageMs = 30 * day)))
    }

    @Test
    fun checkedItemsAreNeverStale() {
        assertNull(tier(item(1, ageMs = 30 * day, checked = true)))
    }

    @Test
    fun keepResetsTheClock() {
        // Added 10 days ago, but kept (reset) 2 hours ago -> fresh again.
        val kept = item(1, ageMs = 10 * day, staleResetAt = now - (2 * 60 * 60 * 1000L))
        assertNull(tier(kept))
    }

    @Test
    fun arrangePutsStaleUncheckedFirstOldestFirst() {
        val fresh = item(1, ageMs = 0)
        val staleWeek = item(2, ageMs = 10 * day)
        val stale3 = item(3, ageMs = 4 * day)
        val done = item(4, ageMs = 20 * day, checked = true)

        val result = StaleSort.arrange(listOf(fresh, staleWeek, stale3, done), now)

        assertEquals(listOf(staleWeek.id, stale3.id, fresh.id, done.id), result.map { it.id })
    }

    @Test
    fun arrangeKeepsCheckedItemsTrailingRegardlessOfAge() {
        val oldDone = item(1, ageMs = 100 * day, checked = true)
        val newOpen = item(2, ageMs = 0)
        assertEquals(listOf(newOpen.id, oldDone.id), StaleSort.arrange(listOf(oldDone, newOpen), now).map { it.id })
    }

    private fun tier(item: Item) = StaleSort.tierOf(item, now)
}
