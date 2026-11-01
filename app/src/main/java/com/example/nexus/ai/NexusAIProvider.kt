package com.example.nexus.ai

import com.example.nexus.model.Agent
import com.example.nexus.model.Conversation
import com.example.nexus.model.NexusTask
import com.example.nexus.model.Operation
import com.example.nexus.model.PermissionSettings
import com.example.nexus.model.RiskLevel
import com.example.nexus.security.NexusPolicyEngine
import com.example.nexus.task.NexusTaskEngine
import java.util.UUID

sealed class AIResponse {
    data class Text(val message: String) : AIResponse()
    data class ProposedOperation(val operation: Operation, val explanation: String) : AIResponse()
    data class StructuredTaskProposal(val task: NexusTask, val explanation: String) : AIResponse()
}

interface NexusAIProvider {
    val providerName: String
    val isOffline: Boolean
    suspend fun processRequest(
        prompt: String,
        conversation: Conversation,
        agent: Agent,
        permissions: PermissionSettings
    ): AIResponse
}

class DeterministicLocalProvider(
    private val taskEngine: NexusTaskEngine
) : NexusAIProvider {
    override val providerName: String = "Deterministic Local Engine"
    override val isOffline: Boolean = true

    override suspend fun processRequest(
        prompt: String,
        conversation: Conversation,
        agent: Agent,
        permissions: PermissionSettings
    ): AIResponse {
        val t = prompt.trim().lowercase()

        // 1. Operational Boundary & Task Check for nmap / network scanning
        if (t.contains("nmap") || t.contains("scanme") || (t.contains("scan") && (t.contains("port") || t.contains("network") || t.contains("host")))) {
            if (!agent.permittedCategories.contains("packages") || !agent.permittedCategories.contains("network")) {
                return AIResponse.Text(
                    """The ${agent.name} cannot execute package installations or network scans.
Its operational boundary is scoped strictly to: [${agent.permittedCategories.joinToString(", ")}].

Operational Boundary Enforcement:
• 'nmap' installation requires the 'packages' category.
• Port scanning 'scanme.nmap.org' requires the 'network' category.

Recommended Action:
Switch to the System Agent using the dropdown selector above. The System Agent possesses authority for 'packages' and 'network', and will formulate the verified 4-step installation and scan task."""
                )
            }

            val task = taskEngine.planTask(prompt, agent, permissions)
            return AIResponse.StructuredTaskProposal(
                task = task,
                explanation = "As the ${agent.name}, I've formulated a verified ${task.steps.size}-step execution plan to set up nmap and scan scanme.nmap.org. Review the proposed steps below; each action will undergo policy evaluation and require your explicit confirmation."
            )
        }

        // 2. C++ & Compilation Task / Capability Inquiry Check
        if (t.contains("c++") || t.contains("cpp") || t.contains("clang") || (t.contains("hello world") && (t.contains("c") || t.contains("compile") || t.contains("run")))) {
            val isQuestion = t.contains("can it") || t.contains("can you") || t.contains("is it possible") || t.contains("does it") || t.endsWith("?")
            if (isQuestion && !t.contains("please set up") && !t.contains("go ahead")) {
                return AIResponse.Text(
                    """Yes, absolutely! PROJECT NEXUS can set up C++ and run a Hello World program seamlessly.

### How NEXUS Executes C++:
1. **Toolchain Management**: NEXUS provides the `clang-llvm` package (v17.0.6 with `clang`, `clang++`, and `libc++`) in its managed package registry.
2. **Autonomous 5-Step Task**:
   • **Step 1 (Inspect)**: Verifies target architecture (aarch64/x86_64) and runtime environment.
   • **Step 2 (Install)**: Installs `clang-llvm` (requires user approval under 'packages' policy).
   • **Step 3 (Workspace)**: Creates isolated `cpp-hello` project directory.
   • **Step 4 (Scaffold)**: Writes standard C++ `main.cpp` using `<iostream>`.
   • **Step 5 (Compile & Run)**: Compiles with `clang++ -O2` and executes `./main`, verifying stdout `Hello, World from PROJECT NEXUS C++!`.
3. **Agent Role Boundaries**:
   • The **System Agent** has authority to install the `clang-llvm` package.
   • The **Developer Agent** specializes in writing `main.cpp` and managing workspaces.
4. **Interactive Terminal**:
   You can also enter `clang++ main.cpp -o main` and `./main` directly in the Terminal.

To start right away, reply **'set up c++ and run hello world'** with the System Agent active or switch to the Terminal tab!"""
                )
            }

            // Imperative task request
            if (!agent.permittedCategories.contains("packages")) {
                return AIResponse.Text(
                    """The ${agent.name} is scoped strictly to [${agent.permittedCategories.joinToString(", ")}].
Installing the `clang-llvm` compiler package requires the 'packages' category.

Recommended Action:
Switch to the System Agent to run the complete 5-step task (toolchain install + compile + run), or let me generate the C++ source in your workspace and use the Terminal."""
                )
            }

            val task = taskEngine.planTask(prompt, agent, permissions)
            return AIResponse.StructuredTaskProposal(
                task = task,
                explanation = "As the ${agent.name}, I've formulated a verified 5-step execution plan to set up the Clang C++ toolchain, scaffold main.cpp, and compile & execute the binary. Review each step below before approval."
            )
        }

        // 2. Task-oriented intents (multi-step lifecycle)
        if (t.contains("set up") || t.contains("setup") || t.contains("create project") ||
            t.contains("scaffold") || t.contains("scraper") || t.contains("environment")) {
            val task = taskEngine.planTask(prompt, agent, permissions)
            return AIResponse.StructuredTaskProposal(
                task = task,
                explanation = "I've analyzed your objective and prepared a verified ${task.steps.size}-step execution plan with ${agent.name}. Review the steps below; each mutating operation will be checked against the policy engine."
            )
        }

        // 2. Specific single operation intents
        if (t.contains("workspace") || t.contains("folder") || t.contains("mkdir")) {
            val name = extractName(prompt, "workspace", "default-workspace")
            val op = Operation(
                id = UUID.randomUUID().toString(),
                opName = "create_workspace",
                label = "Create workspace directory '$name'",
                category = "workspace",
                risk = RiskLevel.LOW,
                params = mapOf("name" to name),
                policyReason = "Workspace folder creation."
            )
            val eval = NexusPolicyEngine.evaluate(op.opName, permissions, agent)
            return AIResponse.ProposedOperation(
                operation = op.copy(policyReason = eval.reason),
                explanation = "I propose creating a new workspace directory named '$name'. I cannot execute this on my own — please review and approve below."
            )
        }

        if (t.contains("install") || t.contains("package") || t.contains("pip")) {
            val pkg = extractName(prompt, "install", "requests")
            val op = Operation(
                id = UUID.randomUUID().toString(),
                opName = "install_package",
                label = "Install tool package '$pkg'",
                category = "packages",
                risk = RiskLevel.MEDIUM,
                params = mapOf("package" to pkg),
                policyReason = "Installing new managed tool package."
            )
            val eval = NexusPolicyEngine.evaluate(op.opName, permissions, agent)
            return AIResponse.ProposedOperation(
                operation = op.copy(policyReason = eval.reason),
                explanation = "Installing package '$pkg' alters the managed tool environment. I have prepared the structured operation for your review."
            )
        }

        if (t.contains("write") || t.contains("create file") || t.contains("script")) {
            val path = extractName(prompt, "file", "script.py")
            val op = Operation(
                id = UUID.randomUUID().toString(),
                opName = "write_file",
                label = "Write file '$path'",
                category = "filesystem",
                risk = RiskLevel.MEDIUM,
                params = mapOf(
                    "path" to path,
                    "content" to "# Created by NEXUS ${agent.name}\nprint('Hello from NEXUS!')\n"
                ),
                policyReason = "Writing file to workspace filesystem."
            )
            val eval = NexusPolicyEngine.evaluate(op.opName, permissions, agent)
            return AIResponse.ProposedOperation(
                operation = op.copy(policyReason = eval.reason),
                explanation = "I have drafted the file write operation for '$path'. Nothing is written until you confirm."
            )
        }

        if (t.contains("delete") || t.contains("remove") || t.contains("rm")) {
            val path = extractName(prompt, "delete", "temp.txt")
            val op = Operation(
                id = UUID.randomUUID().toString(),
                opName = "delete_file",
                label = "Delete file '$path'",
                category = "filesystem",
                risk = RiskLevel.HIGH,
                params = mapOf("path" to path),
                policyReason = "High-risk permanent file deletion."
            )
            val eval = NexusPolicyEngine.evaluate(op.opName, permissions, agent)
            return AIResponse.ProposedOperation(
                operation = op.copy(policyReason = eval.reason),
                explanation = "Deleting files is a high-risk irreversible operation. The policy engine requires your explicit sign-off."
            )
        }

        if (t.contains("fetch") || t.contains("download") || t.contains("curl") || t.contains("http")) {
            val op = Operation(
                id = UUID.randomUUID().toString(),
                opName = "network_fetch",
                label = "Network request simulation",
                category = "network",
                risk = RiskLevel.HIGH,
                params = mapOf("url" to "https://api.github.com/zen"),
                policyReason = "Network operations require explicit network policy enablement."
            )
            val eval = NexusPolicyEngine.evaluate(op.opName, permissions, agent)
            return AIResponse.ProposedOperation(
                operation = op.copy(policyReason = eval.reason),
                explanation = "Network requests can send or receive data externally. Review this operation before permitting transmission."
            )
        }

        // 3. Conversational / Technical Explanations
        if (t.contains("who are you") || t.contains("what is nexus") || t.contains("help")) {
            return AIResponse.Text(
                """PROJECT NEXUS is an offline-first intelligent computing environment for Android.
Active Agent: ${agent.name} (${agent.title})
Operational Scope: ${agent.permittedCategories.joinToString(", ")}

Core Principles:
1. User defines the objective.
2. NEXUS plans, inspects environment, and requests permission.
3. Operations are executed through registered, audited tools.
4. Terminal remains fully functional independently.
5. All data is kept local in NEXUS/ directory."""
            )
        }

        if (t.contains("security") || t.contains("policy") || t.contains("permission") || t.contains("audit")) {
            return AIResponse.Text(
                """NEXUS Security Model:
• Centralized Operation Registry: Every command is categorized and risk-rated (LOW, MEDIUM, HIGH).
• Zero Unapproved Executions: The AI never runs raw arbitrary shell scripts.
• Active Permissions:
  - Filesystem: ${if (permissions.filesystem) "ON" else "OFF"}
  - Packages: ${if (permissions.packages) "ON" else "OFF"}
  - Network: ${if (permissions.network) "ON" else "OFF"}
  - Workspace: ${if (permissions.workspace) "ON" else "OFF"}
  - Shell Scripts: ${if (permissions.shellScripts) "ON" else "OFF"}
• Audit Trail: Every approve, deny, and block decision is recorded permanently in the Security tab."""
            )
        }

        if (t.contains("agent") || t.contains("persona")) {
            return AIResponse.Text(
                """Currently interacting with ${agent.name}.
Instructions: ${agent.description}

Available built-in agents:
• Developer: Code generation, workspace scaffolding, debugging.
• System: Packages, environment diagnostics, runtime setup.
• Security: Policy analysis, audit verification, vulnerability check.
• Data: File parsing, schema migration, persistent memory.
• Research: Technical documentation, algorithmic synthesis.

You can switch agents at any time from the selector above, or create custom agents in the Agents tab."""
            )
        }

        return AIResponse.Text(
            """Received your request: "$prompt"

As ${agent.name}, I'm operating in deterministic offline mode.
You can ask me to:
• "Set up a Python development environment"
• "Create a web scraper project workspace"
• "Create workspace my-project"
• "Install package sqlite-tools"
• "Explain security policies and audit logs"
Or execute commands directly in the Terminal tab!"""
        )
    }

    private fun extractName(prompt: String, keyword: String, fallback: String): String {
        val parts = prompt.split("\\s+".toRegex())
        val idx = parts.indexOfFirst { it.equals(keyword, ignoreCase = true) }
        return if (idx != -1 && idx + 1 < parts.size) {
            parts[idx + 1].replace(Regex("[^a-zA-Z0-9._-]"), "")
        } else fallback
    }
}
