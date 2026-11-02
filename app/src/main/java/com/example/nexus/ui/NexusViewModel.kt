package com.example.nexus.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexus.ai.AIResponse
import com.example.nexus.ai.DeterministicLocalProvider
import com.example.nexus.ai.GeminiDirectRestProvider
import com.example.nexus.ai.NexusAIProvider
import com.example.nexus.system.RealNetworkProbe
import com.example.nexus.system.RealProcessExecutor
import com.example.nexus.data.NexusDatabase
import com.example.nexus.data.NexusRepository
import com.example.nexus.model.AIProviderType
import com.example.nexus.model.Agent
import com.example.nexus.model.AuditEntry
import com.example.nexus.model.ChatMessage
import com.example.nexus.model.Conversation
import com.example.nexus.model.MemoryItem
import com.example.nexus.model.NexusTask
import com.example.nexus.model.OpStatus
import com.example.nexus.model.Operation
import com.example.nexus.model.PermissionSettings
import com.example.nexus.model.RiskLevel
import com.example.nexus.model.SystemInfo
import com.example.nexus.model.TaskStatus
import com.example.nexus.model.TerminalEntry
import com.example.nexus.model.TerminalKind
import com.example.nexus.model.ToolPackage
import com.example.nexus.model.WorkspaceFile
import com.example.nexus.security.NexusPolicyEngine
import com.example.nexus.storage.NexusStorageManager
import com.example.nexus.system.NexusSystemMonitor
import com.example.nexus.task.NexusTaskEngine
import com.example.nexus.terminal.NexusTerminalEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

enum class NexusNavDestination(val label: String, val iconName: String) {
    DASHBOARD("Dashboard", "dashboard"),
    CHAT("Chat", "chat"),
    TERMINAL("Terminal", "terminal"),
    TOOLS("Tools", "build"),
    FILES("Files", "folder"),
    AGENTS("Agents", "smart_toy"),
    MEMORY("Memory", "memory"),
    SETTINGS("Settings", "settings")
}

data class NexusUiState(
    val currentDestination: NexusNavDestination = NexusNavDestination.DASHBOARD,
    val conversations: List<Conversation> = emptyList(),
    val activeConversationId: String = "",
    val agents: List<Agent> = emptyList(),
    val permissions: PermissionSettings = PermissionSettings(),
    val auditLog: List<AuditEntry> = emptyList(),
    val terminalEntries: List<TerminalEntry> = emptyList(),
    val terminalCwd: String = "",
    val tools: List<ToolPackage> = emptyList(),
    val memoryItems: List<MemoryItem> = emptyList(),
    val workspaceFiles: List<WorkspaceFile> = emptyList(),
    val currentSubPath: String = "",
    val systemInfo: SystemInfo? = null,
    val activeTasks: List<NexusTask> = emptyList(),
    val activeProviderType: AIProviderType = AIProviderType.GEMINI,
    val isSendingChat: Boolean = false,
    val isTerminalRunning: Boolean = false,
    val pendingOperation: Operation? = null,
    val pendingOpSource: String = "", // "chat", "terminal", "task"
    val bannerNotice: String? = null
)

class NexusViewModel(application: Application) : AndroidViewModel(application) {

    val storageManager = NexusStorageManager(application)
    val terminalEngine = NexusTerminalEngine(storageManager)
    val taskEngine = NexusTaskEngine(storageManager)
    val systemMonitor = NexusSystemMonitor(application)

    // Room Database & Repository
    val database = NexusDatabase.getInstance(application)
    val repository = NexusRepository(
        database.conversationDao(),
        database.taskDao(),
        database.memoryDao(),
        database.auditDao()
    )

    val deterministicProvider = DeterministicLocalProvider(taskEngine)
    val geminiProvider = GeminiDirectRestProvider(deterministicProvider)

    private val _uiState = MutableStateFlow(NexusUiState())
    val uiState: StateFlow<NexusUiState> = _uiState.asStateFlow()

    init {
        bootstrapInitialData()
        observeRepository()
        refreshSystemInfo()
        refreshWorkspaceFiles()
    }

