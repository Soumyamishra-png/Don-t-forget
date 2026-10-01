package com.thingstoorder.app.widget

import com.thingstoorder.app.R
import com.thingstoorder.app.data.Item
import com.thingstoorder.app.data.StaleSort

// The "Typing / listening" state from the design exists only once voice input is built; it is
// deliberately not a case here yet.
enum class WidgetMascotState(val stripColor: Long, val face: Int, val faceIsLight: Boolean) {
    Empty(0xFF9AD4FF, R.drawable.mark_face_neutral, faceIsLight = false),
    Eager(0xFFFF5E1A, R.drawable.mark_face_eager, faceIsLight = false),
    Stale(0xFFC2410C, R.drawable.mark_face_stale, faceIsLight = true),
    AllDone(0xFF1F6BFF, R.drawable.mark_face_happy, faceIsLight = true);

    companion object {
        fun of(items: List<Item>, now: Long): WidgetMascotState {
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
                caption = "Nothing here yet. What's running low?",
                pillLabel = "Add something",
                takeoverLine = "Need anything?",
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
