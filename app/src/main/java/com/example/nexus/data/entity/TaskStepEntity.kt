package com.example.nexus.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "task_steps",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["taskId"]),
        Index(value = ["taskId", "stepIndex"])
    ]
)
data class TaskStepEntity(
    @PrimaryKey
    val id: String,
    val taskId: String,
    val stepIndex: Int,
    val title: String,
    val description: String,
    val status: String = "PENDING", // "PENDING", "RUNNING", "DONE", "FAILED", "SKIPPED"
    val verificationMethod: String = "",
    val verificationNotes: String = "",
    val operationJson: String? = null
)