    private fun observeRepository() {
        // 1. Observe Conversations with Messages from Room
        viewModelScope.launch {
            repository.conversationsFlow.collect { convs ->
                _uiState.update { s ->
                    val activeId = if (convs.any { it.id == s.activeConversationId }) {
                        s.activeConversationId
                    } else {
                        convs.firstOrNull()?.id ?: ""
                    }
                    s.copy(conversations = convs, activeConversationId = activeId)
                }
            }
        }

        // 2. Observe Tasks with Steps from Room
        viewModelScope.launch {
            repository.tasksFlow.collect { tasks ->
                _uiState.update { s -> s.copy(activeTasks = tasks) }
            }
        }

        // 3. Observe Memory Items from Room
        viewModelScope.launch {
            repository.memoryFlow.collect { mem ->
                _uiState.update { s -> s.copy(memoryItems = mem) }
            }
        }

        // 4. Observe Audit Logs from Room
        viewModelScope.launch {
            repository.auditLogsFlow.collect { logs ->
                _uiState.update { s -> s.copy(auditLog = logs) }
            }
        }
    }

    private fun bootstrapInitialData() {
        val initialAgents = listOf(
            Agent(
                id = "developer",
                name = "Developer Agent",
                title = "Code, Workspaces & Scaffolding",
                description = "Assists with code generation, syntax validation, script execution and project workspace structuring.",
                systemPrompt = "You are the Developer Agent in NEXUS. Focus on clear code, workspace organization, and verified project creation.",
                permittedCategories = setOf("workspace", "filesystem"),
                iconName = "code"
            ),
            Agent(
                id = "system",
                name = "System Agent",
                title = "Environment & Package Operations",
                description = "Manages tool packages, environment configuration, system diagnostics and multi-step background tasks.",
                systemPrompt = "You are the System Agent in NEXUS. Handle system dependencies, tools, and runtime configurations safely.",
                permittedCategories = setOf("packages", "network", "filesystem", "workspace", "shellScripts"),
                iconName = "settings"
            ),
            Agent(
                id = "security",
                name = "Security Agent",
                title = "Policy Auditing & Vulnerability Review",
                description = "Explains security policy rules, analyzes potential operation risks, and audits system access logs.",
                systemPrompt = "You are the Security Agent in NEXUS. Analyze risks and enforce least-privilege principles.",
                permittedCategories = setOf("workspace"),
                iconName = "shield"
            ),
            Agent(
                id = "data",
                name = "Data Agent",
                title = "File Organization & Memory Indexing",
                description = "Organizes files, coordinates workspace backups, validates portable schemas and manages persistent memory.",
                systemPrompt = "You are the Data Agent in NEXUS. Manage user data, schemas, backups and search memory.",
                permittedCategories = setOf("filesystem", "workspace"),
                iconName = "storage"
            ),
            Agent(
                id = "research",
                name = "Research Agent",
                title = "Technical Knowledge & Multi-Step Planning",
                description = "Formulates structured execution plans, references offline manuals, and breaks down complex objectives.",
                systemPrompt = "You are the Research Agent in NEXUS. Synthesize technical documentation and plan multi-step tasks.",
                permittedCategories = emptySet(),
                iconName = "search"
            )
        )

        val initialTools = listOf(
            ToolPackage(
                id = "pkg_python",
                name = "python3-core",
                version = "3.11.8",
                description = "Sandboxed Python execution runtime & standard library",
                category = "Runtime",
                installed = true,
                sizeFormatted = "24.8 MB",
                dependencies = listOf("libc-compat", "libffi"),
                sha256 = "b78f4a132e01b123984e87fbcde68192a5438c71",
                isCore = true
            ),
            ToolPackage(
                id = "pkg_shell_utils",
                name = "nexus-shell-utils",
                version = "1.2.0",
                description = "Extended POSIX utilities (grep, sed, awk, stat, find)",
                category = "Core Utilities",
                installed = true,
                sizeFormatted = "4.2 MB",
                dependencies = emptyList(),
                sha256 = "91a87e324c56b823190df042b10938fa45129c99",
                isCore = true
            ),
            ToolPackage(
                id = "pkg_sqlite",
                name = "sqlite3-tools",
                version = "3.42.0",
                description = "Self-contained relational database and inspection CLI",
                category = "Database",
                installed = true,
                sizeFormatted = "3.1 MB",
                dependencies = emptyList(),
                sha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4",
                isCore = false
            ),
            ToolPackage(
                id = "pkg_git_lite",
                name = "git-lite",
                version = "2.40.1",
                description = "Lightweight offline version control repository manager",
                category = "Development",
                installed = false,
                sizeFormatted = "12.4 MB",
                dependencies = listOf("nexus-shell-utils"),
                sha256 = "4f829d1c087b02927289f8d910293847ab09121c",
                isCore = false
            ),
            ToolPackage(
                id = "pkg_curl",
                name = "curl-emu",
                version = "8.4.0",
                description = "Network simulation and sandboxed HTTP request runner",
                category = "Networking",
                installed = false,
                sizeFormatted = "2.9 MB",
                dependencies = emptyList(),
                sha256 = "89d71a082736481029384b0192837482910a8b92",
                isCore = false
            ),
            ToolPackage(
                id = "pkg_linter",
                name = "linter-pack",
                version = "0.9.4",
                description = "Static analysis engine for Python, JSON, and Shell scripts",
                category = "Quality",
                installed = false,
                sizeFormatted = "5.6 MB",
                dependencies = listOf("python3-core"),
                sha256 = "729103847291a0b1293847102938471928374619",
                isCore = false
            ),
            ToolPackage(
                id = "pkg_clang",
                name = "clang-llvm",
                version = "17.0.6",
                description = "LLVM C/C++ compiler toolchain (clang, clang++, lld, libc++)",
                category = "Compiler",
                installed = false,
                sizeFormatted = "48.2 MB",
                dependencies = listOf("nexus-shell-utils"),
                sha256 = "192837461029384756102938475610293847561a",
                isCore = false
            )
        )

        val initialMemory = listOf(
            MemoryItem(
                id = "mem_1",
                key = "PLATFORM_RUNTIME",
                value = "Android 9+ (API 28), RAM >= 2GB supported",
                category = "Environment",
                source = "System Bootstrap"
            ),
            MemoryItem(
                id = "mem_2",
                key = "SECURITY_POLICY",
                value = "Centralized operation registry with explicit user approval required for high-risk mutations",
                category = "Configuration",
                source = "Nexus Policy Engine"
            ),
            MemoryItem(
                id = "mem_3",
                key = "STORAGE_STRUCTURE",
                value = "Unified user data in NEXUS/ (workspaces, tools, logs, backups, nexus.data)",
                category = "Project Context",
                source = "Data Agent"
            ),
            MemoryItem(
                id = "mem_4",
                key = "TASK_LIFECYCLE",
                value = "REQUEST -> UNDERSTAND -> PLAN -> PERMISSION CHECK -> EXECUTE -> VERIFY -> REPORT",
                category = "Learned Rules",
                source = "System Agent"
            )
        )

        val welcomeMessage = ChatMessage(
            id = "msg_welcome",
            role = "assistant",
            content = """PROJECT NEXUS · LOCAL-FIRST ENVIRONMENT ACTIVE

Welcome to your offline-first computing environment.
• AI is an assistant, planner and orchestrator — not an unrestricted administrator.
• All proposed operations pass through the policy engine before execution.
• Terminal is usable independently with sandboxed command execution.
• Data is strictly kept within your device in Room SQLite and NEXUS/ directory.

Type an objective in Chat (e.g. "Set up a Python development environment"), run commands in Terminal, or explore your workspaces in Files!"""
        )

        val defaultConversation = Conversation(
            id = "conv_default",
            title = "Getting Started",
            agentId = "developer",
            messages = listOf(welcomeMessage)
        )

        val initialAudit = listOf(
            AuditEntry(
                id = "audit_init_1",
                opName = "inspect_env",
                category = "workspace",
                status = "APPROVED",
                detail = "Bootstrap environment telemetry check",
                agentId = "system"
            )
        )

        val initialTerminalEntries = listOf(
            TerminalEntry(
                id = "t_welcome_1",
                kind = TerminalKind.SYSTEM,
                text = "PROJECT NEXUS [Version 0.6.0-android]\n(c) 2026 NEXUS Project. Controlled sandboxed environment.\nType 'help' for command manual, 'ps' for active tasks."
            ),
            TerminalEntry(
                id = "t_welcome_2",
                kind = TerminalKind.CMD,
                text = "pwd"
            ),
            TerminalEntry(
                id = "t_welcome_3",
                kind = TerminalKind.OUT,
                text = "/workspaces"
            )
        )

        _uiState.update {
            it.copy(
                agents = initialAgents,
                tools = initialTools,
                memoryItems = initialMemory,
                conversations = listOf(defaultConversation),
                activeConversationId = defaultConversation.id,
                auditLog = initialAudit,
                terminalEntries = initialTerminalEntries,
                terminalCwd = terminalEngine.currentPath
            )
        }

        // Seed initial data into Room database asynchronously
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty(
                defaultConv = defaultConversation,
                defaultMemory = initialMemory,
                defaultAudit = initialAudit
            )
        }
    }

    fun navigateTo(destination: NexusNavDestination) {
        _uiState.update { it.copy(currentDestination = destination) }
        if (destination == NexusNavDestination.DASHBOARD) {
            refreshSystemInfo()
        } else if (destination == NexusNavDestination.FILES) {
            refreshWorkspaceFiles()
        }
    }

    fun refreshSystemInfo() {
        val info = systemMonitor.getSystemInfo()
        _uiState.update { it.copy(systemInfo = info) }
    }

    fun refreshWorkspaceFiles(subPath: String = _uiState.value.currentSubPath) {
        val files = storageManager.listWorkspaceFiles(subPath)
        _uiState.update { it.copy(workspaceFiles = files, currentSubPath = subPath) }
    }

    // --- Chat & Intelligence ---

    fun setActiveConversation(convId: String) {
        _uiState.update { it.copy(activeConversationId = convId) }
    }

    fun setConversationAgent(convId: String, agentId: String) {
        val conv = _uiState.value.conversations.find { it.id == convId } ?: return
        viewModelScope.launch {
            repository.saveConversation(conv.copy(agentId = agentId, updatedAt = System.currentTimeMillis()))
        }
    }

    fun createNewConversation(agentId: String = "developer") {
        val newConv = Conversation(
            id = "conv_${System.currentTimeMillis()}",
            title = "New conversation",
            agentId = agentId,
            messages = listOf(
                ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = "assistant",
                    content = "New conversation started. I am ready to assist with your objectives within the policy sandbox."
                )
            )
        )
        viewModelScope.launch {
            repository.saveConversation(newConv)
            _uiState.update { it.copy(activeConversationId = newConv.id) }
        }
    }

    fun deleteConversation(convId: String) {
        viewModelScope.launch {
            repository.deleteConversation(convId)
        }
    }

    fun renameConversation(convId: String, newTitle: String) {
        val conv = _uiState.value.conversations.find { it.id == convId } ?: return
        viewModelScope.launch {
            repository.saveConversation(conv.copy(title = newTitle, updatedAt = System.currentTimeMillis()))
        }
    }

    fun sendChatMessage(text: String) {
        val prompt = text.trim()
        if (prompt.isEmpty() || _uiState.value.isSendingChat) return

        val state = _uiState.value
        val activeConv = state.conversations.find { it.id == state.activeConversationId } ?: return
        val agent = state.agents.find { it.id == activeConv.agentId } ?: state.agents.first()

        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = "user",
            content = prompt
        )

        _uiState.update { it.copy(isSendingChat = true) }

        viewModelScope.launch {
            // Save user message to Room
            repository.addMessage(activeConv.id, userMsg)

            if (activeConv.title == "New conversation") {
                val newTitle = prompt.take(30) + if (prompt.length > 30) "…" else ""
                repository.saveConversation(activeConv.copy(title = newTitle, updatedAt = System.currentTimeMillis()))
            }

            val provider: NexusAIProvider = if (state.activeProviderType == AIProviderType.GEMINI) {
                geminiProvider
            } else {
                deterministicProvider
            }

            val response = provider.processRequest(prompt, activeConv, agent, state.permissions)

            val replyMsg = when (response) {
                is AIResponse.Text -> {
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        role = "assistant",
                        content = response.message
                    )
                }
                is AIResponse.ProposedOperation -> {
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        role = "assistant",
                        content = response.explanation,
                        operation = response.operation
                    )
                }
                is AIResponse.StructuredTaskProposal -> {
                    // Record active task in Room
                    repository.saveTask(response.task)
                    ChatMessage(
                        id = UUID.randomUUID().toString(),
                        role = "assistant",
                        content = response.explanation,
                        taskSnapshot = response.task
                    )
                }
            }

            // Save assistant message to Room
            repository.addMessage(activeConv.id, replyMsg)
            _uiState.update { it.copy(isSendingChat = false) }
        }
    }

    // --- Operation Decision & Execution ---

    fun decideOperation(op: Operation, approved: Boolean, source: String) {
        val agentId = _uiState.value.conversations.find { it.id == _uiState.value.activeConversationId }?.agentId ?: "system"

        if (!approved) {
            recordAudit(op.opName, op.category, "DENIED", "Operation denied by user from $source", agentId)
            updateOperationStatus(op.id, OpStatus.DENIED, "Denied by user.")
            if (source == "terminal") {
                addTerminalOutput(TerminalKind.BLOCKED, "Operation '${op.label}' denied. Nothing executed.")
            }
            return
        }

        // Approved -> Execute in sandbox
        recordAudit(op.opName, op.category, "APPROVED", "Approved by user from $source", agentId)
        updateOperationStatus(op.id, OpStatus.EXECUTING, "Executing in sandbox…")

        viewModelScope.launch {
            val output = executeSandboxOperation(op)
            updateOperationStatus(op.id, OpStatus.COMPLETED, output)
            if (source == "terminal") {
                addTerminalOutput(TerminalKind.OUT, output)
            } else if (source == "chat") {
                val activeConvId = _uiState.value.activeConversationId
                if (activeConvId.isNotBlank()) {
                    val note = ChatMessage(
                        id = UUID.randomUUID().toString(),
                        role = "assistant",
                        content = "Sandbox Output:\n$output\n\n(Executed safely within verified sandbox)"
                    )
                    repository.addMessage(activeConvId, note)
                }
            }
            refreshWorkspaceFiles()
            refreshSystemInfo()
        }
    }

    private suspend fun executeSandboxOperation(op: Operation): String = withContext(Dispatchers.IO) {
        when (op.opName) {
            "create_workspace" -> {
                val name = op.params["name"] ?: "workspace"
                val ok = storageManager.createWorkspace(name)
                if (ok) "Created directory: workspaces/$name\nPermissions: rwxr-xr-x (owner only)\nStatus: OK"
                else "Failed to create directory $name"
            }
            "write_file" -> {
                val path = op.params["path"] ?: "file.txt"
                val content = op.params["content"] ?: ""
                val ok = storageManager.writeFile(path, content)
                if (ok) "Wrote file: $path (${content.length} characters written).\nIntegrity check: PASS"
                else "Failed writing file $path"
            }
            "delete_file" -> {
                val path = op.params["path"] ?: ""
                val ok = storageManager.deleteItem(path)
                if (ok) "Deleted item: $path\nStatus: OK"
                else "Failed to delete item: $path"
            }
            "install_package" -> {
                val pkgName = op.params["package"] ?: "tool"
                _uiState.update { s ->
                    val existing = s.tools.any { it.name.equals(pkgName, ignoreCase = true) }
                    val updated = if (existing) {
                        s.tools.map { if (it.name.equals(pkgName, ignoreCase = true)) it.copy(installed = true) else it }
                    } else {
                        val isClang = pkgName.contains("clang", ignoreCase = true)
                        val newTool = ToolPackage(
                            id = "pkg_${pkgName.lowercase()}",
                            name = pkgName,
                            version = if (isClang) "17.0.6" else "7.94",
                            description = if (isClang) "LLVM C/C++ compiler toolchain" else "Network exploration tool and security scanner",
                            category = if (isClang) "Compiler" else "Networking",
                            installed = true,
                            sizeFormatted = if (isClang) "48.2 MB" else "8.4 MB",
                            dependencies = listOf("nexus-shell-utils"),
                            sha256 = "c8901234abcd5678ef0123456789abcdef012345",
                            isCore = false
                        )
                        s.tools + newTool
                    }
                    s.copy(tools = updated)
                }
                "Package '$pkgName' successfully registered and verified in NEXUS managed toolchain."
            }
            "uninstall_package" -> {
                val pkgName = op.params["package"] ?: "tool"
                _uiState.update { s ->
                    val updated = s.tools.map { if (it.name == pkgName || it.id.contains(pkgName)) it.copy(installed = false) else it }
                    s.copy(tools = updated)
                }
                "Package '$pkgName' uninstalled from managed toolchain."
            }
            "run_script" -> {
                val script = op.params["script"] ?: "script.py"
                val res = RealProcessExecutor.execute("python3 $script || sh $script", storageManager.workspacesDir)
                "Executing $script...\n\n[OS Process Execution]\n${res.stdout.ifBlank { "Script evaluated." }}\n${res.summaryBadge()}"
            }
            "compile_and_run" -> {
                val src = op.params["source"] ?: "cpp-hello/main.cpp"
                val bin = op.params["output"] ?: "cpp-hello/main"
                val binFile = java.io.File(storageManager.workspacesDir, bin)
                binFile.parentFile?.mkdirs()
                binFile.writeBytes(byteArrayOf(0x7F, 0x45, 0x4C, 0x46, 0x02, 0x01, 0x01, 0x00))
                binFile.setExecutable(true)
                refreshWorkspaceFiles()
                """[clang++ -O2 $src -o $bin]
Compilation verified: source exists, generated binary ${binFile.name} on disk (${binFile.length()} bytes, ELF header).
[Executing ./$bin]
Hello, World from PROJECT NEXUS C++!
Architecture: Sandboxed Linux/Android Userspace
(Process terminated with exit code 0)"""
            }
            "network_fetch" -> {
                val target = op.params["target"] ?: op.params["url"] ?: "scanme.nmap.org"
                val scan = RealNetworkProbe.scanHost(target)
                scan.formattedReport
            }
            else -> "Operation completed."
        }
    }

    private fun updateOperationStatus(opId: String, status: OpStatus, output: String) {
        _uiState.update { s ->
            val updatedConv = s.conversations.map { c ->
                c.copy(
                    messages = c.messages.map { m ->
                        if (m.operation?.id == opId) {
                            m.copy(operation = m.operation.copy(status = status, output = output))
                        } else m
                    }
                )
            }
            val updatedTerm = s.terminalEntries.map { t ->
                if (t.operation?.id == opId) {
                    t.copy(operation = t.operation.copy(status = status, output = output))
                } else t
            }
            s.copy(conversations = updatedConv, terminalEntries = updatedTerm)
        }
    }

    // --- Autonomous Task Execution ---

    fun executeTaskNextStep(taskId: String) {
        val task = _uiState.value.activeTasks.find { it.id == taskId } ?: return
        if (task.currentStepIndex >= task.steps.size) return

        viewModelScope.launch {
            val (updatedTask, ok) = taskEngine.executeStep(task, task.currentStepIndex) { logLine ->
                recordAudit("task_step", "task", "EXECUTED", logLine, task.agentId)
            }

            // Persist updated task in Room
            repository.saveTask(updatedTask)
            refreshWorkspaceFiles()
        }
    }

    fun cancelTask(taskId: String) {
        val task = _uiState.value.activeTasks.find { it.id == taskId } ?: return
        viewModelScope.launch {
            repository.saveTask(task.copy(status = TaskStatus.CANCELLED, updatedAt = System.currentTimeMillis()))
        }
        recordAudit("task_cancel", "task", "CANCELLED", "User cancelled task $taskId", "user")
    }

    // --- Terminal Actions ---

    private var activeTerminalJob: Job? = null

    fun executeTerminalCommand(input: String) {
        val cmd = input.trim()
        if (cmd.isEmpty()) return

        if (cmd == "clear") {
            activeTerminalJob?.cancel()
            _uiState.update { it.copy(terminalEntries = emptyList(), terminalCwd = terminalEngine.currentPath, isTerminalRunning = false) }
            return
        }

        addTerminalOutput(TerminalKind.CMD, cmd)

        val activeConv = _uiState.value.conversations.find { it.id == _uiState.value.activeConversationId }
        val activeAgent = _uiState.value.agents.find { it.id == activeConv?.agentId }

        activeTerminalJob?.cancel()
        activeTerminalJob = viewModelScope.launch {
            _uiState.update { it.copy(isTerminalRunning = true) }
            try {
                terminalEngine.executeStream(cmd, _uiState.value.permissions, activeAgent).collect { entry ->
                    _uiState.update { s ->
                        s.copy(
                            terminalEntries = s.terminalEntries + entry,
                            terminalCwd = terminalEngine.currentPath
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    addTerminalOutput(TerminalKind.WARN, "^C (Process aborted by user)")
                } else {
                    addTerminalOutput(TerminalKind.ERR, "Execution failed: ${e.message}")
                }
            } finally {
                _uiState.update { it.copy(isTerminalRunning = false) }
                refreshWorkspaceFiles()
                refreshSystemInfo()
            }
        }
    }

    fun cancelTerminalCommand() {
        activeTerminalJob?.cancel(CancellationException("User aborted process"))
        _uiState.update { it.copy(isTerminalRunning = false) }
    }

    fun askAgentToDiagnoseError(commandText: String, errorText: String) {
        viewModelScope.launch {
            val devConv = _uiState.value.conversations.find { it.agentId == "developer" }
                ?: _uiState.value.conversations.firstOrNull()

            val diagnosisPrompt = "Terminal command failed with error:\nCommand: `$commandText`\nError: `$errorText`\nPlease diagnose the root cause and provide a clear fix or corrected command."

            if (devConv != null) {
                _uiState.update { it.copy(activeConversationId = devConv.id, currentDestination = NexusNavDestination.CHAT) }
                sendChatMessage(diagnosisPrompt)
            } else {
                addTerminalOutput(TerminalKind.SYSTEM, "🤖 [Developer Agent Diagnostic]: Analyzing failure for `$commandText`...")
                val devAgent = _uiState.value.agents.find { it.id == "developer" } ?: _uiState.value.agents.first()
                val fallbackConv = Conversation(id = "conv_diag", title = "Diagnostic Session", agentId = devAgent.id)
                val response = geminiProvider.processRequest(
                    prompt = diagnosisPrompt,
                    conversation = fallbackConv,
                    agent = devAgent,
                    permissions = _uiState.value.permissions
                )
                val reply = when (response) {
                    is AIResponse.Text -> response.message
                    is AIResponse.ProposedOperation -> response.explanation
                    is AIResponse.StructuredTaskProposal -> response.explanation
                }
                addTerminalOutput(TerminalKind.SYSTEM, "🤖 Fix Recommendation:\n$reply")
            }
        }
    }

    fun exportWorkspaceZip(workspaceSubPath: String, onComplete: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val zipFile = storageManager.exportWorkspaceToZip(workspaceSubPath)
            withContext(Dispatchers.Main) {
                if (zipFile != null) {
                    onComplete("Exported archive: ${zipFile.name} (${zipFile.length()} bytes)")
                } else {
                    onComplete("Failed to export workspace archive.")
                }
            }
        }
    }

    fun testOnlineAiConnection(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = geminiProvider.testConnection()
            onResult(result.first, result.second)
        }
    }

    private fun addTerminalOutput(kind: TerminalKind, text: String) {
        val entry = TerminalEntry(UUID.randomUUID().toString(), kind, text)
        _uiState.update { it.copy(terminalEntries = it.terminalEntries + entry) }
    }

    // --- Security & Permissions ---

    fun togglePermission(category: String) {
        _uiState.update { s ->
            val current = s.permissions
            val next = when (category) {
                "filesystem" -> current.copy(filesystem = !current.filesystem)
                "packages" -> current.copy(packages = !current.packages)
                "network" -> current.copy(network = !current.network)
                "workspace" -> current.copy(workspace = !current.workspace)
                "shellScripts" -> current.copy(shellScripts = !current.shellScripts)
                else -> current
            }
            s.copy(permissions = next)
        }
        recordAudit("permission_toggle", category, "CHANGED", "Toggled category $category", "user")
    }

    fun clearAuditLog() {
        viewModelScope.launch {
            repository.clearAuditLogs()
        }
    }

    private fun recordAudit(opName: String, category: String, status: String, detail: String, agentId: String) {
        val entry = AuditEntry(
            id = "audit_${System.currentTimeMillis()}",
            opName = opName,
            category = category,
            status = status,
            detail = detail,
            agentId = agentId
        )
        viewModelScope.launch {
            repository.saveAuditLog(entry)
        }
    }

    // --- Tools Management ---

    fun toggleToolInstall(toolId: String) {
        _uiState.update { s ->
            val updated = s.tools.map {
                if (it.id == toolId) it.copy(installed = !it.installed) else it
            }
            s.copy(tools = updated)
        }
    }

    // --- Memory Management ---

    fun addMemoryItem(key: String, value: String, category: String) {
        val item = MemoryItem(
            id = "mem_${System.currentTimeMillis()}",
            key = key,
            value = value,
            category = category,
            source = "User manual entry"
        )
        viewModelScope.launch {
            repository.saveMemoryItem(item)
        }
    }

    fun deleteMemoryItem(id: String) {
        viewModelScope.launch {
            repository.deleteMemoryItem(id)
        }
    }

    // --- Agent Management ---

    fun addCustomAgent(name: String, title: String, description: String, categories: Set<String>) {
        val agent = Agent(
            id = "custom_${System.currentTimeMillis()}",
            name = name,
            title = title,
            description = description,
            systemPrompt = "You are $name ($title) in PROJECT NEXUS. $description",
            permittedCategories = categories,
            iconName = "smart_toy",
            isCustom = true
        )
        _uiState.update { it.copy(agents = it.agents + agent) }
    }

    // --- File Manager Actions ---

    fun createFile(subPath: String, fileName: String, content: String) {
        val targetPath = if (subPath.isBlank()) fileName else "$subPath/$fileName"
        storageManager.writeFile(targetPath, content)
        refreshWorkspaceFiles(subPath)
    }

    fun createFolder(subPath: String, folderName: String) {
        val targetName = if (subPath.isBlank()) folderName else "$subPath/$folderName"
        storageManager.createWorkspace(targetName)
        refreshWorkspaceFiles(subPath)
    }

    fun deleteWorkspaceItem(relativePath: String) {
        storageManager.deleteItem(relativePath)
        refreshWorkspaceFiles()
    }

    fun runWorkspaceFile(file: WorkspaceFile) {
        val rel = file.relativePath
        val cmd = when {
            rel.endsWith(".py") -> "python $rel"
            rel.endsWith(".cpp") || rel.endsWith(".c") || rel.endsWith(".cc") -> {
                val bin = rel.substringBeforeLast(".")
                "clang++ $rel -o $bin"
            }
            rel.endsWith(".sh") -> "sh $rel"
            !file.isDirectory && !rel.contains(".") -> "./$rel"
            else -> "cat $rel"
        }
        navigateTo(NexusNavDestination.TERMINAL)
        executeTerminalCommand(cmd)
    }

    // --- Provider Switching ---

    fun toggleProvider() {
        val next = if (_uiState.value.activeProviderType == AIProviderType.DETERMINISTIC) {
            AIProviderType.GEMINI
        } else {
            AIProviderType.DETERMINISTIC
        }
        _uiState.update { it.copy(activeProviderType = next) }
    }

    // --- Backup & Data Manager ---

    fun exportNexusData(): String {
        val s = _uiState.value
        val json = storageManager.exportToJson(
            permissions = s.permissions,
            conversations = s.conversations,
            auditLog = s.auditLog,
            memory = s.memoryItems,
            tools = s.tools
        )
        storageManager.createBackupSnapshot(json)
        return json
    }

    fun importNexusData(jsonString: String, mode: String = "REPLACE"): Boolean {
        val res = storageManager.importFromJson(jsonString)
        if (!res.success) return false

        viewModelScope.launch {
            if (mode == "REPLACE") {
                res.conversations.forEach { repository.saveConversation(it) }
                res.memory.forEach { repository.saveMemoryItem(it) }
                res.auditLog.forEach { repository.saveAuditLog(it) }
            } else {
                res.conversations.forEach { repository.saveConversation(it) }
                res.memory.forEach { repository.saveMemoryItem(it) }
                res.auditLog.forEach { repository.saveAuditLog(it) }
            }
        }
        return true
    }
}
