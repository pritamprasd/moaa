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

class ChatGptCloudConnector : LlmConnector {

    override suspend fun executeChat(profile: LlmProfile, request: ChatCompletionRequest): ChatCompletionResponse = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val targetModel = request.model?.ifBlank { null } ?: profile.targetModel ?: "gpt-4o-mini"
        val token = profile.apiKey?.trim() ?: profile.accessToken?.trim()
        if (token.isNullOrEmpty()) {
            throw IllegalArgumentException("API Key or token is not configured for ${profile.name}")
        }

        val endpoint = "https://api.openai.com/v1/chat/completions"

        val jsonBody = JSONObject().apply {
            put("model", targetModel)
            put("stream", false)
            request.temperature?.let { put("temperature", it) }
            request.maxTokens?.let { put("max_tokens", it) }

            val messagesArray = JSONArray()
            request.messages.forEach { msg ->
                val mObj = JSONObject().apply {
                    put("role", msg.role)
                    put("content", msg.content)
                    if (msg.toolCallId != null) {
                        put("tool_call_id", msg.toolCallId)
                    }
                }
                messagesArray.put(mObj)
            }
            put("messages", messagesArray)

            if (request.tools.isNotEmpty()) {
                val toolsArr = JSONArray()
                request.tools.forEach { t ->
                    toolsArr.put(t.toOpenAiToolJson())
                }
                put("tools", toolsArr)
            }
        }

        val url = URL(endpoint)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10000
            readTimeout = 60000
            doInput = true
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Authorization", "Bearer $token")
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
                    throw RuntimeException("429 Too Many Requests (OpenAI Quota Exceeded): $errorMsg")
                }
                throw RuntimeException("ChatGPT Cloud [${profile.name}] error $responseCode: $errorMsg")
            }

            val responseBody = conn.inputStream.bufferedReader().use { it.readText() }
            val latency = System.currentTimeMillis() - startTime

            val jsonResponse = JSONObject(responseBody)
            var content = ""
            val toolCallsList = mutableListOf<dev.motherofallapps.host.tool.llmgateway.mcp.model.McpToolCall>()

            if (jsonResponse.has("choices")) {
                val choices = jsonResponse.getJSONArray("choices")
                if (choices.length() > 0) {
                    val choice = choices.getJSONObject(0)
                    if (choice.has("message")) {
                        val msgObj = choice.getJSONObject("message")
                        content = msgObj.optString("content", "")
                        if (msgObj.has("tool_calls")) {
                            val tcArr = msgObj.getJSONArray("tool_calls")
                            for (i in 0 until tcArr.length()) {
                                val tc = tcArr.getJSONObject(i)
                                val fn = tc.optJSONObject("function") ?: JSONObject()
                                toolCallsList.add(
                                    dev.motherofallapps.host.tool.llmgateway.mcp.model.McpToolCall(
                                        id = tc.optString("id", UUID.randomUUID().toString()),
                                        name = fn.optString("name", ""),
                                        argumentsJson = fn.optString("arguments", "{}")
                                    )
                                )
                            }
                        }
                    }
                }
            }

            ChatCompletionResponse(
                id = jsonResponse.optString("id", "chatcmpl-${UUID.randomUUID()}"),
                model = jsonResponse.optString("model", targetModel),
                content = content,
                profileUsed = profile.name,
                latencyMs = latency,
                toolCalls = toolCallsList
            )
        } finally {
            conn.disconnect()
        }
    }

    override fun streamChat(profile: LlmProfile, request: ChatCompletionRequest): Flow<ChatStreamChunk> = flow {
        val targetModel = request.model?.ifBlank { null } ?: profile.targetModel ?: "gpt-4o-mini"
        val token = profile.apiKey?.trim() ?: profile.accessToken?.trim()
        if (token.isNullOrEmpty()) {
            throw IllegalArgumentException("API Key or token is not configured for ${profile.name}")
        }

        val endpoint = "https://api.openai.com/v1/chat/completions"

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
            connectTimeout = 10000
            readTimeout = 60000
            doInput = true
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Authorization", "Bearer $token")
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
                    throw RuntimeException("429 Too Many Requests (OpenAI Quota Exceeded): $errorMsg")
                }
                throw RuntimeException("ChatGPT Cloud [${profile.name}] stream error $responseCode: $errorMsg")
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
                                }
                                finishReason = if (choice.has("finish_reason") && !choice.isNull("finish_reason")) choice.optString("finish_reason").ifBlank { null } else null
                            }
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
        val token = profile.apiKey?.trim() ?: profile.accessToken?.trim()

        if (token.isNullOrEmpty()) {
            return@withContext PingResult(
                isSuccess = false,
                latencyMs = 0L,
                errorMessage = "API key or token is not configured"
            )
        }

        try {
            val url = URL("https://api.openai.com/v1/models")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Authorization", "Bearer $token")
            }

            val code = conn.responseCode
            val latency = System.currentTimeMillis() - startTime
            if (code in 200..299) {
                conn.disconnect()
                return@withContext PingResult(
                    isSuccess = true,
                    latencyMs = latency,
                    models = listOf("gpt-4o", "gpt-4o-mini", "o1-preview", "gpt-3.5-turbo")
                )
            } else {
                conn.disconnect()
                return@withContext PingResult(
                    isSuccess = false,
                    latencyMs = latency,
                    errorMessage = "OpenAI API returned HTTP $code"
                )
            }
        } catch (e: Exception) {
            val total = System.currentTimeMillis() - startTime
            PingResult(
                isSuccess = false,
                latencyMs = total,
                errorMessage = e.message ?: "Failed to connect to OpenAI"
            )
        }
    }
}
