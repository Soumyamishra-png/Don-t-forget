package com.thingstoorder.app.widget

import com.thingstoorder.app.R
import com.thingstoorder.app.data.Item
import com.thingstoorder.app.data.StaleSort

/**
 * The widget's mood. Each mood is a whole-surface colour with a face tile (Mint Line):
 * empty on paper, listening on mint, eager on green, stale (pleading) on coral, all done on ink.
 */
enum class WidgetMascotState(val card: Int, val pill: Int, val tile: Int, val onInk: Boolean) {
    Empty(R.drawable.widget_card_empty, R.drawable.widget_pill_empty, R.drawable.face_tile_neutral, onInk = false),
    Listening(R.drawable.widget_card_listening, R.drawable.widget_pill_listening, R.drawable.face_tile_listening, onInk = false),
    Eager(R.drawable.widget_card_eager, R.drawable.widget_pill_eager, R.drawable.face_tile_eager, onInk = false),
    Stale(R.drawable.widget_card_stale, R.drawable.widget_pill_stale, R.drawable.face_tile_pleading, onInk = false),
    AllDone(R.drawable.widget_card_alldone, R.drawable.widget_pill_alldone, R.drawable.face_tile_happy, onInk = true);

    companion object {
        fun of(items: List<Item>, now: Long, listening: Boolean = false): WidgetMascotState {
            if (listening) return Listening
            if (items.isEmpty()) return Empty
            val remaining = items.filter { !it.checked }
            if (remaining.isEmpty()) return AllDone
            val oldestStale = remaining.filter { StaleSort.tierOf(it, now) != null }
                .minByOrNull { it.staleResetAt ?: it.createdAt }
            return if (oldestStale != null) Stale else Eager
        }
    }
}

data class WidgetCopy(val caption: String, val pillLabel: String, val takeoverLine: String)

object WidgetCopyBuilder {
    fun forState(state: WidgetMascotState, items: List<Item>, now: Long): WidgetCopy {
        val remaining = items.count { !it.checked }
        val oldestStaleName = items.filter { !it.checked }
            .filter { StaleSort.tierOf(it, now) != null }
            .minByOrNull { it.staleResetAt ?: it.createdAt }?.name

        return when (state) {
            WidgetMascotState.Empty -> WidgetCopy(
                caption = "Nothing here yet.",
                pillLabel = "Add something",
                takeoverLine = "Need anything?",
            )
            WidgetMascotState.Listening -> WidgetCopy(
                caption = "Listening…",
                pillLabel = "Listening…",
                takeoverLine = "Listening…",
            )
            WidgetMascotState.Eager -> WidgetCopy(
                caption = "Ready when you are.",
                pillLabel = pluralThings(remaining),
                takeoverLine = "$remaining to order. Anything else?",
            )
            WidgetMascotState.Stale -> WidgetCopy(
                caption = if (oldestStaleName != null) "Still need $oldestStaleName?" else "Still need it?",
                pillLabel = if (oldestStaleName != null) "Still need $oldestStaleName?" else "Still need it?",
                takeoverLine = if (oldestStaleName != null) "$oldestStaleName?" else "Need anything?",
            )
            WidgetMascotState.AllDone -> WidgetCopy(
                caption = "All sorted. Nice one.",
                pillLabel = "All sorted",
                takeoverLine = "All sorted!",
            )
        }
    }

    private fun pluralThings(count: Int) = when (count) {
        1 -> "1 thing to order"
        else -> "$count things to order"
    }
}
