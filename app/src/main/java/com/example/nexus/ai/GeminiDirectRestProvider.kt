package com.example.nexus.ai

import com.example.BuildConfig
import com.example.nexus.model.Agent
import com.example.nexus.model.Conversation
import com.example.nexus.model.PermissionSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiDirectRestProvider(
    private val fallbackProvider: DeterministicLocalProvider
) : NexusAIProvider {
    override val providerName: String = "Google Gemini 2.5 (Online)"
    override val isOffline: Boolean = false

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val apiKey: String
        get() = try {
            BuildConfig.GEMINI_API_KEY.trim()
        } catch (_: Exception) {
            ""
        }

    val isKeyConfigured: Boolean
        get() = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"

    suspend fun testConnection(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        if (!isKeyConfigured) {
            return@withContext Pair(
                false,
                "API key not injected into BuildConfig.GEMINI_API_KEY. Ensure AI Studio server secrets are populated."
            )
        }

        val startTime = System.currentTimeMillis()
        try {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "Respond with exact word PONG")
                            })
                        })
                    })
                })
            }

            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(body).build()

            val response = httpClient.newCall(request).execute()
            val latency = System.currentTimeMillis() - startTime
            val responseBody = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                Pair(true, "Connected to Gemini 2.5 Flash (${latency}ms latency)")
            } else {
                val errorMsg = try {
                    val errJson = JSONObject(responseBody)
                    errJson.optJSONObject("error")?.optString("message") ?: "HTTP ${response.code}"
                } catch (_: Exception) {
                    "HTTP ${response.code}: $responseBody"
                }
                Pair(false, "Gemini Cloud returned error: $errorMsg (${latency}ms)")
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            Pair(false, "Network error reaching Gemini: ${e.message} (${latency}ms)")
        }
    }

    override suspend fun processRequest(
        prompt: String,
        conversation: Conversation,
        agent: Agent,
        permissions: PermissionSettings
    ): AIResponse = withContext(Dispatchers.IO) {
        val t = prompt.lowercase()
        // If user prompt asks for workspace setup or task execution, keep deterministic task planner
        if (t.contains("set up") || t.contains("setup") || t.contains("workspace") || t.contains("install")) {
            return@withContext fallbackProvider.processRequest(prompt, conversation, agent, permissions)
        }

        if (!isKeyConfigured) {
            return@withContext AIResponse.Text(
                "[Status: Offline Intelligence Mode - BuildConfig key not set]\n\n" +
                (fallbackProvider.processRequest(prompt, conversation, agent, permissions) as? AIResponse.Text)?.message.orEmpty()
            )
        }

        val startTime = System.currentTimeMillis()
        try {
            // Select model: gemini-2.5-flash
            val modelName = "gemini-2.5-flash"
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

            // Construct system instruction and prompt
            val systemInstruction = """You are ${agent.name} (${agent.title}) in PROJECT NEXUS - an offline-first technical Android terminal and agent environment.
${agent.systemPrompt}
Operating boundaries: Your permitted categories are [${agent.permittedCategories.joinToString(", ")}].
Style: Concise, technical, precise, developer-friendly. Avoid AI boilerplate."""

            val historyParts = conversation.messages.takeLast(6).map { msg ->
                val role = if (msg.role == "user") "user" else "model"
                JSONObject().apply {
                    put("role", role)
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", msg.content) })
                    })
                }
            }

            val requestJson = JSONObject().apply {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemInstruction) })
                    })
                })
                put("contents", JSONArray().apply {
                    historyParts.forEach { put(it) }
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        })
                    })
                })
            }

            val body = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(body).build()

            val response = httpClient.newCall(request).execute()
            val latency = System.currentTimeMillis() - startTime
            val responseBody = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                val json = JSONObject(responseBody)
                val candidates = json.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val contentObj = firstCandidate?.optJSONObject("content")
                val parts = contentObj?.optJSONArray("parts")
                val text = parts?.optJSONObject(0)?.optString("text")

                if (!text.isNullOrBlank()) {
                    AIResponse.Text(text.trim() + "\n\n[Online AI Telemetry: $modelName | ${latency}ms latency | Verified via HTTPS]")
                } else {
                    fallbackProvider.processRequest(prompt, conversation, agent, permissions)
                }
            } else {
                AIResponse.Text(
                    "[Gemini Cloud Response: HTTP ${response.code} (${latency}ms)]\n\nFalling back to Local Intelligence Engine:\n\n" +
                    (fallbackProvider.processRequest(prompt, conversation, agent, permissions) as? AIResponse.Text)?.message.orEmpty()
                )
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            AIResponse.Text(
                "[Network Notice: ${e.message ?: "Unable to reach Gemini Cloud"} (${latency}ms)]\n\nOffline Local Intelligence Response:\n\n" +
                (fallbackProvider.processRequest(prompt, conversation, agent, permissions) as? AIResponse.Text)?.message.orEmpty()
            )
        }
    }
}
