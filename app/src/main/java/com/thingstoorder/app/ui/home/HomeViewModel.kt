package com.thingstoorder.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.thingstoorder.app.data.AppDatabase
import com.thingstoorder.app.data.Item
import com.thingstoorder.app.data.SettingsRepository
import com.thingstoorder.app.voice.ParsedItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).itemDao()
    private val settings = SettingsRepository(app)

    val items = dao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val watchedApps = settings.watchedApps.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun add(name: String): Boolean {
        val clean = name.trim()
        if (clean.isEmpty()) return false
        viewModelScope.launch { dao.insert(Item(name = clean)) }
        return true
    }


    /** Adds spoken items. Names already on the list are skipped (quantity updated when a new one is given); returns the skipped names. */
    fun addVoice(parsed: List<ParsedItem>, onDone: (skipped: List<String>) -> Unit) {
        viewModelScope.launch {
            val existing = dao.getRemaining().toMutableList()
            val skipped = mutableListOf<String>()
            for (p in parsed) {
                val match = existing.firstOrNull { it.name.equals(p.name, ignoreCase = true) }
                if (match != null) {
                    skipped += match.name
                    if (p.quantity != null) dao.setQuantity(match.id, p.quantity)
                } else {
                    val id = dao.insert(Item(name = p.name, quantity = p.quantity))
                    existing += Item(id = id, name = p.name, quantity = p.quantity)
                }
            }
            onDone(skipped.distinct())
        }
    }

    fun toggle(item: Item) {
        viewModelScope.launch { dao.setChecked(item.id, !item.checked) }
    }

    fun remove(item: Item) {
        viewModelScope.launch { dao.delete(item) }
    }

    fun save(item: Item, name: String, note: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch { dao.update(item.copy(name = clean, note = note.trim().ifEmpty { null })) }
    }

    fun clearDone() {
        viewModelScope.launch { dao.clearChecked() }
    }

    fun keepFresh(item: Item) {
        viewModelScope.launch { dao.keepFresh(item.id, System.currentTimeMillis()) }
    }
}
