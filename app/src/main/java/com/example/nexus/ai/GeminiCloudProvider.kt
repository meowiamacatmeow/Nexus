package com.example.nexus.ai

import com.example.nexus.model.Agent
import com.example.nexus.model.Conversation
import com.example.nexus.model.PermissionSettings
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiCloudProvider(
    private val fallbackProvider: DeterministicLocalProvider
) : NexusAIProvider {
    override val providerName: String = "Google Gemini (Cloud)"
    override val isOffline: Boolean = false

    private val generalModel by lazy {
        try {
            Firebase.ai.generativeModel("gemini-3.8-flash")
        } catch (e: Exception) {
            null
        }
    }

    private val complexModel by lazy {
        try {
            Firebase.ai.generativeModel("gemini-4-argon")
        } catch (e: Exception) {
            null
        }
    }

    private val fastModel by lazy {
        try {
            Firebase.ai.generativeModel("gemini-3.1-flash-lite")
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun processRequest(
        prompt: String,
        conversation: Conversation,
        agent: Agent,
        permissions: PermissionSettings
    ): AIResponse = withContext(Dispatchers.IO) {
        val t = prompt.lowercase()
        // If user prompt asks for structured task or workspace mutations, keep deterministic safety
        if (t.contains("set up") || t.contains("setup") || t.contains("workspace") || t.contains("install")) {
            return@withContext fallbackProvider.processRequest(prompt, conversation, agent, permissions)
        }

        // Model selection: gemini-4-argon for complex tasks, gemini-3.1-flash-lite for fast tasks, gemini-3.8-flash for general
        val isComplex = t.contains("audit") || t.contains("security") || t.contains("vulnerability") ||
                t.contains("architecture") || t.contains("algorithm") || t.contains("complex") || t.contains("crypt")
        val isFast = t.contains("quick") || t.contains("syntax") || t.contains("what is") || t.contains("meaning") || t.length < 30

        val targetModel = try {
            when {
                isComplex -> complexModel ?: generalModel
                isFast -> fastModel ?: generalModel
                else -> generalModel
            }
        } catch (e: Exception) {
            null
        }

        if (targetModel == null) {
            return@withContext fallbackProvider.processRequest(prompt, conversation, agent, permissions)
        }

        try {
            // Role-specific system instructions
            val systemInstructions = """Role: ${agent.name} (${agent.title}) in PROJECT NEXUS Android Environment.
${agent.systemPrompt}
Operating boundaries: Permitted categories are [${agent.permittedCategories.joinToString(", ")}].
Style: Concise, technical, precise, developer-friendly. Avoid AI boilerplate."""

            // Maintain conversation history context
            val historyContext = conversation.messages.takeLast(6).joinToString("\n") { msg ->
                val sender = if (msg.role == "user") "User" else agent.name
                "$sender: ${msg.content}"
            }

            val promptWithContext = """System Instruction:
$systemInstructions

Recent Conversation History:
$historyContext

Current User Input:
$prompt"""

            val response = targetModel.generateContent(promptWithContext)
            val text = response.text?.trim()
            if (!text.isNullOrBlank()) {
                AIResponse.Text(text)
            } else {
                fallbackProvider.processRequest(prompt, conversation, agent, permissions)
            }
        } catch (e: Exception) {
            AIResponse.Text("Cloud provider notice: ${e.localizedMessage ?: "Network or attestation unavailable"}.\n\nOffline Local Intelligence Response:\n\n" +
                (fallbackProvider.processRequest(prompt, conversation, agent, permissions) as? AIResponse.Text)?.message.orEmpty())
        }
    }
}
