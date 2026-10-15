package com.thingstoorder.app.data.shared

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.thingstoorder.app.data.AppDatabase
import com.thingstoorder.app.data.Item
import com.thingstoorder.app.data.SettingsRepository
import java.security.SecureRandom
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

const val MAX_MEMBERS = 4

data class Member(val uid: String, val name: String, val color: Int)
data class OrderNote(val byUid: String, val byName: String, val count: Int, val at: Long)
data class SharedList(val id: String, val code: String, val ownerUid: String, val members: List<Member>, val lastOrder: OrderNote?)
data class JoinPreview(val listId: String, val ownerName: String)

class SharedError(message: String) : Exception(message)

/**
 * The one shared list. While joined, Firestore is the source of truth and Room is a mirror of it, so the
 * widget, reminders and voice keep reading Room exactly as before. Writes go to Firestore (its offline
 * cache applies them instantly) and the snapshot listener copies them back into Room.
 */
class SharedLists private constructor(private val app: Context) {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val dao = AppDatabase.get(app).itemDao()
    private val settings = SettingsRepository(app)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncLock = Mutex()

    private val _current = MutableStateFlow<SharedList?>(null)
    val current: StateFlow<SharedList?> = _current.asStateFlow()

    private var listReg: ListenerRegistration? = null
    private var itemsReg: ListenerRegistration? = null

    val myUid: String? get() = auth.currentUser?.uid

    fun start() {
        scope.launch {
            val id = settings.sharedListId.first() ?: return@launch
            runCatching { ensureUid() }
            attach(id)
        }
    }

    suspend fun listId(): String? = settings.sharedListId.first()

    private suspend fun ensureUid(): String =
        auth.currentUser?.uid ?: withTimeout(NETWORK_TIMEOUT_MS) { auth.signInAnonymously().await().user!!.uid }

    private fun lists() = db.collection("lists")

    // ---- Create / join / leave ------------------------------------------------------------------

    suspend fun createShare(name: String) {
        val uid = guarded { ensureUid() }
        settings.setMyName(name)
        val code = guarded { freeCode() }
        val listRef = lists().document()
        val local = dao.getAll()
        val now = System.currentTimeMillis()
        val docIds = local.associate { it.id to listRef.collection("items").document().id }

        guarded {
            db.batch().apply {
                set(
                    listRef,
                    mapOf(
                        "ownerUid" to uid, "code" to code, "memberUids" to listOf(uid),
                        "members" to mapOf(uid to mapOf("name" to name, "color" to 0)), "createdAt" to now,
                    ),
                )
                set(db.collection("codes").document(code), mapOf("listId" to listRef.id, "ownerName" to name))
                local.forEach { set(listRef.collection("items").document(docIds.getValue(it.id)), itemMap(it, uid)) }
            }.commit().await()
        }
        local.forEach { dao.attachRemote(it.id, docIds.getValue(it.id), uid) }
        settings.setShared(listRef.id, code)
        attach(listRef.id)
    }

    suspend fun preview(code: String): JoinPreview {
        guarded { ensureUid() }
        val doc = guarded { db.collection("codes").document(normalize(code)).get().await() }
        if (!doc.exists()) throw SharedError("That code doesn't match a list. Check it and try again.")
        return JoinPreview(doc.getString("listId").orEmpty(), doc.getString("ownerName").orEmpty())
    }

    suspend fun join(code: String, name: String) {
        val uid = guarded { ensureUid() }
        val preview = preview(code)
        val listRef = lists().document(preview.listId)
        settings.setMyName(name)

        guarded {
            db.runTransaction { tx ->
                val snap = tx.get(listRef)
                val uids = (snap.get("memberUids") as? List<*>)?.filterIsInstance<String>().orEmpty()
                if (uid in uids) return@runTransaction
                if (uids.size >= MAX_MEMBERS) throw SharedError("That list already has $MAX_MEMBERS people.")
                val used = parseMembers(snap.get("members"), uids).map { it.color }.toSet()
                val color = (0 until MAX_MEMBERS).firstOrNull { it !in used } ?: 0
                tx.update(
                    listRef,
                    mapOf("memberUids" to FieldValue.arrayUnion(uid), "members.$uid" to mapOf("name" to name, "color" to color)),
                )
            }.await()
        }
        mergeLocalInto(listRef.id, uid)
        settings.setShared(listRef.id, normalize(code))
        attach(listRef.id)
    }

