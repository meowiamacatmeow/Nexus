package com.example.nexus.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.nexus.data.entity.MemoryItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {

    @Query("SELECT * FROM memory_items ORDER BY updatedAt DESC")
    fun getAllMemoryItems(): Flow<List<MemoryItemEntity>>

    @Query("SELECT * FROM memory_items WHERE category = :category ORDER BY updatedAt DESC")
    fun getMemoryItemsByCategory(category: String): Flow<List<MemoryItemEntity>>

    @Query("SELECT * FROM memory_items WHERE `key` LIKE '%' || :query || '%' OR `value` LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchMemory(query: String): Flow<List<MemoryItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemoryItem(item: MemoryItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemoryItems(items: List<MemoryItemEntity>)

    @Update
    suspend fun updateMemoryItem(item: MemoryItemEntity)

    @Query("DELETE FROM memory_items WHERE id = :id")
    suspend fun deleteMemoryItem(id: String)

    @Query("DELETE FROM memory_items")
    suspend fun clearAllMemory()
}
