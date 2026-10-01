package com.thingstoorder.app.widget

import com.thingstoorder.app.data.Item
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetMascotStateTest {
    private val day = 24 * 60 * 60 * 1000L
    private val now = 10 * day

    private fun item(id: Long, ageMs: Long, checked: Boolean = false, name: String = "item$id") =
        Item(id = id, name = name, checked = checked, createdAt = now - ageMs)

    @Test
    fun emptyListIsEmptyState() {
        assertEquals(WidgetMascotState.Empty, WidgetMascotState.of(emptyList(), now))
    }

    @Test
    fun allCheckedIsAllDone() {
        val items = listOf(item(1, 0, checked = true), item(2, 0, checked = true))
        assertEquals(WidgetMascotState.AllDone, WidgetMascotState.of(items, now))
    }

    @Test
    fun freshRemainingItemsAreEager() {
        val items = listOf(item(1, ageMs = 0), item(2, ageMs = 60_000))
        assertEquals(WidgetMascotState.Eager, WidgetMascotState.of(items, now))
    }

    @Test
    fun anyStaleUncheckedItemWinsOverEager() {
        val items = listOf(item(1, ageMs = 0), item(2, ageMs = 2 * day))
        assertEquals(WidgetMascotState.Stale, WidgetMascotState.of(items, now))
    }

    @Test
    fun staleCheckedItemsDoNotCountSoListIsAllDone() {
        val items = listOf(item(1, ageMs = 30 * day, checked = true))
        assertEquals(WidgetMascotState.AllDone, WidgetMascotState.of(items, now))
    }

    @Test
    fun staleCaptionNamesTheOldestStaleItem() {
        val items = listOf(
            item(1, ageMs = 2 * day, name = "Milk"),
            item(2, ageMs = 8 * day, name = "Atta"),
        )
        val copy = WidgetCopyBuilder.forState(WidgetMascotState.Stale, items, now)
        assertEquals("Still need Atta?", copy.caption)
    }

    @Test
    fun eagerPillLabelIsSingularForOneItem() {
        val copy = WidgetCopyBuilder.forState(WidgetMascotState.Eager, listOf(item(1, 0)), now)
        assertEquals("1 thing to order", copy.pillLabel)
    }
}
