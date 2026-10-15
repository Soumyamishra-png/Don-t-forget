package com.thingstoorder.app.data

import android.content.Context
import com.thingstoorder.app.data.shared.SharedLists
import com.thingstoorder.app.voice.ParsedItem

/**
 * Every change to the list goes through here. On a local list it writes Room; on a shared list it writes
 * Firestore, and the sync listener mirrors the result into Room.
 */
class ItemStore(context: Context) {
    private val app = context.applicationContext
    private val dao = AppDatabase.get(app).itemDao()
    private val shared = SharedLists.get(app)

    private suspend fun remote(id: Long): Pair<String, Item>? {
        val listId = shared.listId() ?: return null
        val item = dao.getById(id) ?: return null
        return if (item.remoteId != null) listId to item else null
    }

    suspend fun add(name: String, quantity: String? = null, note: String? = null) {
        val listId = shared.listId()
        if (listId != null) shared.addItem(listId, name, quantity, note)
        else dao.insert(Item(name = name, quantity = quantity, note = note))
    }

    /** Adds spoken items; a name already on the list is skipped (its quantity updated if one was spoken). Returns the skipped names. */
    suspend fun addParsed(parsed: List<ParsedItem>): List<String> {
        val existing = dao.getRemaining().toMutableList()
        val skipped = mutableListOf<String>()
        for (p in parsed) {
            val match = existing.firstOrNull { it.name.equals(p.name, ignoreCase = true) }
            if (match != null) {
                skipped += match.name
                if (p.quantity != null) setQuantity(match.id, p.quantity)
            } else {
                add(p.name, p.quantity)
                existing += Item(name = p.name, quantity = p.quantity)
            }
        }
        return skipped.distinct()
    }

    suspend fun setChecked(id: Long, checked: Boolean) {
        val r = remote(id)
        if (r != null) shared.setChecked(r.first, r.second.remoteId!!, checked) else dao.setChecked(id, checked)
    }

    suspend fun toggle(id: Long) {
        val item = dao.getById(id) ?: return
        setChecked(id, !item.checked)
    }

    suspend fun remove(id: Long) {
        val r = remote(id)
        if (r != null) shared.remove(r.first, r.second.remoteId!!) else dao.deleteById(id)
    }

    suspend fun edit(item: Item, name: String, note: String?) {
        val r = remote(item.id)
        if (r != null) shared.edit(r.first, r.second.remoteId!!, name, note) else dao.update(item.copy(name = name, note = note))
    }

    suspend fun setQuantity(id: Long, quantity: String?) {
        val r = remote(id)
        if (r != null) shared.setQuantity(r.first, r.second.remoteId!!, quantity) else dao.setQuantity(id, quantity)
    }

    suspend fun keepFresh(id: Long, at: Long) {
        val r = remote(id)
        if (r != null) shared.keepFresh(r.first, r.second.remoteId!!, at) else dao.keepFresh(id, at)
    }

    suspend fun clearChecked() {
        val listId = shared.listId()
        if (listId != null) shared.clearChecked(listId) else dao.clearChecked()
    }
}
