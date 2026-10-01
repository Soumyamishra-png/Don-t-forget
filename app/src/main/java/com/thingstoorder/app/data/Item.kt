package com.thingstoorder.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class Item(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val note: String? = null,
    val checked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    // Reset to "now" when the user taps Keep on a stale item; staleness age is measured
    // from this if set, otherwise from createdAt.
    val staleResetAt: Long? = null,
)
