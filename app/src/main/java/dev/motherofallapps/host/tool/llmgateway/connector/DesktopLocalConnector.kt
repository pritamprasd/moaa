package dev.motherofallapps.host.tool.llmgateway.connector

import dev.motherofallapps.host.tool.llmgateway.model.ChatCompletionRequest
import dev.motherofallapps.host.tool.llmgateway.model.ChatCompletionResponse
import dev.motherofallapps.host.tool.llmgateway.model.ChatStreamChunk
import dev.motherofallapps.host.tool.llmgateway.model.LlmProfile
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

class DesktopLocalConnector : LlmConnector {

    override suspend fun executeChat(profile: LlmProfile, request: ChatCompletionRequest): ChatCompletionResponse = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val baseUrl = profile.hostAddress?.trimEnd('/') ?: throw IllegalArgumentException("Host address is not configured for ${profile.name}")
        val endpoint = if (baseUrl.endsWith("/v1")) "$baseUrl/chat/completions" else "$baseUrl/v1/chat/completions"

        val targetModel = request.model?.ifBlank { null } ?: profile.targetModel ?: "llama3.2:latest"

        val jsonBody = JSONObject().apply {
            put("model", targetModel)
            put("stream", false)
            request.temperature?.let { put("temperature", it) }
            request.maxTokens?.let { put("max_tokens", it) }

            val messagesArray = JSONArray()
            request.messages.forEach { msg ->
                messagesArray.put(JSONObject().apply {
                    put("role", msg.role)
                    put("content", msg.content)
                })
            }
            put("messages", messagesArray)
        }

