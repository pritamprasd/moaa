package dev.pritam.host.tool.llmgateway.connector

import dev.pritam.host.tool.llmgateway.model.ChatCompletionRequest
import dev.pritam.host.tool.llmgateway.model.ChatCompletionResponse
import dev.pritam.host.tool.llmgateway.model.ChatStreamChunk
import dev.pritam.host.tool.llmgateway.model.LlmProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class GeminiCloudConnector : LlmConnector {

    override suspend fun executeChat(profile: LlmProfile, request: ChatCompletionRequest): ChatCompletionResponse = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val targetModel = request.model?.ifBlank { null } ?: profile.targetModel ?: "gemini-1.5-flash"
        val cleanModel = targetModel.removePrefix("models/")

        val apiKey = profile.apiKey?.trim()
        val token = profile.accessToken?.trim()

        val endpoint = if (!apiKey.isNullOrEmpty()) {
            "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent?key=$apiKey"
        } else {
            "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent"
        }

        val jsonBody = formatGeminiPayload(request)

        val url = URL(endpoint)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10000
            readTimeout = 60000
            doInput = true
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            if (!token.isNullOrEmpty() && apiKey.isNullOrEmpty()) {
                setRequestProperty("Authorization", "Bearer $token")
            }
        }

        try {
            OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                writer.write(jsonBody.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode !in 200..299) {
                val errorMsg = try {
                    conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
                } catch (e: Exception) {
                    "HTTP $responseCode"
                }
                if (responseCode == 429) {
                    throw RuntimeException("429 Too Many Requests (Gemini Quota Exceeded): $errorMsg")
                }
                throw RuntimeException("Gemini Cloud [${profile.name}] error $responseCode: $errorMsg")
            }

            val responseBody = conn.inputStream.bufferedReader().use { it.readText() }
            val latency = System.currentTimeMillis() - startTime

            val jsonResponse = JSONObject(responseBody)
            val content = extractGeminiText(jsonResponse)
            val toolCalls = extractGeminiToolCalls(jsonResponse)

            ChatCompletionResponse(
                id = "gemini-${UUID.randomUUID()}",
                model = cleanModel,
                content = content,
                profileUsed = profile.name,
                latencyMs = latency,
                toolCalls = toolCalls
            )
        } finally {
            conn.disconnect()
        }
    }

    override fun streamChat(profile: LlmProfile, request: ChatCompletionRequest): Flow<ChatStreamChunk> = flow {
        val targetModel = request.model?.ifBlank { null } ?: profile.targetModel ?: "gemini-1.5-flash"
        val cleanModel = targetModel.removePrefix("models/")
        val apiKey = profile.apiKey?.trim()
        val token = profile.accessToken?.trim()

        val endpoint = if (!apiKey.isNullOrEmpty()) {
            "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:streamGenerateContent?alt=sse&key=$apiKey"
        } else {
            "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:streamGenerateContent?alt=sse"
        }

        val jsonBody = formatGeminiPayload(request)

        val url = URL(endpoint)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10000
            readTimeout = 60000
            doInput = true
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            if (!token.isNullOrEmpty() && apiKey.isNullOrEmpty()) {
                setRequestProperty("Authorization", "Bearer $token")
            }
        }

        try {
            OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                writer.write(jsonBody.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode !in 200..299) {
                val errorMsg = try {
                    conn.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP $responseCode"
                } catch (e: Exception) {
                    "HTTP $responseCode"
                }
                if (responseCode == 429) {
                    throw RuntimeException("429 Too Many Requests (Gemini Quota Exceeded): $errorMsg")
                }
                throw RuntimeException("Gemini Cloud [${profile.name}] stream error $responseCode: $errorMsg")
            }

            BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val currentLine = line?.trim() ?: continue
                    if (currentLine.isEmpty() || currentLine.startsWith(":")) continue

                    val jsonStr = if (currentLine.startsWith("data: ")) {
                        currentLine.substring(6).trim()
                    } else if (currentLine.startsWith("{")) {
                        currentLine
                    } else continue

                    try {
                        val json = JSONObject(jsonStr)
                        val delta = extractGeminiText(json)
                        if (delta.isNotEmpty()) {
                            emit(
                                ChatStreamChunk(
                                    id = "chunk-${UUID.randomUUID()}",
                                    model = cleanModel,
                                    deltaContent = delta,
                                    finishReason = null,
                                    profileUsed = profile.name
                                )
                            )
                        }
                    } catch (e: Exception) {
                        // Skip malformed chunk
                    }
                }
            }

            emit(
                ChatStreamChunk(
                    id = "chunk-${UUID.randomUUID()}",
                    model = cleanModel,
                    deltaContent = "",
                    finishReason = "stop",
                    profileUsed = profile.name
                )
            )
        } finally {
            conn.disconnect()
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun ping(profile: LlmProfile): PingResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val apiKey = profile.apiKey?.trim()
        val token = profile.accessToken?.trim()

        if (apiKey.isNullOrEmpty() && token.isNullOrEmpty()) {
            return@withContext PingResult(
                isSuccess = false,
                latencyMs = 0L,
                errorMessage = "API key or OAuth token is not configured"
            )
        }

        val endpoint = if (!apiKey.isNullOrEmpty()) {
            "https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey"
        } else {
            "https://generativelanguage.googleapis.com/v1beta/models"
        }

        try {
            val url = URL(endpoint)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                if (!token.isNullOrEmpty() && apiKey.isNullOrEmpty()) {
                    setRequestProperty("Authorization", "Bearer $token")
                }
            }

            val code = conn.responseCode
            val latency = System.currentTimeMillis() - startTime
            if (code in 200..299) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()

                val models = mutableListOf<String>()
                try {
                    val json = JSONObject(body)
                    val list = json.getJSONArray("models")
                    for (i in 0 until list.length()) {
                        val obj = list.getJSONObject(i)
                        val name = obj.optString("name", "").removePrefix("models/")
                        if (name.contains("gemini")) models.add(name)
                    }
                } catch (e: Exception) {
                    models.addAll(listOf("gemini-1.5-flash", "gemini-1.5-pro", "gemini-2.0-flash-exp"))
                }

                return@withContext PingResult(
                    isSuccess = true,
                    latencyMs = latency,
                    models = if (models.isEmpty()) listOf("gemini-1.5-flash", "gemini-1.5-pro") else models
                )
            } else {
                conn.disconnect()
                return@withContext PingResult(
                    isSuccess = false,
                    latencyMs = latency,
                    errorMessage = "Gemini API returned HTTP $code"
                )
            }
        } catch (e: Exception) {
            val total = System.currentTimeMillis() - startTime
            PingResult(
                isSuccess = false,
                latencyMs = total,
                errorMessage = e.message ?: "Failed to connect to Google Gemini"
            )
        }
    }

    private fun formatGeminiPayload(request: ChatCompletionRequest): JSONObject {
        val root = JSONObject()
        val contentsArray = JSONArray()
        var systemInstruction: String? = null

        request.messages.forEach { msg ->
            if (msg.role.equals("system", ignoreCase = true)) {
                systemInstruction = msg.content
            } else {
                val geminiRole = if (msg.role.equals("assistant", ignoreCase = true)) "model" else "user"
                val contentObj = JSONObject().apply {
                    put("role", geminiRole)
                    val parts = JSONArray().apply {
                        put(JSONObject().apply { put("text", msg.content) })
                    }
                    put("parts", parts)
                }
                contentsArray.put(contentObj)
            }
        }

        if (contentsArray.length() == 0) {
            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply { put(JSONObject().apply { put("text", "Hello") }) })
            })
        }

        root.put("contents", contentsArray)

        if (!systemInstruction.isNullOrBlank()) {
            val sysObj = JSONObject().apply {
                val parts = JSONArray().apply {
                    put(JSONObject().apply { put("text", systemInstruction) })
                }
                put("parts", parts)
            }
            root.put("systemInstruction", sysObj)
        }

        if (request.tools.isNotEmpty()) {
            val fnDecls = JSONArray()
            request.tools.forEach { t ->
                val fnObj = JSONObject().apply {
                    put("name", t.name)
                    put("description", t.description)
                    put("parameters", t.inputSchema.toJson())
                }
                fnDecls.put(fnObj)
            }
            val toolsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("functionDeclarations", fnDecls)
                })
            }
            root.put("tools", toolsArray)
        }

        val genConfig = JSONObject()
        request.temperature?.let { genConfig.put("temperature", it) }
        request.maxTokens?.let { genConfig.put("maxOutputTokens", it) }
        if (genConfig.length() > 0) {
            root.put("generationConfig", genConfig)
        }

        return root
    }

    private fun extractGeminiText(json: JSONObject): String {
        if (!json.has("candidates")) return ""
        val candidates = json.getJSONArray("candidates")
        if (candidates.length() == 0) return ""
        val cand = candidates.getJSONObject(0)
        if (!cand.has("content")) return ""
        val content = cand.getJSONObject("content")
        if (!content.has("parts")) return ""
        val parts = content.getJSONArray("parts")
        val sb = StringBuilder()
        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)
            sb.append(part.optString("text", ""))
        }
        return sb.toString()
    }

    private fun extractGeminiToolCalls(json: JSONObject): List<dev.pritam.host.tool.llmgateway.mcp.model.McpToolCall> {
        val list = mutableListOf<dev.pritam.host.tool.llmgateway.mcp.model.McpToolCall>()
        if (!json.has("candidates")) return list
        val candidates = json.getJSONArray("candidates")
        if (candidates.length() == 0) return list
        val cand = candidates.getJSONObject(0)
        if (!cand.has("content")) return list
        val content = cand.getJSONObject("content")
        if (!content.has("parts")) return list
        val parts = content.getJSONArray("parts")
        for (i in 0 until parts.length()) {
            val part = parts.getJSONObject(i)
            if (part.has("functionCall")) {
                val fc = part.getJSONObject("functionCall")
                val fnName = fc.optString("name", "")
                val fnArgs = fc.optJSONObject("args")?.toString() ?: "{}"
                if (fnName.isNotBlank()) {
                    list.add(
                        dev.pritam.host.tool.llmgateway.mcp.model.McpToolCall(
                            name = fnName,
                            argumentsJson = fnArgs
                        )
                    )
                }
            }
        }
        return list
    }
}
