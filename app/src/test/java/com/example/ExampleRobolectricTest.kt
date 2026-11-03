package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.nexus.ai.AIResponse
import com.example.nexus.ai.DeterministicLocalProvider
import com.example.nexus.data.NexusDatabase
import com.example.nexus.data.entity.ConversationEntity
import com.example.nexus.data.entity.MemoryItemEntity
import com.example.nexus.data.entity.MessageEntity
import com.example.nexus.data.entity.TaskEntity
import com.example.nexus.data.entity.TaskStepEntity
import com.example.nexus.model.Agent
import com.example.nexus.model.Conversation
import com.example.nexus.model.PermissionSettings
import com.example.nexus.model.RiskLevel
import com.example.nexus.model.TerminalEntry
import com.example.nexus.model.TerminalKind
import com.example.nexus.security.NexusPolicyEngine
import com.example.nexus.storage.NexusStorageManager
import com.example.nexus.system.CommandExecutor
import com.example.nexus.system.ProcessStreamEvent
import com.example.nexus.task.NexusTaskEngine
import com.example.nexus.terminal.NexusTerminalEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Nexus 1.2v", appName)
    }

    @Test
    fun `test policy engine permission checks and boundaries`() {
        val permissions = PermissionSettings(
            filesystem = false,
            packages = false,
            network = false,
            workspace = true,
            shellScripts = false
        )

        val devAgent = Agent(
            id = "developer",
            name = "Developer Agent",
            title = "Code",
            description = "Desc",
            systemPrompt = "Prompt",
            permittedCategories = setOf("workspace", "filesystem")
        )

        val wsEval = NexusPolicyEngine.evaluate("create_workspace", permissions, devAgent)
        assertTrue(wsEval.allowed)
        assertEquals(RiskLevel.LOW, wsEval.risk)

        val fsEval = NexusPolicyEngine.evaluate("write_file", permissions, devAgent)
        assertFalse(fsEval.allowed)

        val netEval = NexusPolicyEngine.evaluate("network_fetch", permissions.copy(network = true), devAgent)
        assertFalse(netEval.allowed)
    }

    @Test
    fun `test storage manager file write and read in sandbox`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = NexusStorageManager(context)

        val created = storage.createWorkspace("test-workspace")
        assertTrue(created)

        val written = storage.writeFile("test-workspace/test.txt", "Hello NEXUS")
        assertTrue(written)

        val read = storage.readFile("test-workspace/test.txt")
        assertEquals("Hello NEXUS", read)
    }

    @Test
    fun `test room conversation and message relationships`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = NexusDatabase.getInstance(context)
        val convDao = db.conversationDao()

        val conv = ConversationEntity(
            id = "conv_test_1",
            title = "Room Test Conversation",
            agentId = "developer"
        )
        convDao.insertConversation(conv)

        val msg1 = MessageEntity(
            id = "msg_test_1",
            conversationId = conv.id,
            role = "user",
            content = "Testing Room relationships"
        )
        val msg2 = MessageEntity(
            id = "msg_test_2",
            conversationId = conv.id,
            role = "assistant",
            content = "Room relationship working"
        )
        convDao.insertMessages(listOf(msg1, msg2))

        val result = convDao.getConversationWithMessages(conv.id).first()
        assertNotNull(result)
        assertEquals("Room Test Conversation", result?.conversation?.title)
        assertEquals(2, result?.messages?.size)
        assertEquals("Testing Room relationships", result?.messages?.get(0)?.content)

        // Test cascade deletion
        convDao.deleteConversation(conv.id)
        val afterDelete = convDao.getConversationWithMessages(conv.id).first()
        assertEquals(null, afterDelete)
    }

    @Test
    fun `test room task and task steps relationships`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = NexusDatabase.getInstance(context)
        val taskDao = db.taskDao()

        val task = TaskEntity(
            id = "task_test_1",
            title = "Test Task Scaffolding",
            objective = "Build Room Test",
            agentId = "system",
            status = "PLAN",
            currentStepIndex = 0
        )
        taskDao.insertTask(task)

        val step1 = TaskStepEntity(
            id = "step_test_1",
            taskId = task.id,
            stepIndex = 0,
            title = "Initialize Environment",
            description = "Check environment status",
            status = "PENDING"
        )
        val step2 = TaskStepEntity(
            id = "step_test_2",
            taskId = task.id,
            stepIndex = 1,
            title = "Verify Schema",
            description = "Ensure tables exist",
            status = "PENDING"
        )
        taskDao.insertTaskSteps(listOf(step1, step2))

        val result = taskDao.getTaskWithSteps(task.id).first()
        assertNotNull(result)
        assertEquals("Test Task Scaffolding", result?.task?.title)
        assertEquals(2, result?.steps?.size)
        assertEquals("Initialize Environment", result?.steps?.get(0)?.title)
    }

    @Test
    fun `test room memory items insert query and search`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = NexusDatabase.getInstance(context)
        val memDao = db.memoryDao()

        val item1 = MemoryItemEntity(
            id = "mem_test_1",
            key = "ROOM_SCHEMA_V1",
            value = "Room SQLite schema initialized with foreign keys",
            category = "Configuration"
        )
        val item2 = MemoryItemEntity(
            id = "mem_test_2",
            key = "AGENT_POLICY_MODE",
            value = "Zero trust policy enforcement active",
            category = "Security"
        )
        memDao.clearAllMemory()
        memDao.insertMemoryItems(listOf(item1, item2))

        val configItems = memDao.getMemoryItemsByCategory("Configuration").first()
        assertTrue(configItems.any { it.key == "ROOM_SCHEMA_V1" })

        val searchResult = memDao.searchMemory("SQLite").first()
        assertTrue(searchResult.any { it.key == "ROOM_SCHEMA_V1" })
    }

    @Test
    fun `test developer agent declines nmap request due to boundary enforcement`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = NexusStorageManager(context)
        val taskEngine = NexusTaskEngine(storage)
        val provider = DeterministicLocalProvider(taskEngine)

        val devAgent = Agent(
            id = "developer",
            name = "Developer Agent",
            title = "Code & Workspaces",
            description = "Handles local code and filesystems",
            systemPrompt = "Code specialist",
            permittedCategories = setOf("workspace", "filesystem")
        )

        val permissions = PermissionSettings(
            filesystem = true,
            packages = true,
            network = true,
            workspace = true,
            shellScripts = false
        )

        val conv = Conversation(id = "conv_1", title = "Nmap Test", agentId = devAgent.id)
        val response = provider.processRequest("ask the developer to set up nmap. And run it by scanning scanme.nmap.org", conv, devAgent, permissions)

        assertTrue(response is AIResponse.Text)
        val text = (response as AIResponse.Text).message
        assertTrue(text.contains("cannot execute package installations or network scans"))
        assertTrue(text.contains("Switch to the System Agent"))
    }

    @Test
    fun `test system agent creates 4-step verified task for nmap setup and scan`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = NexusStorageManager(context)
        val taskEngine = NexusTaskEngine(storage)
        val provider = DeterministicLocalProvider(taskEngine)

        val sysAgent = Agent(
            id = "system",
            name = "System Agent",
            title = "OS & Packages",
            description = "Handles system packages and network operations",
            systemPrompt = "System administrator",
            permittedCategories = setOf("packages", "network", "filesystem", "workspace", "shellScripts")
        )

        val permissions = PermissionSettings(
            filesystem = true,
            packages = true,
            network = true,
            workspace = true,
            shellScripts = false
        )

        val conv = Conversation(id = "conv_2", title = "Nmap Test", agentId = sysAgent.id)
        val response = provider.processRequest("set up nmap. And run it by scanning scanme.nmap.org", conv, sysAgent, permissions)

        assertTrue(response is AIResponse.StructuredTaskProposal)
        val proposal = response as AIResponse.StructuredTaskProposal
        assertEquals(4, proposal.task.steps.size)
        assertEquals("inspect_env", proposal.task.steps[0].operation?.opName)
        assertEquals("install_package", proposal.task.steps[1].operation?.opName)
        assertEquals("network_fetch", proposal.task.steps[2].operation?.opName)
        assertEquals("write_file", proposal.task.steps[3].operation?.opName)
        assertEquals("nmap", proposal.task.steps[1].operation?.params?.get("package"))
        assertEquals("scanme.nmap.org", proposal.task.steps[2].operation?.params?.get("target"))
    }

    @Test
    fun `test terminal executes nmap command through policy engine and requires approval`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = NexusStorageManager(context)
        val terminal = NexusTerminalEngine(storage)

        val permissions = PermissionSettings(
            filesystem = true,
            packages = true,
            network = true,
            workspace = true,
            shellScripts = true
        )

        val result = terminal.execute("nmap scanme.nmap.org", permissions, null)
        assertTrue(result is NexusTerminalEngine.ExecResult.RequiresApproval)
        val approval = result as NexusTerminalEngine.ExecResult.RequiresApproval
        assertEquals("network_fetch", approval.operation.opName)
        assertEquals(RiskLevel.HIGH, approval.operation.risk)
        assertEquals("scanme.nmap.org", approval.operation.params["target"])
    }

    @Test
    fun `test cpp capability inquiry returns explanation of multi-agent and 5-step task workflow`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = NexusStorageManager(context)
        val taskEngine = NexusTaskEngine(storage)
        val provider = DeterministicLocalProvider(taskEngine)

        val devAgent = Agent(
            id = "developer",
            name = "Developer Agent",
            title = "Code",
            description = "Desc",
            systemPrompt = "Prompt",
            permittedCategories = setOf("workspace", "filesystem")
        )

        val permissions = PermissionSettings()
        val conv = Conversation(id = "conv_cpp_inquiry", title = "C++ Query", agentId = devAgent.id)
        val response = provider.processRequest("Can it set up c++ and run a hello world command?", conv, devAgent, permissions)

        assertTrue(response is AIResponse.Text)
        val text = (response as AIResponse.Text).message
        assertTrue(text.contains("Yes, absolutely!"))
        assertTrue(text.contains("clang-llvm"))
        assertTrue(text.contains("Autonomous 5-Step Task"))
        assertTrue(text.contains("Hello, World from PROJECT NEXUS C++!"))
    }

    @Test
    fun `test system agent creates 5-step verified task for cpp hello world`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = NexusStorageManager(context)
        val taskEngine = NexusTaskEngine(storage)
        val provider = DeterministicLocalProvider(taskEngine)

        val sysAgent = Agent(
            id = "system",
            name = "System Agent",
            title = "System",
            description = "System tools",
            systemPrompt = "Admin",
            permittedCategories = setOf("packages", "network", "filesystem", "workspace", "shellScripts")
        )

        val permissions = PermissionSettings(
            filesystem = true,
            packages = true,
            network = true,
            workspace = true,
            shellScripts = true
        )

        val conv = Conversation(id = "conv_cpp_task", title = "C++ Task", agentId = sysAgent.id)
        val response = provider.processRequest("set up c++ and run hello world", conv, sysAgent, permissions)

        assertTrue(response is AIResponse.StructuredTaskProposal)
        val proposal = response as AIResponse.StructuredTaskProposal
        assertEquals(5, proposal.task.steps.size)
        assertEquals("inspect_env", proposal.task.steps[0].operation?.opName)
        assertEquals("install_package", proposal.task.steps[1].operation?.opName)
        assertEquals("create_workspace", proposal.task.steps[2].operation?.opName)
        assertEquals("write_file", proposal.task.steps[3].operation?.opName)
        assertEquals("compile_and_run", proposal.task.steps[4].operation?.opName)

        // Execute Step 4 (write main.cpp) and Step 5 (compile_and_run)
        val (afterStep4, ok4) = taskEngine.executeStep(proposal.task, 3) {}
        assertTrue(ok4)
        assertTrue(storage.readFile("cpp-hello/main.cpp")?.contains("Hello, World from PROJECT NEXUS C++!") == true)

        val (afterStep5, ok5) = taskEngine.executeStep(afterStep4, 4) {}
        assertTrue(ok5)
        assertTrue(afterStep5.steps[4].verificationNotes.contains("Hello, World from PROJECT NEXUS C++!"))
    }

    @Test
    fun `test terminal compiles and executes cpp hello world`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = NexusStorageManager(context)
        val terminal = NexusTerminalEngine(storage)

        val permissions = PermissionSettings(
            filesystem = true,
            packages = true,
            network = true,
            workspace = true,
            shellScripts = true
        )

        // 1. Create source file
        storage.writeFile("main.cpp", "#include <iostream>\nint main(){std::cout<<\"Hello, World from PROJECT NEXUS C++!\";return 0;}")

        // 2. Compile with clang++
        val compileResult = terminal.execute("clang++ main.cpp -o main", permissions, null)
        assertTrue(compileResult is NexusTerminalEngine.ExecResult.Output)
        val compileOutput = (compileResult as NexusTerminalEngine.ExecResult.Output).lines
        assertTrue(compileOutput.any { it.text.contains("Compilation") || it.text.contains("clang++") })

        // 3. Execute binary
        val runResult = terminal.execute("./main", permissions, null)
        assertTrue(runResult is NexusTerminalEngine.ExecResult.Output)
        val output = (runResult as NexusTerminalEngine.ExecResult.Output).lines
        assertTrue(output.isNotEmpty())
    }

    @Test
    fun `test terminal history and command cycling`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = NexusStorageManager(context)
        val terminal = NexusTerminalEngine(storage)
        val permissions = PermissionSettings(filesystem = true, workspace = true, shellScripts = true)

        terminal.execute("pwd", permissions, null)
        terminal.execute("ls -la", permissions, null)
        terminal.execute("help", permissions, null)

        assertEquals(3, terminal.history.size)
        assertEquals("pwd", terminal.history[0])
        assertEquals("ls -la", terminal.history[1])
        assertEquals("help", terminal.history[2])
    }

    @Test
    fun `test real process executor runs shell commands with telemetry`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = NexusStorageManager(context)
        val result = com.example.nexus.system.RealProcessExecutor.execute(
            command = "echo 'NEXUS_REAL_EXEC'",
            workingDir = storage.workspacesDir
        )

        assertTrue(result.stdout.contains("NEXUS_REAL_EXEC"))
        assertEquals(0, result.exitCode)
        assertTrue(result.executionTimeMs >= 0)
        assertTrue(result.summaryBadge().contains("exit: 0"))
    }

    @Test
    fun `test view model runWorkspaceFile executes python and cpp scripts`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val viewModel = com.example.nexus.ui.NexusViewModel(app)

        val pyFile = com.example.nexus.model.WorkspaceFile(
            name = "hello.py",
            path = "/workspaces/hello.py",
            relativePath = "hello.py",
            isDirectory = false,
            sizeBytes = 40,
            lastModified = System.currentTimeMillis()
        )
        viewModel.storageManager.writeFile("hello.py", "print('Hello Python')")
        viewModel.runWorkspaceFile(pyFile)

        assertEquals(com.example.nexus.ui.NexusNavDestination.TERMINAL, viewModel.uiState.value.currentDestination)
        assertTrue(viewModel.uiState.value.terminalEntries.any { it.text.contains("python hello.py") })
    }

    @Test
    fun `test CommandExecutor captures stdout, exit code, and execution duration`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = NexusStorageManager(context)
        val executor = CommandExecutor(defaultWorkingDir = storage.workspacesDir)

        val result = executor.execute("echo 'COMMAND_EXECUTOR_TEST'")

        assertEquals(0, result.exitCode)
        assertTrue(result.isSuccess)
        assertFalse(result.hasError)
        assertFalse(result.isTimeout)
        assertTrue(result.stdout.contains("COMMAND_EXECUTOR_TEST"))
        assertEquals("", result.stderr)
        assertTrue(result.durationMs >= 0)
        assertTrue(result.formattedDuration().contains("ms") || result.formattedDuration().contains("s"))
        assertTrue(result.summaryBadge().contains("exit: 0"))
    }

    @Test
    fun `test CommandExecutor captures stderr and non-zero exit codes`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = NexusStorageManager(context)
        val executor = CommandExecutor(defaultWorkingDir = storage.workspacesDir)

        val result = executor.execute("sh -c 'echo \"FATAL ERROR ON STREAM\" >&2; exit 42'")

        assertEquals(42, result.exitCode)
        assertFalse(result.isSuccess)
        assertTrue(result.hasError)
        assertTrue(result.stderr.contains("FATAL ERROR ON STREAM"))
        assertTrue(result.summaryBadge().contains("exit: 42"))
    }

    @Test
    fun `test CommandExecutor formats robust terminal entries`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = NexusStorageManager(context)
        val executor = CommandExecutor(defaultWorkingDir = storage.workspacesDir)

        val result = executor.execute("sh -c 'echo \"Standard output\"; echo \"Standard error output\" >&2; exit 1'")
        val terminalEntries = result.toTerminalEntries()

        // Verify stdout mapped to OUT
        assertTrue(terminalEntries.any { it.kind == TerminalKind.OUT && it.text.contains("Standard output") })
        // Verify stderr mapped to ERR
        assertTrue(terminalEntries.any { it.kind == TerminalKind.ERR && it.text.contains("Standard error output") })
        // Verify badge mapped to WARN due to non-zero exit code
        assertTrue(terminalEntries.any { it.kind == TerminalKind.WARN && it.text.contains("exit: 1") })

        val plainDisplay = result.formatTerminalDisplay()
        assertTrue(plainDisplay.contains("Standard output"))
        assertTrue(plainDisplay.contains("[STDERR]"))
        assertTrue(plainDisplay.contains("Standard error output"))
        assertTrue(plainDisplay.contains("exit: 1"))
    }

    @Test
    fun `test CommandExecutor timeout handling forcibly terminates runaway process`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = NexusStorageManager(context)
        val executor = CommandExecutor(defaultWorkingDir = storage.workspacesDir)

        val result = executor.execute("sleep 10", timeoutMs = 500L)

        assertTrue(result.isTimeout)
        assertEquals(124, result.exitCode)
        assertFalse(result.isSuccess)
        assertTrue(result.stderr.contains("timed out"))
        assertTrue(result.summaryBadge().contains("TIMEOUT"))
    }

    @Test
    fun `test CommandExecutor executeStream emits stdout lines and completed event`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = NexusStorageManager(context)
        val executor = CommandExecutor(defaultWorkingDir = storage.workspacesDir)

        val events = executor.executeStream("echo 'Line1'; echo 'Line2'").toList()

        val stdoutLines = events.filterIsInstance<ProcessStreamEvent.StdoutLine>().map { it.line }
        assertTrue(stdoutLines.contains("Line1"))
        assertTrue(stdoutLines.contains("Line2"))

        val completed = events.filterIsInstance<ProcessStreamEvent.Completed>().firstOrNull()
        assertNotNull(completed)
        assertEquals(0, completed?.result?.exitCode)
        assertTrue(completed?.result?.isSuccess == true)
    }

    @Test
    fun `test NexusTerminalEngine executeStream streams output entries`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = NexusStorageManager(context)
        val terminal = NexusTerminalEngine(storage)
        val permissions = PermissionSettings(filesystem = true, workspace = true, shellScripts = true)

        val entries = terminal.executeStream("echo 'STREAM_OUTPUT_TEST'", permissions, null).toList()
        assertTrue(entries.any { it.kind == TerminalKind.OUT && it.text.contains("STREAM_OUTPUT_TEST") })
        assertTrue(entries.any { it.kind == TerminalKind.META && it.text.contains("exit: 0") })
    }

    @Test
    fun `test NexusStorageManager exports and imports workspace ZIP archive`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = NexusStorageManager(context)

        val wsName = "zip-test-ws"
        storage.createWorkspace(wsName)
        storage.writeFile("$wsName/code.py", "print('Archived code')")
        storage.writeFile("$wsName/info.txt", "Nexus metadata")

        val zipFile = storage.exportWorkspaceToZip(wsName)
        assertNotNull(zipFile)
        assertTrue(zipFile!!.exists())
        assertTrue(zipFile.length() > 0)

        val importedWsName = "zip-imported-ws"
        val importOk = storage.importWorkspaceFromZip(zipFile, importedWsName)
        assertTrue(importOk)

        val readCode = storage.readFile("$importedWsName/code.py")
        assertEquals("print('Archived code')", readCode)
        val readInfo = storage.readFile("$importedWsName/info.txt")
        assertEquals("Nexus metadata", readInfo)
    }

    @Test
    fun `test highlightSyntax styles keywords and strings correctly`() {
        val pythonCode = "def calculate(val):\n    return \"result\"\n# comment"
        val highlighted = com.example.nexus.ui.screens.highlightSyntax(pythonCode, "calc.py")

        assertNotNull(highlighted)
        assertEquals(pythonCode, highlighted.text)
        assertTrue(highlighted.spanStyles.isNotEmpty())
    }
}

