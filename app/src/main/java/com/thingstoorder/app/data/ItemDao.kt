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
}
