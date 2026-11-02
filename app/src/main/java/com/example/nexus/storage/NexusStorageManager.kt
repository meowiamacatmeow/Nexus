package com.example.nexus.storage

import android.content.Context
import com.example.nexus.model.AuditEntry
import com.example.nexus.model.ChatMessage
import com.example.nexus.model.Conversation
import com.example.nexus.model.MemoryItem
import com.example.nexus.model.PermissionSettings
import com.example.nexus.model.ToolPackage
import com.example.nexus.model.WorkspaceFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class NexusStorageManager(private val context: Context) {

    val nexusDir: File = File(context.filesDir, "NEXUS")
    val workspacesDir: File = File(nexusDir, "workspaces")
    val toolsDir: File = File(nexusDir, "tools")
    val downloadsDir: File = File(nexusDir, "downloads")
    val modelsDir: File = File(nexusDir, "models")
    val logsDir: File = File(nexusDir, "logs")
    val backupsDir: File = File(nexusDir, "backups")
    val nexusDataFile: File = File(nexusDir, "nexus.data")

    init {
        ensureDirectoryStructure()
        seedInitialFiles()
    }

    private fun ensureDirectoryStructure() {
        nexusDir.mkdirs()
        workspacesDir.mkdirs()
        toolsDir.mkdirs()
        downloadsDir.mkdirs()
        modelsDir.mkdirs()
        logsDir.mkdirs()
        backupsDir.mkdirs()
    }

    private fun seedInitialFiles() {
        val defaultWs = File(workspacesDir, "default-workspace")
        if (!defaultWs.exists()) {
            defaultWs.mkdirs()
            File(defaultWs, "README.md").writeText(
                """# PROJECT NEXUS Workspace
Welcome to your local computing environment.
Files created by terminal commands or approved agent tasks reside in this sandbox.

- Isolated environment
- Policy-checked mutations
- Local-first persistence
""".trimIndent()
            )
            File(defaultWs, "config.json").writeText(
                """{
  "project": "NEXUS_INIT",
  "version": "1.0.0",
  "isolated": true
}""".trimIndent()
            )
        }

        val pythonWs = File(workspacesDir, "python-dev")
        if (!pythonWs.exists()) {
            pythonWs.mkdirs()
            File(pythonWs, "main.py").writeText(
                """# NEXUS Python Environment Example
import sys

def main():
    print("NEXUS Sandbox Runtime Active")
    print(f"System Version: {sys.version.split()[0]}")

if __name__ == '__main__':
    main()
""".trimIndent()
            )
            File(pythonWs, "requirements.txt").writeText(
                """# Project dependencies
nexus-core>=0.6.0
requests-mock>=1.0.0
""".trimIndent()
            )
        }
    }

    // --- File operations ---

    fun createWorkspace(name: String): Boolean {
        val safeName = name.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val dir = File(workspacesDir, safeName)
        return dir.mkdirs() || dir.exists()
    }

    fun writeFile(relativePath: String, content: String): Boolean {
        return try {
            val file = File(workspacesDir, relativePath)
            file.parentFile?.mkdirs()
            file.writeText(content)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun readFile(relativePath: String): String? {
        return try {
            val file = File(workspacesDir, relativePath)
            if (file.exists() && file.isFile) file.readText() else null
        } catch (e: Exception) {
            null
        }
    }

    fun deleteItem(relativePath: String): Boolean {
        return try {
            val file = File(workspacesDir, relativePath)
            if (file.exists()) file.deleteRecursively() else false
        } catch (e: Exception) {
            false
        }
    }

    fun listWorkspaceFiles(subPath: String = ""): List<WorkspaceFile> {
        val target = if (subPath.isBlank()) workspacesDir else File(workspacesDir, subPath)
        if (!target.exists()) return emptyList()

        val files = target.listFiles() ?: return emptyList()
        return files.map { file ->
            val rel = file.relativeTo(workspacesDir).path
            WorkspaceFile(
                name = file.name,
                path = file.absolutePath,
                relativePath = rel,
                isDirectory = file.isDirectory,
                sizeBytes = if (file.isFile) file.length() else (file.listFiles()?.size?.toLong() ?: 0L),
                lastModified = file.lastModified()
            )
        }.sortedWith(compareByDescending<WorkspaceFile> { it.isDirectory }.thenBy { it.name.lowercase() })
    }

    // --- nexus.data Export & Import ---

    data class ExportPayload(
        val format: String = "nexus.data",
        val schemaVersion: Int = 5,
        val app: String = "PROJECT NEXUS",
        val exportedAt: Long = System.currentTimeMillis(),
        val permissions: PermissionSettings,
        val conversations: List<Conversation>,
        val auditLog: List<AuditEntry>,
        val memory: List<MemoryItem>,
        val tools: List<ToolPackage>
    )

    fun exportToJson(
        permissions: PermissionSettings,
        conversations: List<Conversation>,
        auditLog: List<AuditEntry>,
        memory: List<MemoryItem>,
        tools: List<ToolPackage>
    ): String {
        val root = JSONObject()
        root.put("format", "nexus.data")
        root.put("schemaVersion", 5)
        root.put("app", "PROJECT NEXUS")
        root.put("exportedAt", System.currentTimeMillis())

        val permObj = JSONObject().apply {
            put("filesystem", permissions.filesystem)
            put("packages", permissions.packages)
            put("network", permissions.network)
            put("workspace", permissions.workspace)
            put("shellScripts", permissions.shellScripts)
        }
        root.put("permissions", permObj)

        val convArray = JSONArray()
        conversations.forEach { conv ->
            val cObj = JSONObject().apply {
                put("id", conv.id)
                put("title", conv.title)
                put("agentId", conv.agentId)
                put("createdAt", conv.createdAt)
                put("updatedAt", conv.updatedAt)
                val mArr = JSONArray()
                conv.messages.forEach { msg ->
                    mArr.put(JSONObject().apply {
                        put("id", msg.id)
                        put("role", msg.role)
                        put("content", msg.content)
                        put("timestamp", msg.timestamp)
                    })
                }
                put("messages", mArr)
            }
            convArray.put(cObj)
        }
        root.put("conversations", convArray)

        val auditArray = JSONArray()
        auditLog.forEach { a ->
            auditArray.put(JSONObject().apply {
                put("id", a.id)
                put("opName", a.opName)
                put("category", a.category)
                put("status", a.status)
                put("detail", a.detail)
                put("timestamp", a.timestamp)
                put("agentId", a.agentId)
            })
        }
        root.put("auditLog", auditArray)

        val memoryArray = JSONArray()
        memory.forEach { m ->
            memoryArray.put(JSONObject().apply {
                put("id", m.id)
                put("key", m.key)
                put("value", m.value)
                put("category", m.category)
                put("source", m.source)
                put("updatedAt", m.updatedAt)
            })
        }
        root.put("memory", memoryArray)

        val jsonStr = root.toString(2)
        nexusDataFile.writeText(jsonStr)
        return jsonStr
    }

    data class ImportResult(
        val success: Boolean,
        val conversations: List<Conversation> = emptyList(),
        val permissions: PermissionSettings? = null,
        val auditLog: List<AuditEntry> = emptyList(),
        val memory: List<MemoryItem> = emptyList(),
        val errorMessage: String? = null
    )

    fun importFromJson(jsonString: String): ImportResult {
        return try {
            val root = JSONObject(jsonString)
            val format = root.optString("format", "")
            if (format != "nexus.data") {
                return ImportResult(false, errorMessage = "Invalid file signature. Expected 'format': 'nexus.data'.")
            }
            val schemaVersion = root.optInt("schemaVersion", 1)
            if (schemaVersion < 1 || schemaVersion > 5) {
                return ImportResult(false, errorMessage = "Unsupported schema version: $schemaVersion")
            }

            // Permissions
            val permObj = root.optJSONObject("permissions")
            val permissions = if (permObj != null) {
                PermissionSettings(
                    filesystem = permObj.optBoolean("filesystem", false),
                    packages = permObj.optBoolean("packages", false),
                    network = permObj.optBoolean("network", false),
                    workspace = permObj.optBoolean("workspace", true),
                    shellScripts = permObj.optBoolean("shellScripts", false)
                )
            } else null

            // Conversations
            val convList = mutableListOf<Conversation>()
            val convArr = root.optJSONArray("conversations")
            if (convArr != null) {
                for (i in 0 until convArr.length()) {
                    val cObj = convArr.getJSONObject(i)
                    val msgList = mutableListOf<ChatMessage>()
                    val mArr = cObj.optJSONArray("messages")
                    if (mArr != null) {
                        for (j in 0 until mArr.length()) {
                            val mObj = mArr.getJSONObject(j)
                            msgList.add(
                                ChatMessage(
                                    id = mObj.optString("id", "msg_${System.currentTimeMillis()}_$j"),
                                    role = mObj.optString("role", "assistant"),
                                    content = mObj.optString("content", ""),
                                    timestamp = mObj.optLong("timestamp", System.currentTimeMillis())
                                )
                            )
                        }
                    }
                    convList.add(
                        Conversation(
                            id = cObj.optString("id", "conv_${System.currentTimeMillis()}_$i"),
                            title = cObj.optString("title", "Imported Conversation"),
                            agentId = cObj.optString("agentId", "developer"),
                            createdAt = cObj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = cObj.optLong("updatedAt", System.currentTimeMillis()),
                            messages = msgList
                        )
                    )
                }
            }

            // Audit
            val auditList = mutableListOf<AuditEntry>()
            val aArr = root.optJSONArray("auditLog")
            if (aArr != null) {
                for (i in 0 until aArr.length()) {
                    val aObj = aArr.getJSONObject(i)
                    auditList.add(
                        AuditEntry(
                            id = aObj.optString("id", "audit_$i"),
                            opName = aObj.optString("opName", "unknown"),
                            category = aObj.optString("category", "system"),
                            status = aObj.optString("status", "APPROVED"),
                            detail = aObj.optString("detail", ""),
                            timestamp = aObj.optLong("timestamp", System.currentTimeMillis()),
                            agentId = aObj.optString("agentId", "system")
                        )
                    )
                }
            }

            // Memory
            val memList = mutableListOf<MemoryItem>()
            val mArr = root.optJSONArray("memory")
            if (mArr != null) {
                for (i in 0 until mArr.length()) {
                    val mObj = mArr.getJSONObject(i)
                    memList.add(
                        MemoryItem(
                            id = mObj.optString("id", "mem_$i"),
                            key = mObj.optString("key", "key_$i"),
                            value = mObj.optString("value", ""),
                            category = mObj.optString("category", "General"),
                            source = mObj.optString("source", "import"),
                            updatedAt = mObj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            ImportResult(
                success = true,
                conversations = convList,
                permissions = permissions,
                auditLog = auditList,
                memory = memList
            )
        } catch (e: Exception) {
            ImportResult(false, errorMessage = "Parse error: ${e.message}")
        }
    }

    fun createBackupSnapshot(jsonContent: String): String {
        val fileName = "nexus_backup_${System.currentTimeMillis()}.data"
        val file = File(backupsDir, fileName)
        file.writeText(jsonContent)
        return fileName
    }

    fun listBackups(): List<String> {
        return backupsDir.listFiles()?.map { it.name }?.sortedDescending() ?: emptyList()
    }

    /**
     * Exports a workspace directory to a standard ZIP file in backupsDir.
     */
    fun exportWorkspaceToZip(workspaceRelativePath: String): File? {
        val targetDir = if (workspaceRelativePath.isBlank()) workspacesDir else File(workspacesDir, workspaceRelativePath)
        if (!targetDir.exists() || !targetDir.isDirectory) return null
        val safeName = targetDir.name.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val zipFile = File(backupsDir, "${safeName}_export_${System.currentTimeMillis()}.zip")
        return try {
            java.util.zip.ZipOutputStream(java.io.FileOutputStream(zipFile)).use { zos ->
                targetDir.walkTopDown().forEach { file ->
                    val relPath = file.relativeTo(targetDir).path
                    if (relPath.isNotEmpty()) {
                        if (file.isDirectory) {
                            zos.putNextEntry(java.util.zip.ZipEntry("$relPath/"))
                            zos.closeEntry()
                        } else {
                            zos.putNextEntry(java.util.zip.ZipEntry(relPath))
                            file.inputStream().use { it.copyTo(zos) }
                            zos.closeEntry()
                        }
                    }
                }
            }
            zipFile
        } catch (e: Exception) {
            zipFile.delete()
            null
        }
    }

    /**
     * Imports files from a ZIP archive into a workspace safely guarding against Zip Slip.
     */
    fun importWorkspaceFromZip(zipFile: File, targetWorkspaceName: String): Boolean {
        val destDir = File(workspacesDir, targetWorkspaceName)
        destDir.mkdirs()
        return try {
            java.util.zip.ZipInputStream(zipFile.inputStream()).use { zis ->
                var entry: java.util.zip.ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val outFile = File(destDir, entry.name)
                    // Security check: Zip Slip vulnerability prevention
                    if (!outFile.canonicalPath.startsWith(destDir.canonicalPath)) {
                        throw SecurityException("Zip Slip detected in archive: ${entry.name}")
                    }
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        outFile.outputStream().use { zis.copyTo(it) }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
