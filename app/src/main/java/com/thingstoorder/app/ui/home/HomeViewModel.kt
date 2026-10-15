package com.thingstoorder.app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.thingstoorder.app.data.AppDatabase
import com.thingstoorder.app.data.Item
import com.thingstoorder.app.data.ItemStore
import com.thingstoorder.app.data.SettingsRepository
import com.thingstoorder.app.voice.ParsedItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).itemDao()
    private val store = ItemStore(app)
    private val settings = SettingsRepository(app)

    val items = dao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    private val sharedRepo = com.thingstoorder.app.data.shared.SharedLists.get(app)
    val shared = sharedRepo.current
    val myUid get() = sharedRepo.myUid
    val bannerSeen = settings.orderBannerSeen.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Long.MAX_VALUE)

    fun dismissBanner(at: Long) {
        viewModelScope.launch { settings.setOrderBannerSeen(at) }
    }

    val watchedApps = settings.watchedApps.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun add(name: String): Boolean {
        val clean = name.trim()
        if (clean.isEmpty()) return false
        viewModelScope.launch { store.add(clean) }
        return true
    }


    /** Adds spoken items; returns the names that were already on the list. */
    fun addVoice(parsed: List<ParsedItem>, onDone: (skipped: List<String>) -> Unit) {
        viewModelScope.launch { onDone(store.addParsed(parsed)) }
    }

    fun toggle(item: Item) {
        viewModelScope.launch { store.setChecked(item.id, !item.checked) }
    }

    fun remove(item: Item) {
        viewModelScope.launch { store.remove(item.id) }
    }

    fun save(item: Item, name: String, note: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch { store.edit(item, clean, note.trim().ifEmpty { null }) }
    }

    fun clearDone() {
        viewModelScope.launch { store.clearChecked() }
    }

    fun keepFresh(item: Item) {
        viewModelScope.launch { store.keepFresh(item.id, System.currentTimeMillis()) }
    }
}
