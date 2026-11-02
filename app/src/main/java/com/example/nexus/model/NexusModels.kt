package com.example.nexus.model

enum class RiskLevel {
    LOW, MEDIUM, HIGH
}

enum class OpStatus {
    PENDING, APPROVED, DENIED, EXECUTING, COMPLETED, FAILED
}

enum class TaskStatus {
    UNDERSTAND, PLAN, PERMISSION_CHECK, EXECUTING, VERIFYING, COMPLETED, FAILED, CANCELLED
}

enum class StepStatus {
    PENDING, RUNNING, DONE, FAILED, SKIPPED
}

enum class TerminalKind {
    CMD, OUT, ERR, BLOCKED, META, SYSTEM, WARN
}

enum class AIProviderType {
    DETERMINISTIC,
    GEMINI
}

data class Agent(
    val id: String,
    val name: String,
    val title: String,
    val description: String,
    val systemPrompt: String,
    val permittedCategories: Set<String>,
    val iconName: String = "terminal",
    val isCustom: Boolean = false
)

data class Operation(
    val id: String,
    val opName: String,
    val label: String,
    val category: String,
    val risk: RiskLevel,
    val params: Map<String, String>,
    val status: OpStatus = OpStatus.PENDING,
    val policyReason: String = "",
    val output: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class AuditEntry(
    val id: String,
    val opName: String,
    val category: String,
    val status: String, // "APPROVED", "DENIED", "BLOCKED"
    val detail: String,
    val timestamp: Long = System.currentTimeMillis(),
    val agentId: String = "system"
)

data class PermissionSettings(
    val filesystem: Boolean = false,
    val packages: Boolean = false,
    val network: Boolean = false,
    val workspace: Boolean = true,
    val shellScripts: Boolean = false
)

data class ChatMessage(
    val id: String,
    val role: String, // "user", "assistant", "system"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val operation: Operation? = null,
    val taskSnapshot: NexusTask? = null
)

data class Conversation(
    val id: String,
    val title: String,
    val agentId: String = "developer",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val messages: List<ChatMessage> = emptyList()
)

data class TaskStep(
    val id: String,
    val title: String,
    val description: String,
    val operation: Operation? = null,
    val status: StepStatus = StepStatus.PENDING,
    val verificationMethod: String = "",
    val verificationNotes: String = ""
)

data class NexusTask(
    val id: String,
    val title: String,
    val objective: String,
    val agentId: String,
    val status: TaskStatus = TaskStatus.UNDERSTAND,
    val steps: List<TaskStep> = emptyList(),
    val currentStepIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val executionLogs: List<String> = emptyList()
)

data class ToolPackage(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val category: String,
    val installed: Boolean,
    val sizeFormatted: String,
    val dependencies: List<String> = emptyList(),
    val sha256: String,
    val isCore: Boolean = false
)

data class MemoryItem(
    val id: String,
    val key: String,
    val value: String,
    val category: String, // "Environment", "Configuration", "Project Context", "Learned Rules"
    val source: String = "user",
    val updatedAt: Long = System.currentTimeMillis()
)

data class WorkspaceFile(
    val name: String,
    val path: String,
    val relativePath: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long
)

data class TerminalEntry(
    val id: String,
    val kind: TerminalKind,
    val text: String,
    val operation: Operation? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class SystemInfo(
    val ramFreeMb: Long,
    val ramTotalMb: Long,
    val ramPercent: Int,
    val storageFreeGb: Double,
    val storageTotalGb: Double,
    val batteryLevel: Int,
    val isCharging: Boolean,
    val cpuCores: Int,
    val abi: String,
    val localIp: String = "127.0.0.1",
    val networkState: String = "Connected"
)
