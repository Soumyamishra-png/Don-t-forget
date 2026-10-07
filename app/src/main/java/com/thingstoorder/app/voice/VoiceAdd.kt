package com.thingstoorder.app.voice

import com.thingstoorder.app.data.Item
import com.thingstoorder.app.data.ItemDao

/**
 * Adds spoken items. A name already on the list (not yet checked off) is skipped, with its quantity
 * updated when a new one was spoken. Returns the names that were already there.
 */
suspend fun ItemDao.addParsed(parsed: List<ParsedItem>): List<String> {
    val existing = getRemaining().toMutableList()
    val skipped = mutableListOf<String>()
    for (p in parsed) {
        val match = existing.firstOrNull { it.name.equals(p.name, ignoreCase = true) }
        if (match != null) {
            skipped += match.name
            if (p.quantity != null) setQuantity(match.id, p.quantity)
        } else {
            val id = insert(Item(name = p.name, quantity = p.quantity))
            existing += Item(id = id, name = p.name, quantity = p.quantity)
        }
    }
    return skipped.distinct()
}
