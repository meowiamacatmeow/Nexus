package com.example.nexus.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "memory_items",
    indices = [
        Index(value = ["category"]),
        Index(value = ["key"])
    ]
)
data class MemoryItemEntity(
    @PrimaryKey
    val id: String,
    val key: String,
    val value: String,
    val category: String, // "Environment", "Configuration", "Project Context", "Learned Rules"
    val source: String = "user",
    val updatedAt: Long = System.currentTimeMillis()
)
