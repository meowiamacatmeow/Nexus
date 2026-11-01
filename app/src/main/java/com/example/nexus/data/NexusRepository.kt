package com.example.nexus.data

import com.example.nexus.data.dao.AuditDao
import com.example.nexus.data.dao.ConversationDao
import com.example.nexus.data.dao.MemoryDao
import com.example.nexus.data.dao.TaskDao
import com.example.nexus.data.entity.AuditLogEntity
import com.example.nexus.data.entity.ConversationEntity
import com.example.nexus.data.entity.ConversationWithMessages
import com.example.nexus.data.entity.MemoryItemEntity
import com.example.nexus.data.entity.MessageEntity
import com.example.nexus.data.entity.TaskEntity
import com.example.nexus.data.entity.TaskStepEntity
import com.example.nexus.data.entity.TaskWithSteps
import com.example.nexus.model.AuditEntry
import com.example.nexus.model.ChatMessage
import com.example.nexus.model.Conversation
import com.example.nexus.model.MemoryItem
import com.example.nexus.model.NexusTask
import com.example.nexus.model.OpStatus
import com.example.nexus.model.Operation
import com.example.nexus.model.RiskLevel
import com.example.nexus.model.StepStatus
import com.example.nexus.model.TaskStatus
import com.example.nexus.model.TaskStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class NexusRepository(
    private val conversationDao: ConversationDao,
    private val taskDao: TaskDao,
    private val memoryDao: MemoryDao,
    private val auditDao: AuditDao
) {

    // --- Conversations & Messages Flow ---

    val conversationsFlow: Flow<List<Conversation>> = conversationDao.getAllConversationsWithMessages()
        .map { list ->
            list.map { cwm ->
                Conversation(
                    id = cwm.conversation.id,
                    title = cwm.conversation.title,
                    agentId = cwm.conversation.agentId,
                    createdAt = cwm.conversation.createdAt,
                    updatedAt = cwm.conversation.updatedAt,
                    messages = cwm.messages.sortedBy { it.timestamp }.map { it.toDomain() }
                )
            }
        }

    suspend fun saveConversation(conv: Conversation) = withContext(Dispatchers.IO) {
        val entity = ConversationEntity(
            id = conv.id,
            title = conv.title,
            agentId = conv.agentId,
            createdAt = conv.createdAt,
            updatedAt = conv.updatedAt
        )
        conversationDao.insertConversation(entity)
        val msgEntities = conv.messages.map { it.toEntity(conv.id) }
        conversationDao.insertMessages(msgEntities)
    }

    suspend fun addMessage(convId: String, msg: ChatMessage) = withContext(Dispatchers.IO) {
        conversationDao.insertMessage(msg.toEntity(convId))
    }

    suspend fun deleteConversation(convId: String) = withContext(Dispatchers.IO) {
        conversationDao.deleteConversation(convId)
    }

    // --- Tasks Flow ---

    val tasksFlow: Flow<List<NexusTask>> = taskDao.getAllTasksWithSteps()
        .map { list ->
            list.map { tws -> tws.toDomain() }
        }

    suspend fun saveTask(task: NexusTask) = withContext(Dispatchers.IO) {
        val entity = TaskEntity(
            id = task.id,
            title = task.title,
            objective = task.objective,
            agentId = task.agentId,
            status = task.status.name,
            currentStepIndex = task.currentStepIndex,
            createdAt = task.createdAt,
            updatedAt = task.updatedAt,
            executionLogsJson = JSONArray(task.executionLogs).toString()
        )
        taskDao.insertTask(entity)
        val stepEntities = task.steps.mapIndexed { idx, s -> s.toEntity(task.id, idx) }
        taskDao.insertTaskSteps(stepEntities)
    }

    suspend fun deleteTask(taskId: String) = withContext(Dispatchers.IO) {
        taskDao.deleteTask(taskId)
    }

    // --- Memory Flow ---

    val memoryFlow: Flow<List<MemoryItem>> = memoryDao.getAllMemoryItems()
        .map { list ->
            list.map {
                MemoryItem(
                    id = it.id,
                    key = it.key,
                    value = it.value,
                    category = it.category,
                    source = it.source,
                    updatedAt = it.updatedAt
                )
            }
        }

    fun searchMemory(query: String): Flow<List<MemoryItem>> = memoryDao.searchMemory(query)
        .map { list ->
            list.map {
                MemoryItem(
                    id = it.id,
                    key = it.key,
                    value = it.value,
                    category = it.category,
                    source = it.source,
                    updatedAt = it.updatedAt
                )
            }
        }

    suspend fun saveMemoryItem(item: MemoryItem) = withContext(Dispatchers.IO) {
        val entity = MemoryItemEntity(
            id = item.id,
            key = item.key,
            value = item.value,
            category = item.category,
            source = item.source,
            updatedAt = item.updatedAt
        )
        memoryDao.insertMemoryItem(entity)
    }

    suspend fun deleteMemoryItem(id: String) = withContext(Dispatchers.IO) {
        memoryDao.deleteMemoryItem(id)
    }

    // --- Audit Logs Flow ---

    val auditLogsFlow: Flow<List<AuditEntry>> = auditDao.getAllAuditLogs()
        .map { list ->
            list.map {
                AuditEntry(
                    id = it.id,
                    opName = it.opName,
                    category = it.category,
                    status = it.status,
                    detail = it.detail,
                    timestamp = it.timestamp,
                    agentId = it.agentId
                )
            }
        }

    suspend fun saveAuditLog(entry: AuditEntry) = withContext(Dispatchers.IO) {
        val entity = AuditLogEntity(
            id = entry.id,
            opName = entry.opName,
            category = entry.category,
            status = entry.status,
            detail = entry.detail,
            timestamp = entry.timestamp,
            agentId = entry.agentId
        )
        auditDao.insertAuditLog(entity)
    }

    suspend fun clearAuditLogs() = withContext(Dispatchers.IO) {
        auditDao.clearAuditLogs()
    }

    // --- Initial Seeding ---

    suspend fun seedInitialDataIfEmpty(
        defaultConv: Conversation,
        defaultMemory: List<MemoryItem>,
        defaultAudit: List<AuditEntry>
    ) = withContext(Dispatchers.IO) {
        val existingConvs = conversationDao.getAllConversations().firstOrNull()
        if (existingConvs.isNullOrEmpty()) {
            saveConversation(defaultConv)
        }

        val existingMem = memoryDao.getAllMemoryItems().firstOrNull()
        if (existingMem.isNullOrEmpty()) {
            defaultMemory.forEach { saveMemoryItem(it) }
        }

        val existingAudit = auditDao.getAllAuditLogs().firstOrNull()
        if (existingAudit.isNullOrEmpty()) {
            defaultAudit.forEach { saveAuditLog(it) }
        }
    }

    // --- Private Helper Mappings ---

    private fun MessageEntity.toDomain(): ChatMessage {
        val op = operationJson?.let { parseOperationJson(it) }
        return ChatMessage(
            id = id,
            role = role,
            content = content,
            timestamp = timestamp,
            operation = op
        )
    }

    private fun ChatMessage.toEntity(conversationId: String): MessageEntity {
        val opJson = operation?.let { serializeOperationJson(it) }
        return MessageEntity(
            id = id,
            conversationId = conversationId,
            role = role,
            content = content,
            timestamp = timestamp,
            operationJson = opJson,
            taskSnapshotId = taskSnapshot?.id
        )
    }

    private fun TaskWithSteps.toDomain(): NexusTask {
        val logList = try {
            val arr = JSONArray(task.executionLogsJson)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }

        return NexusTask(
            id = task.id,
            title = task.title,
            objective = task.objective,
            agentId = task.agentId,
            status = try { TaskStatus.valueOf(task.status) } catch (e: Exception) { TaskStatus.UNDERSTAND },
            steps = steps.sortedBy { it.stepIndex }.map { it.toDomain() },
            currentStepIndex = task.currentStepIndex,
            createdAt = task.createdAt,
            updatedAt = task.updatedAt,
            executionLogs = logList
        )
    }

    private fun TaskStepEntity.toDomain(): TaskStep {
        val op = operationJson?.let { parseOperationJson(it) }
        return TaskStep(
            id = id,
            title = title,
            description = description,
            operation = op,
            status = try { StepStatus.valueOf(status) } catch (e: Exception) { StepStatus.PENDING },
            verificationMethod = verificationMethod,
            verificationNotes = verificationNotes
        )
    }

    private fun TaskStep.toEntity(taskId: String, index: Int): TaskStepEntity {
        val opJson = operation?.let { serializeOperationJson(it) }
        return TaskStepEntity(
            id = id,
            taskId = taskId,
            stepIndex = index,
            title = title,
            description = description,
            status = status.name,
            verificationMethod = verificationMethod,
            verificationNotes = verificationNotes,
            operationJson = opJson
        )
    }

    private fun serializeOperationJson(op: Operation): String {
        val obj = JSONObject().apply {
            put("id", op.id)
            put("opName", op.opName)
            put("label", op.label)
            put("category", op.category)
            put("risk", op.risk.name)
            put("status", op.status.name)
            put("policyReason", op.policyReason)
            put("output", op.output)
            put("timestamp", op.timestamp)
            val paramsObj = JSONObject()
            op.params.forEach { (k, v) -> paramsObj.put(k, v) }
            put("params", paramsObj)
        }
        return obj.toString()
    }

    private fun parseOperationJson(json: String): Operation? {
        return try {
            val obj = JSONObject(json)
            val paramsMap = mutableMapOf<String, String>()
            val pObj = obj.optJSONObject("params")
            pObj?.keys()?.forEach { k ->
                paramsMap[k] = pObj.optString(k, "")
            }
            Operation(
                id = obj.optString("id", UUID.randomUUID().toString()),
                opName = obj.optString("opName", ""),
                label = obj.optString("label", ""),
                category = obj.optString("category", "workspace"),
                risk = try { RiskLevel.valueOf(obj.optString("risk", "LOW")) } catch (e: Exception) { RiskLevel.LOW },
                params = paramsMap,
                status = try { OpStatus.valueOf(obj.optString("status", "PENDING")) } catch (e: Exception) { OpStatus.PENDING },
                policyReason = obj.optString("policyReason", ""),
                output = obj.optString("output", ""),
                timestamp = obj.optLong("timestamp", System.currentTimeMillis())
            )
        } catch (e: Exception) {
            null
        }
    }
}