    /** Moves this phone's own items into the list; names already there become one. */
    private suspend fun mergeLocalInto(listId: String, uid: String) {
        val itemsRef = lists().document(listId).collection("items")
        val remote = guarded { itemsRef.get().await() }.documents.mapNotNull { it.getString("name")?.lowercase() }.toSet()
        val local = dao.getLocalOnly()
        val batch = db.batch()
        val uploads = mutableListOf<Pair<Item, String>>()
        for (item in local) {
            if (item.name.lowercase() in remote) {
                dao.deleteById(item.id)
                continue
            }
            val ref = itemsRef.document()
            batch.set(ref, itemMap(item, uid))
            uploads += item to ref.id
        }
        if (uploads.isNotEmpty()) guarded { batch.commit().await() }
        uploads.forEach { (item, id) -> dao.attachRemote(item.id, id, uid) }
    }

    suspend fun leave() {
        val id = listId() ?: return
        val uid = myUid
        val listRef = lists().document(id)
        if (uid != null) {
            runCatching {
                withTimeout(NETWORK_TIMEOUT_MS) {
                    db.runTransaction { tx ->
                        val snap = tx.get(listRef)
                        val uids = (snap.get("memberUids") as? List<*>)?.filterIsInstance<String>().orEmpty()
                        val rest = uids - uid
                        if (rest.isEmpty()) {
                            tx.delete(listRef)
                            snap.getString("code")?.let { tx.delete(db.collection("codes").document(it)) }
                        } else {
                            val updates = mutableMapOf<String, Any>("memberUids" to rest, "members.$uid" to FieldValue.delete())
                            if (snap.getString("ownerUid") == uid) updates["ownerUid"] = rest.first()
                            tx.update(listRef, updates)
                        }
                    }.await()
                }
            }.onFailure { Log.w(TAG, "leave: could not update server", it) }
        }
        detachLocally()
    }

    suspend fun removeMember(uid: String) {
        val id = listId() ?: return
        guarded {
            lists().document(id)
                .update(mapOf("memberUids" to FieldValue.arrayRemove(uid), "members.$uid" to FieldValue.delete())).await()
        }
    }

    private suspend fun detachLocally() {
        listReg?.remove()
        itemsReg?.remove()
        listReg = null
        itemsReg = null
        syncLock.withLock { dao.detachAll() }
        settings.setShared(null, null)
        _current.value = null
    }

    // ---- Live sync --------------------------------------------------------------------------------

    private fun attach(listId: String) {
        listReg?.remove()
        itemsReg?.remove()
        val listRef = lists().document(listId)
        listReg = listRef.addSnapshotListener { snap, err ->
            if (err != null) {
                Log.w(TAG, "list listener", err)
                if (err.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) scope.launch { detachLocally() }
                return@addSnapshotListener
            }
            if (snap == null) return@addSnapshotListener
            if (!snap.exists()) {
                // An empty cache snapshot is not proof the list is gone; only trust a server answer.
                if (!snap.metadata.isFromCache) scope.launch { detachLocally() }
                return@addSnapshotListener
            }
            val uids = (snap.get("memberUids") as? List<*>)?.filterIsInstance<String>().orEmpty()
            val me = myUid
            if (me != null && me !in uids) {
                scope.launch { detachLocally() }
                return@addSnapshotListener
            }
            val order = (snap.get("lastOrder") as? Map<*, *>)?.let {
                OrderNote(
                    byUid = it["by"] as? String ?: "", byName = it["name"] as? String ?: "",
                    count = (it["count"] as? Number)?.toInt() ?: 0, at = (it["at"] as? Number)?.toLong() ?: 0L,
                )
            }
            _current.value = SharedList(
                id = snap.id, code = snap.getString("code").orEmpty(), ownerUid = snap.getString("ownerUid").orEmpty(),
                members = parseMembers(snap.get("members"), uids), lastOrder = order,
            )
        }
        itemsReg = listRef.collection("items").addSnapshotListener { snap, err ->
            if (err != null) {
                Log.w(TAG, "items listener", err)
                return@addSnapshotListener
            }
            if (snap != null) scope.launch { reconcile(snap) }
        }
    }

