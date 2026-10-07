package com.thingstoorder.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("SELECT * FROM items ORDER BY checked ASC, createdAt ASC, id ASC")
    fun observeAll(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE checked = 0 ORDER BY createdAt ASC, id ASC")
    suspend fun getRemaining(): List<Item>

    @Query("SELECT * FROM items WHERE checked = 0 ORDER BY createdAt ASC, id ASC")
    fun observeRemaining(): Flow<List<Item>>

    @Query("SELECT * FROM items ORDER BY checked ASC, createdAt ASC, id ASC")
    suspend fun getAll(): List<Item>

    @Query("UPDATE items SET checked = NOT checked WHERE id = :id")
    suspend fun toggleChecked(id: Long)

    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM items WHERE checked = 0")
    fun observeRemainingCount(): Flow<Int>

    @Insert
    suspend fun insert(item: Item): Long

    @Update
    suspend fun update(item: Item)

    @Delete
    suspend fun delete(item: Item)

    @Query("UPDATE items SET checked = :checked WHERE id = :id")
    suspend fun setChecked(id: Long, checked: Boolean)

    @Query("DELETE FROM items WHERE checked = 1")
    suspend fun clearChecked()

    @Query("UPDATE items SET quantity = :quantity WHERE id = :id")
    suspend fun setQuantity(id: Long, quantity: String?)

    @Query("UPDATE items SET staleResetAt = :at WHERE id = :id")
    suspend fun keepFresh(id: Long, at: Long)
}
