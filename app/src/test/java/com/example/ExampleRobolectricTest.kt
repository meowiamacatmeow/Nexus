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
import com.example.nexus.security.NexusPolicyEngine
import com.example.nexus.storage.NexusStorageManager
import com.example.nexus.task.NexusTaskEngine
import com.example.nexus.terminal.NexusTerminalEngine
import kotlinx.coroutines.flow.first
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
        assertEquals("PROJECT NEXUS", appName)
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
        memDao.insertMemoryItems(listOf(item1, item2))

        val configItems = memDao.getMemoryItemsByCategory("Configuration").first()
        assertEquals(1, configItems.size)
        assertEquals("ROOM_SCHEMA_V1", configItems[0].key)

        val searchResult = memDao.searchMemory("SQLite").first()
        assertEquals(1, searchResult.size)
        assertEquals("ROOM_SCHEMA_V1", searchResult[0].key)
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
    fun `test terminal executes nmap command through policy engine and requires approval`() {
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
    fun `test terminal compiles and executes cpp hello world`() {
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
        assertTrue(compileResult is NexusTerminalEngine.ExecResult.RequiresApproval)
        val approval = compileResult as NexusTerminalEngine.ExecResult.RequiresApproval
        assertEquals("compile_and_run", approval.operation.opName)

        // 3. Execute binary
        val runResult = terminal.execute("./main", permissions, null)
        assertTrue(runResult is NexusTerminalEngine.ExecResult.Output)
        val output = (runResult as NexusTerminalEngine.ExecResult.Output).lines
        assertTrue(output.any { it.text.contains("Hello, World from PROJECT NEXUS C++!") })
    }
}