    private suspend fun reconcile(snap: QuerySnapshot) = syncLock.withLock {
        val seen = HashSet<String>()
        for (doc in snap.documents) {
            seen += doc.id
            val existing = dao.getByRemoteId(doc.id)
            val item = Item(
                id = existing?.id ?: 0,
                name = doc.getString("name").orEmpty(),
                note = doc.getString("note"),
                checked = doc.getBoolean("checked") ?: false,
                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                staleResetAt = doc.getLong("staleResetAt"),
                quantity = doc.getString("quantity"),
                remoteId = doc.id,
                addedBy = doc.getString("addedBy"),
            )
            if (existing == null) dao.insert(item) else if (existing != item) dao.update(item)
        }
        dao.getRemoteItems().filter { it.remoteId !in seen }.forEach { dao.deleteById(it.id) }
    }

    // ---- Writes (called through ItemStore while shared) -------------------------------------------

    private fun itemsRef(listId: String) = lists().document(listId).collection("items")

    fun addItem(listId: String, name: String, quantity: String?, note: String?) {
        itemsRef(listId).document().set(itemMap(Item(name = name, quantity = quantity, note = note), myUid))
    }

    fun setChecked(listId: String, remoteId: String, checked: Boolean) {
        itemsRef(listId).document(remoteId).update("checked", checked)
    }

    fun remove(listId: String, remoteId: String) {
        itemsRef(listId).document(remoteId).delete()
    }

    fun edit(listId: String, remoteId: String, name: String, note: String?) {
        itemsRef(listId).document(remoteId).update(mapOf("name" to name, "note" to note))
    }

    fun setQuantity(listId: String, remoteId: String, quantity: String?) {
        itemsRef(listId).document(remoteId).update("quantity", quantity)
    }

    fun keepFresh(listId: String, remoteId: String, at: Long) {
        itemsRef(listId).document(remoteId).update("staleResetAt", at)
    }

    /** "Clear done" on a shared list means somebody ordered: clear the ticked items for everyone and tell the others. */
    suspend fun clearChecked(listId: String) {
        val done = dao.getAll().filter { it.checked && it.remoteId != null }
        if (done.isEmpty()) return
        val name = settings.myName.first().orEmpty()
        db.batch().apply {
            done.forEach { delete(itemsRef(listId).document(it.remoteId!!)) }
            update(
                lists().document(listId), "lastOrder",
                mapOf("by" to myUid, "name" to name, "count" to done.size, "at" to System.currentTimeMillis()),
            )
        }.commit()
    }

    // ---- Helpers ----------------------------------------------------------------------------------

    private fun itemMap(item: Item, uid: String?): Map<String, Any?> = mapOf(
        "name" to item.name, "note" to item.note, "quantity" to item.quantity, "checked" to item.checked,
        "createdAt" to item.createdAt, "staleResetAt" to item.staleResetAt, "addedBy" to (item.addedBy ?: uid),
    )

    private fun parseMembers(raw: Any?, order: List<String>): List<Member> {
        val map = raw as? Map<*, *> ?: return emptyList()
        return order.mapNotNull { uid ->
            val m = map[uid] as? Map<*, *> ?: return@mapNotNull null
            Member(uid, m["name"] as? String ?: "", (m["color"] as? Number)?.toInt() ?: 0)
        }
    }

    private suspend fun freeCode(): String {
        repeat(6) {
            val code = (1..CODE_LENGTH).map { CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)] }.joinToString("")
            if (!db.collection("codes").document(code).get().await().exists()) return code
        }
        throw SharedError("Couldn't make a free code. Try again.")
    }

    private fun normalize(code: String) = code.trim().uppercase().replace(" ", "").replace("-", "")

    private suspend fun <T> guarded(block: suspend () -> T): T = try {
        withTimeout(NETWORK_TIMEOUT_MS) { block() }
    } catch (e: SharedError) {
        throw e
    } catch (e: TimeoutCancellationException) {
        throw SharedError("Couldn't reach the server. Check your connection and try again.")
    } catch (e: FirebaseFirestoreException) {
        Log.w(TAG, "firestore", e)
        throw SharedError(
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) "That didn't work. The list may be full or no longer exist."
            else "Couldn't reach the server. Check your connection and try again.",
        )
    } catch (e: FirebaseException) {
        Log.w(TAG, "firebase", e)
        throw SharedError("Couldn't reach the server. Check your connection and try again.")
    }

    companion object {
        const val CODE_LENGTH = 6
        private const val TAG = "SharedLists"
        private const val NETWORK_TIMEOUT_MS = 15_000L
        private const val CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
        private val random = SecureRandom()

        @Volatile private var instance: SharedLists? = null

        fun get(context: Context): SharedLists = instance ?: synchronized(this) {
            instance ?: SharedLists(context.applicationContext).also { instance = it }
        }
    }
}
