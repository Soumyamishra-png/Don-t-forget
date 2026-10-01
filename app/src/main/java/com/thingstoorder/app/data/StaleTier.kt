package com.thingstoorder.app.data

enum class StaleTier(val label: String) {
    Day("1 day"),
    ThreeDays("3 days"),
    Week("a week"),
}

object StaleSort {
    private const val DAY_MS = 24 * 60 * 60 * 1000L

    fun tierOf(item: Item, now: Long): StaleTier? {
        if (item.checked) return null
        val ageMs = now - (item.staleResetAt ?: item.createdAt)
        val ageDays = ageMs / DAY_MS
        return when {
            ageDays >= 7 -> StaleTier.Week
            ageDays >= 3 -> StaleTier.ThreeDays
            ageDays >= 1 -> StaleTier.Day
            else -> null
        }
    }

    // Stale (unchecked) items first, oldest first; then the rest of the unchecked items in
    // their existing order; checked items keep their existing trailing position.
    fun arrange(items: List<Item>, now: Long): List<Item> {
        val (unchecked, checked) = items.partition { !it.checked }
        val (stale, fresh) = unchecked.partition { tierOf(it, now) != null }
        val staleOldestFirst = stale.sortedBy { it.staleResetAt ?: it.createdAt }
        return staleOldestFirst + fresh + checked
    }
}
