package com.example.nexus.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "audit_logs",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["category"])
    ]
)
data class AuditLogEntity(
    @PrimaryKey
    val id: String,
    val opName: String,
    val category: String,
    val status: String, // "APPROVED", "DENIED", "BLOCKED"
    val detail: String,
    val timestamp: Long = System.currentTimeMillis(),
    val agentId: String = "system"
)
