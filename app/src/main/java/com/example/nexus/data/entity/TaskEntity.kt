package com.example.nexus.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val objective: String,
    val agentId: String,
    val status: String = "UNDERSTAND", // "UNDERSTAND", "PLAN", "PERMISSION_CHECK", "EXECUTING", "VERIFYING", "COMPLETED", "FAILED", "CANCELLED"
    val currentStepIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val executionLogsJson: String = "[]"
)
