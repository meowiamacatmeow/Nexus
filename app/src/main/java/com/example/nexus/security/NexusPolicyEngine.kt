package com.example.nexus.security

import com.example.nexus.model.Agent
import com.example.nexus.model.Operation
import com.example.nexus.model.PermissionSettings
import com.example.nexus.model.RiskLevel

data class OperationSpec(
    val opName: String,
    val label: String,
    val category: String,
    val risk: RiskLevel,
    val isReadOnly: Boolean = false
)

data class PolicyEvaluation(
    val allowed: Boolean,
    val risk: RiskLevel,
    val reason: String,
    val requiresConfirmation: Boolean,
    val category: String,
    val label: String,
    val suggestedAgent: String? = null
)

object NexusPolicyEngine {
    val REGISTRY: Map<String, OperationSpec> = mapOf(
        "create_workspace" to OperationSpec("create_workspace", "Create workspace folder", "workspace", RiskLevel.LOW),
        "write_file" to OperationSpec("write_file", "Write file in workspace", "filesystem", RiskLevel.MEDIUM),
        "read_file" to OperationSpec("read_file", "Read workspace file", "filesystem", RiskLevel.LOW, isReadOnly = true),
        "delete_file" to OperationSpec("delete_file", "Delete file or directory", "filesystem", RiskLevel.HIGH),
        "install_package" to OperationSpec("install_package", "Install tool/package", "packages", RiskLevel.MEDIUM),
        "uninstall_package" to OperationSpec("uninstall_package", "Remove tool/package", "packages", RiskLevel.MEDIUM),
        "run_script" to OperationSpec("run_script", "Execute shell script", "shellScripts", RiskLevel.HIGH),
        "network_fetch" to OperationSpec("network_fetch", "Send network request", "network", RiskLevel.HIGH),
        "inspect_env" to OperationSpec("inspect_env", "Inspect environment telemetry", "workspace", RiskLevel.LOW, isReadOnly = true),
        "compile_and_run" to OperationSpec("compile_and_run", "Compile and execute binary", "filesystem", RiskLevel.MEDIUM),
        "clear_memory" to OperationSpec("clear_memory", "Clear agent memory context", "workspace", RiskLevel.MEDIUM)
    )

    fun getSpec(opName: String): OperationSpec? = REGISTRY[opName]

    fun evaluate(
        opName: String,
        permissions: PermissionSettings,
        agent: Agent?
    ): PolicyEvaluation {
        val spec = REGISTRY[opName]
            ?: return PolicyEvaluation(
                allowed = false,
                risk = RiskLevel.HIGH,
                reason = "Operation '$opName' is not in the trusted NEXUS operation registry.",
                requiresConfirmation = true,
                category = "unknown",
                label = opName
            )

        // 1. Agent operational boundary check
        if (agent != null && !agent.permittedCategories.contains(spec.category)) {
            val suggested = findBestAgentFor(spec.category)
            return PolicyEvaluation(
                allowed = false,
                risk = spec.risk,
                reason = "${agent.name} is scoped to ${agent.permittedCategories.joinToString(", ")}. Operation requires '${spec.category}'.",
                requiresConfirmation = true,
                category = spec.category,
                label = spec.label,
                suggestedAgent = suggested
            )
        }

        // 2. Permission Category check
        val isPermitted = when (spec.category) {
            "filesystem" -> permissions.filesystem
            "packages" -> permissions.packages
            "network" -> permissions.network
            "workspace" -> permissions.workspace
            "shellScripts" -> permissions.shellScripts
            else -> false
        }

        if (!isPermitted) {
            return PolicyEvaluation(
                allowed = false,
                risk = spec.risk,
                reason = "Permission category '${spec.category}' is currently disabled in Security Settings.",
                requiresConfirmation = true,
                category = spec.category,
                label = spec.label
            )
        }

        // Read-only safe operations do not strictly require confirmation if policy category is enabled
        val needsConfirm = !spec.isReadOnly || spec.risk != RiskLevel.LOW

        return PolicyEvaluation(
            allowed = true,
            risk = spec.risk,
            reason = if (needsConfirm) "Within policy and agent scope. Awaiting explicit user confirmation." else "Approved (Read-only trusted query).",
            requiresConfirmation = needsConfirm,
            category = spec.category,
            label = spec.label
        )
    }

    private fun findBestAgentFor(category: String): String? {
        return when (category) {
            "workspace", "filesystem" -> "Developer Agent"
            "packages", "network", "shellScripts" -> "System Agent"
            "memory" -> "Data Agent"
            else -> "System Agent"
        }
    }
}