        val url = URL(endpoint)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 8000
            readTimeout = 60000
            doInput = true
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
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
                throw RuntimeException("Desktop Host [${profile.name}] returned error $responseCode: $errorMsg")
            }

            val responseBody = conn.inputStream.bufferedReader().use { it.readText() }
            val latency = System.currentTimeMillis() - startTime

            val jsonResponse = JSONObject(responseBody)
            val content = if (jsonResponse.has("choices")) {
                val choices = jsonResponse.getJSONArray("choices")
                if (choices.length() > 0) {
                    val choice = choices.getJSONObject(0)
                    if (choice.has("message")) {
                        choice.getJSONObject("message").optString("content", "")
                    } else if (choice.has("text")) {
                        choice.optString("text", "")
                    } else ""
                } else ""
            } else if (jsonResponse.has("message")) {
                jsonResponse.getJSONObject("message").optString("content", "")
            } else if (jsonResponse.has("response")) {
                jsonResponse.optString("response", "")
            } else {
                responseBody
            }

            val resModel = jsonResponse.optString("model", targetModel)

            ChatCompletionResponse(
                id = jsonResponse.optString("id", "chatcmpl-${UUID.randomUUID()}"),
                model = resModel,
                content = content,
                profileUsed = profile.name,
                latencyMs = latency
            )
        } finally {
            conn.disconnect()
        }
    }

    override fun streamChat(profile: LlmProfile, request: ChatCompletionRequest): Flow<ChatStreamChunk> = flow {
        val baseUrl = profile.hostAddress?.trimEnd('/') ?: throw IllegalArgumentException("Host address is not configured for ${profile.name}")
        val endpoint = if (baseUrl.endsWith("/v1")) "$baseUrl/chat/completions" else "$baseUrl/v1/chat/completions"
        val targetModel = request.model?.ifBlank { null } ?: profile.targetModel ?: "llama3.2:latest"

        val jsonBody = JSONObject().apply {
            put("model", targetModel)
            put("stream", true)
            request.temperature?.let { put("temperature", it) }
            request.maxTokens?.let { put("max_tokens", it) }

            val messagesArray = JSONArray()
            request.messages.forEach { msg ->
                messagesArray.put(JSONObject().apply {
                    put("role", msg.role)
                    put("content", msg.content)
                })
            }
            put("messages", messagesArray)
        }

        val url = URL(endpoint)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 8000
            readTimeout = 60000
            doInput = true
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
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
                throw RuntimeException("Desktop Host [${profile.name}] streaming error $responseCode: $errorMsg")
            }

            BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val currentLine = line?.trim() ?: continue
                    if (currentLine.isEmpty() || currentLine.startsWith(":")) continue
                    if (currentLine == "data: [DONE]" || currentLine == "[DONE]") {
                        emit(
                            ChatStreamChunk(
                                id = "chunk-${UUID.randomUUID()}",
                                model = targetModel,
                                deltaContent = "",
                                finishReason = "stop",
                                profileUsed = profile.name
                            )
                        )
                        break
                    }

                    val jsonStr = if (currentLine.startsWith("data: ")) {
                        currentLine.substring(6).trim()
                    } else if (currentLine.startsWith("{")) {
                        currentLine
                    } else continue

                    try {
                        val json = JSONObject(jsonStr)
                        var delta = ""
                        var finishReason: String? = null

                        if (json.has("choices")) {
                            val choices = json.getJSONArray("choices")
                            if (choices.length() > 0) {
                                val choice = choices.getJSONObject(0)
                                if (choice.has("delta")) {
                                    val deltaObj = choice.getJSONObject("delta")
                                    delta = deltaObj.optString("content", "")
                                } else if (choice.has("text")) {
                                    delta = choice.optString("text", "")
                                }
                                finishReason = if (choice.has("finish_reason") && !choice.isNull("finish_reason")) choice.optString("finish_reason").ifBlank { null } else null
                            }
                        } else if (json.has("message")) {
                            delta = json.getJSONObject("message").optString("content", "")
                            if (json.optBoolean("done", false)) finishReason = "stop"
                        } else if (json.has("response")) {
                            delta = json.optString("response", "")
                            if (json.optBoolean("done", false)) finishReason = "stop"
                        }

                        if (delta.isNotEmpty() || finishReason != null) {
                            emit(
                                ChatStreamChunk(
                                    id = json.optString("id", "chunk-${UUID.randomUUID()}"),
                                    model = json.optString("model", targetModel),
                                    deltaContent = delta,
                                    finishReason = finishReason,
                                    profileUsed = profile.name
                                )
                            )
                        }
                    } catch (e: Exception) {
                        // Skip malformed chunk
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun ping(profile: LlmProfile): PingResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val baseUrl = profile.hostAddress?.trimEnd('/') ?: return@withContext PingResult(
            isSuccess = false,
            latencyMs = 0L,
            errorMessage = "Host address not set"
        )

        // Try /v1/models or /api/tags
        val endpointsToTry = listOf(
            if (baseUrl.endsWith("/v1")) "$baseUrl/models" else "$baseUrl/v1/models",
            "$baseUrl/api/tags",
            baseUrl
        )

        for (endpoint in endpointsToTry) {
            try {
                val url = URL(endpoint)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 3000
                    readTimeout = 4000
                }

                val code = conn.responseCode
                val latency = System.currentTimeMillis() - startTime
                if (code in 200..299) {
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    conn.disconnect()

                    val models = parseModelsList(body)
                    return@withContext PingResult(
                        isSuccess = true,
                        latencyMs = latency,
                        models = models
                    )
                }
                conn.disconnect()
            } catch (e: Exception) {
                // Try next
            }
        }

        val totalTime = System.currentTimeMillis() - startTime
        PingResult(
            isSuccess = false,
            latencyMs = totalTime,
            errorMessage = "Failed to reach desktop endpoint at $baseUrl"
        )
    }

    private fun parseModelsList(responseBody: String): List<String> {
        val models = mutableListOf<String>()
        try {
            val json = JSONObject(responseBody)
            if (json.has("data")) {
                val data = json.getJSONArray("data")
                for (i in 0 until data.length()) {
                    val obj = data.getJSONObject(i)
                    val id = obj.optString("id", "")
                    if (id.isNotEmpty()) models.add(id)
                }
            } else if (json.has("models")) {
                val data = json.getJSONArray("models")
                for (i in 0 until data.length()) {
                    val obj = data.getJSONObject(i)
                    val name = obj.optString("name", obj.optString("model", ""))
                    if (name.isNotEmpty()) models.add(name)
                }
            }
        } catch (e: Exception) {
            // Ignored
        }
        return models
    }
}
