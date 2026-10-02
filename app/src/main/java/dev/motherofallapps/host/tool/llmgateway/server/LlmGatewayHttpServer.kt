package dev.motherofallapps.host.tool.llmgateway.server

import dev.motherofallapps.host.logging.AppLogHub
import dev.motherofallapps.host.logging.LogLevel
import dev.motherofallapps.host.tool.llmgateway.engine.LlmRouterEngine
import dev.motherofallapps.host.tool.llmgateway.mcp.model.McpServerProfile
import dev.motherofallapps.host.tool.llmgateway.mcp.model.McpToolCall
import dev.motherofallapps.host.tool.llmgateway.mcp.model.McpTransportType
import dev.motherofallapps.host.tool.llmgateway.mcp.storage.McpServerRepository
import dev.motherofallapps.host.tool.llmgateway.model.ChatCompletionRequest
import dev.motherofallapps.host.tool.llmgateway.model.ChatMessage
import dev.motherofallapps.host.tool.llmgateway.model.RouterResult
import dev.motherofallapps.host.tool.llmgateway.storage.LlmProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID

data class GatewayServerTelemetry(
    val isRunning: Boolean = false,
    val port: Int = 8080,
    val hostAddress: String = "127.0.0.1:8080",
    val totalRequestsRouted: Long = 0L,
    val failoversTriggered: Long = 0L,
    val activeConnections: Int = 0,
    val startedAt: Long = 0L
)

class LlmGatewayHttpServer(
    private val repository: LlmProfileRepository,
    private val routerEngine: LlmRouterEngine,
    private val mcpRepository: McpServerRepository? = null,
    val port: Int = 8080
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverJob: Job? = null
    private var serverSocket: ServerSocket? = null

    private val _telemetry = MutableStateFlow(GatewayServerTelemetry(port = port))
    val telemetry: StateFlow<GatewayServerTelemetry> = _telemetry.asStateFlow()

    fun start() {
        if (_telemetry.value.isRunning) return

        serverJob = scope.launch {
            try {
                val socket = ServerSocket(port, 50, InetAddress.getByName("127.0.0.1"))
                serverSocket = socket

                _telemetry.value = _telemetry.value.copy(
                    isRunning = true,
                    startedAt = System.currentTimeMillis()
                )

                AppLogHub.log(
                    toolId = "llm-gateway",
                    toolName = "LLM Gateway",
                    level = LogLevel.INFO,
                    tag = "Server",
                    message = "LLM GATEWAY SERVER [START] Loopback HTTP IPC Server listening on http://127.0.0.1:$port"
                )

                while (isActive && !socket.isClosed) {
                    try {
                        val clientSocket = socket.accept()
                        launch {
                            handleClientConnection(clientSocket)
                        }
                    } catch (e: Exception) {
                        if (!socket.isClosed) {
                            // Ignored
                        }
                    }
                }
            } catch (e: Exception) {
                AppLogHub.log(
                    toolId = "llm-gateway",
                    toolName = "LLM Gateway",
                    level = LogLevel.ERROR,
                    tag = "Server",
                    message = "LLM GATEWAY SERVER [FATAL] Server binding failed on port $port: ${e.message}"
                )
                stop()
            }
        }
    }

    fun stop() {
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            // Ignored
        }
        serverSocket = null
        serverJob?.cancel()
        serverJob = null

        _telemetry.value = _telemetry.value.copy(
            isRunning = false
        )

        AppLogHub.log(
            toolId = "llm-gateway",
            toolName = "LLM Gateway",
            level = LogLevel.INFO,
            tag = "Server",
            message = "LLM GATEWAY SERVER [STOP] Loopback HTTP IPC Server stopped."
        )
    }

    private suspend fun handleClientConnection(socket: Socket) {
        try {
            socket.soTimeout = 60000
            val input = BufferedReader(InputStreamReader(socket.getInputStream(), "UTF-8"))
            val output = socket.getOutputStream()

            val requestLine = input.readLine() ?: return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return

            val method = parts[0].uppercase()
            val uri = parts[1]

            // Read headers
            var contentLength = 0
            var line: String?
            while (input.readLine().also { line = it } != null) {
                val h = line?.trim() ?: break
                if (h.isEmpty()) break
                if (h.startsWith("Content-Length:", ignoreCase = true)) {
                    contentLength = h.substring(15).trim().toIntOrNull() ?: 0
                }
            }

            // Handle CORS preflight
            if (method == "OPTIONS") {
                sendCorsResponse(output)
                return
            }

            when {
                uri.startsWith("/v1/chat/completions") && method == "POST" -> {
                    val body = readRequestBody(input, contentLength)
                    handleChatCompletions(body, output)
                }

                uri.startsWith("/v1/gateway/status") && method == "GET" -> {
                    handleGatewayStatus(output)
                }

                uri.startsWith("/v1/gateway/profiles") && method == "GET" -> {
                    handleGatewayProfiles(output)
                }

                uri.startsWith("/v1/models") && method == "GET" -> {
                    handleGetModels(output)
                }

                uri.startsWith("/v1/mcp/servers") && method == "GET" -> {
                    handleGetMcpServers(output)
                }

                uri.startsWith("/v1/mcp/servers") && method == "POST" -> {
                    val body = readRequestBody(input, contentLength)
                    handleAddMcpServer(body, output)
                }

                uri.startsWith("/v1/mcp/tools") && method == "GET" -> {
                    handleGetMcpTools(output)
                }

                uri.startsWith("/v1/mcp/tools/call") && method == "POST" -> {
                    val body = readRequestBody(input, contentLength)
                    handleCallMcpTool(body, output)
                }

                else -> {
                    sendJsonResponse(output, 404, JSONObject().apply {
                        put("error", "Endpoint not found: $method $uri")
                    }.toString())
                }
            }
        } catch (e: Exception) {
            // Client disconnect or socket error
        } finally {
            try {
                socket.close()
            } catch (e: Exception) {
                // Ignored
            }
        }
    }

    private fun readRequestBody(input: BufferedReader, contentLength: Int): String {
        if (contentLength <= 0) return ""
        val bodyChars = CharArray(contentLength)
        var read = 0
        while (read < contentLength) {
            val r = input.read(bodyChars, read, contentLength - read)
            if (r == -1) break
            read += r
        }
        return String(bodyChars, 0, read)
    }

    private suspend fun handleChatCompletions(body: String, output: OutputStream) {
        val request = parseChatRequest(body)

        _telemetry.value = _telemetry.value.copy(
            totalRequestsRouted = _telemetry.value.totalRequestsRouted + 1
        )

        if (request.stream) {
            // Stream response using SSE
            val header = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/event-stream; charset=utf-8\r\n" +
                    "Cache-Control: no-cache\r\n" +
                    "Connection: keep-alive\r\n" +
                    "Access-Control-Allow-Origin: *\r\n\r\n"
            output.write(header.toByteArray(Charsets.UTF_8))
            output.flush()

            var failoverLogged = false
            routerEngine.routeChatStream(request).collect { result ->
                when (result) {
                    is RouterResult.Success -> {
                        val chunk = result.data
                        val sseData = formatSseChunk(chunk)
                        output.write("data: $sseData\n\n".toByteArray(Charsets.UTF_8))
                        output.flush()
                    }
                    is RouterResult.FallbackSuccess -> {
                        if (!failoverLogged) {
                            failoverLogged = true
                            _telemetry.value = _telemetry.value.copy(
                                failoversTriggered = _telemetry.value.failoversTriggered + 1
                            )
                        }
                        val chunk = result.data
                        val sseData = formatSseChunk(chunk)
                        output.write("data: $sseData\n\n".toByteArray(Charsets.UTF_8))
                        output.flush()
                    }
                    is RouterResult.AllTargetsExhausted -> {
                        val errObj = JSONObject().apply {
                            put("error", JSONObject().apply {
                                put("message", "All LLM Gateway targets exhausted")
                                put("details", JSONObject(result.errors))
                            })
                        }
                        output.write("data: $errObj\n\n".toByteArray(Charsets.UTF_8))
                        output.flush()
                    }
                }
            }
            output.write("data: [DONE]\n\n".toByteArray(Charsets.UTF_8))
            output.flush()
        } else {
            // Synchronous response
            val result = routerEngine.routeChat(request)
            when (result) {
                is RouterResult.Success -> {
                    val resp = result.data
                    val json = formatOpenAiResponse(resp, emptyList())
                    sendJsonResponse(output, 200, json.toString())
                }
                is RouterResult.FallbackSuccess -> {
                    _telemetry.value = _telemetry.value.copy(
                        failoversTriggered = _telemetry.value.failoversTriggered + 1
                    )
                    val resp = result.data
                    val json = formatOpenAiResponse(resp, result.attemptedProfiles)
                    sendJsonResponse(output, 200, json.toString())
                }
                is RouterResult.AllTargetsExhausted -> {
                    val json = JSONObject().apply {
                        put("error", JSONObject().apply {
                            put("message", "All configured LLM Gateway targets failed or exhausted")
                            put("errors", JSONObject(result.errors))
                        })
                    }
                    sendJsonResponse(output, 502, json.toString())
                }
            }
        }
    }

    private fun handleGatewayStatus(output: OutputStream) {
        val t = _telemetry.value
        val profiles = repository.profiles.value
        val json = JSONObject().apply {
            put("status", if (t.isRunning) "RUNNING" else "STOPPED")
            put("port", t.port)
            put("endpoint", "http://${t.hostAddress}")
            put("uptime_seconds", if (t.isRunning) (System.currentTimeMillis() - t.startedAt) / 1000 else 0)
            put("total_requests_routed", t.totalRequestsRouted)
            put("failovers_triggered", t.failoversTriggered)
            put("active_profiles_count", profiles.count { it.isEnabled })
            put("total_profiles_count", profiles.size)
            put("active_mcp_tools_count", mcpRepository?.getAllActiveTools()?.size ?: 0)
        }
        sendJsonResponse(output, 200, json.toString())
    }

    private fun handleGatewayProfiles(output: OutputStream) {
        val profiles = repository.profiles.value
        val array = JSONArray()
        profiles.forEach { p ->
            array.put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("category", p.category.name)
                put("provider_type", p.providerType)
                put("target_model", p.targetModel ?: "")
                put("host_address", p.hostAddress ?: "")
                put("status", p.status.name)
                put("latency_ms", p.latencyMs)
                put("priority_order", p.priorityOrder)
                put("is_enabled", p.isEnabled)
            })
        }
        val json = JSONObject().apply {
            put("profiles", array)
        }
        sendJsonResponse(output, 200, json.toString())
    }

    private fun handleGetModels(output: OutputStream) {
        val profiles = repository.profiles.value.filter { it.isEnabled }
        val modelsList = mutableSetOf<String>()
        profiles.forEach { p ->
            p.targetModel?.let { modelsList.add(it) }
        }
        if (modelsList.isEmpty()) {
            modelsList.addAll(listOf("llama3.2:latest", "gemini-1.5-flash", "gpt-4o-mini"))
        }

        val dataArray = JSONArray()
        modelsList.forEach { m ->
            dataArray.put(JSONObject().apply {
                put("id", m)
                put("object", "model")
                put("created", System.currentTimeMillis() / 1000)
                put("owned_by", "llm-gateway")
            })
        }

        val json = JSONObject().apply {
            put("object", "list")
            put("data", dataArray)
        }
        sendJsonResponse(output, 200, json.toString())
    }

    private fun handleGetMcpServers(output: OutputStream) {
        val repo = mcpRepository
        val arr = JSONArray()
        if (repo != null) {
            repo.servers.value.forEach { s ->
                arr.put(JSONObject().apply {
                    put("id", s.id)
                    put("name", s.name)
                    put("transport_type", s.transportType.name)
                    put("endpoint_url", s.endpointUrl)
                    put("is_enabled", s.isEnabled)
                    put("status", s.status.name)
                    put("latency_ms", s.latencyMs)
                    put("tools_count", s.discoveredTools.size)
                    put("last_synced_at", s.lastSyncedAt)
                })
            }
        }
        val json = JSONObject().apply {
            put("servers", arr)
        }
        sendJsonResponse(output, 200, json.toString())
    }

    private fun handleAddMcpServer(body: String, output: OutputStream) {
        val repo = mcpRepository ?: run {
            sendJsonResponse(output, 500, JSONObject().put("error", "MCP repository not available").toString())
            return
        }

        try {
            val json = JSONObject(body)
            val name = json.optString("name", "New MCP Server")
            val transportStr = json.optString("transport_type", "HTTP_JSONRPC")
            val transport = try { McpTransportType.valueOf(transportStr) } catch (e: Exception) { McpTransportType.HTTP_JSONRPC }
            val endpoint = json.optString("endpoint_url", "")

            val server = McpServerProfile(
                id = UUID.randomUUID().toString(),
                name = name,
                transportType = transport,
                endpointUrl = endpoint,
                isEnabled = true
            )
            repo.addServer(server)

            sendJsonResponse(output, 201, JSONObject().apply {
                put("status", "CREATED")
                put("id", server.id)
                put("name", server.name)
            }.toString())
        } catch (e: Exception) {
            sendJsonResponse(output, 400, JSONObject().put("error", "Invalid JSON: ${e.message}").toString())
        }
    }

    private fun handleGetMcpTools(output: OutputStream) {
        val repo = mcpRepository
        val arr = JSONArray()
        if (repo != null) {
            val tools = repo.getAllActiveTools()
            tools.forEach { t ->
                arr.put(JSONObject().apply {
                    put("name", t.name)
                    put("description", t.description)
                    put("server_profile_id", t.serverProfileId)
                    put("parameters", t.inputSchema.toJson())
                })
            }
        }
        val json = JSONObject().apply {
            put("tools", arr)
        }
        sendJsonResponse(output, 200, json.toString())
    }

    private suspend fun handleCallMcpTool(body: String, output: OutputStream) {
        val repo = mcpRepository ?: run {
            sendJsonResponse(output, 500, JSONObject().put("error", "MCP repository not available").toString())
            return
        }

        try {
            val json = JSONObject(body)
            val name = json.optString("name", "")
            val argsObj = json.optJSONObject("arguments") ?: JSONObject()

            if (name.isBlank()) {
                sendJsonResponse(output, 400, JSONObject().put("error", "Missing tool 'name' parameter").toString())
                return
            }

            val call = McpToolCall(
                id = UUID.randomUUID().toString(),
                name = name,
                argumentsJson = argsObj.toString()
            )
            val result = repo.executeToolCall(call)

            sendJsonResponse(output, 200, JSONObject().apply {
                put("tool", result.toolName)
                put("call_id", result.callId)
                put("is_error", result.isError)
                put("content", result.content)
                put("latency_ms", result.latencyMs)
            }.toString())
        } catch (e: Exception) {
            sendJsonResponse(output, 400, JSONObject().put("error", "Tool execution error: ${e.message}").toString())
        }
    }

    private fun parseChatRequest(body: String): ChatCompletionRequest {
        if (body.isBlank()) return ChatCompletionRequest()
        return try {
            val json = JSONObject(body)
            val model = if (json.has("model") && !json.isNull("model")) json.optString("model").ifBlank { null } else null
            val temp = if (json.has("temperature")) json.optDouble("temperature", 0.7) else null
            val maxTokens = if (json.has("max_tokens")) json.optInt("max_tokens") else null
            val stream = json.optBoolean("stream", false)
            val autoExecute = json.optBoolean("auto_execute_tools", true)

            val messages = mutableListOf<ChatMessage>()
            if (json.has("messages")) {
                val array = json.getJSONArray("messages")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    messages.add(
                        ChatMessage(
                            role = obj.optString("role", "user"),
                            content = obj.optString("content", "")
                        )
                    )
                }
            }

            ChatCompletionRequest(
                model = model,
                messages = messages,
                temperature = temp,
                maxTokens = maxTokens,
                stream = stream,
                autoExecuteTools = autoExecute
            )
        } catch (e: Exception) {
            ChatCompletionRequest()
        }
    }

    private fun formatOpenAiResponse(resp: dev.motherofallapps.host.tool.llmgateway.model.ChatCompletionResponse, fallbacks: List<String>): JSONObject {
        return JSONObject().apply {
            put("id", resp.id)
            put("object", "chat.completion")
            put("created", System.currentTimeMillis() / 1000)
            put("model", resp.model)
            val choices = JSONArray().apply {
                put(JSONObject().apply {
                    put("index", 0)
                    put("message", JSONObject().apply {
                        put("role", "assistant")
                        put("content", resp.content)
                        if (resp.toolCalls.isNotEmpty()) {
                            val tcArr = JSONArray()
                            resp.toolCalls.forEach { tc ->
                                tcArr.put(JSONObject().apply {
                                    put("id", tc.id)
                                    put("type", "function")
                                    put("function", JSONObject().apply {
                                        put("name", tc.name)
                                        put("arguments", tc.argumentsJson)
                                    })
                                })
                            }
                            put("tool_calls", tcArr)
                        }
                    })
                    put("finish_reason", if (resp.toolCalls.isNotEmpty()) "tool_calls" else "stop")
                })
            }
            put("choices", choices)
            put("gateway_meta", JSONObject().apply {
                put("profile_used", resp.profileUsed)
                put("latency_ms", resp.latencyMs)
                put("fallback_attempts", JSONArray(fallbacks))
                if (resp.toolResults.isNotEmpty()) {
                    val trArr = JSONArray()
                    resp.toolResults.forEach { tr ->
                        trArr.put(JSONObject().apply {
                            put("call_id", tr.callId)
                            put("tool", tr.toolName)
                            put("is_error", tr.isError)
                            put("content", tr.content)
                        })
                    }
                    put("mcp_tool_results", trArr)
                }
            })
        }
    }

    private fun formatSseChunk(chunk: dev.motherofallapps.host.tool.llmgateway.model.ChatStreamChunk): JSONObject {
        return JSONObject().apply {
            put("id", chunk.id)
            put("object", "chat.completion.chunk")
            put("created", System.currentTimeMillis() / 1000)
            put("model", chunk.model)
            val choices = JSONArray().apply {
                put(JSONObject().apply {
                    put("index", 0)
                    put("delta", JSONObject().apply {
                        if (chunk.deltaContent.isNotEmpty()) {
                            put("content", chunk.deltaContent)
                        }
                    })
                    put("finish_reason", chunk.finishReason)
                })
            }
            put("choices", choices)
            put("profile_used", chunk.profileUsed)
        }
    }

    private fun sendJsonResponse(output: OutputStream, statusCode: Int, body: String) {
        val statusText = when (statusCode) {
            200 -> "OK"
            201 -> "Created"
            400 -> "Bad Request"
            404 -> "Not Found"
            500 -> "Internal Server Error"
            502 -> "Bad Gateway"
            else -> "Response"
        }
        val bytes = body.toByteArray(Charsets.UTF_8)
        val header = "HTTP/1.1 $statusCode $statusText\r\n" +
                "Content-Type: application/json; charset=utf-8\r\n" +
                "Content-Length: ${bytes.size}\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Access-Control-Allow-Headers: *\r\n" +
                "Connection: close\r\n\r\n"
        output.write(header.toByteArray(Charsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    private fun sendCorsResponse(output: OutputStream) {
        val header = "HTTP/1.1 204 No Content\r\n" +
                "Access-Control-Allow-Origin: *\r\n" +
                "Access-Control-Allow-Methods: GET, POST, OPTIONS, DELETE\r\n" +
                "Access-Control-Allow-Headers: *\r\n" +
                "Connection: close\r\n\r\n"
        output.write(header.toByteArray(Charsets.UTF_8))
        output.flush()
    }
}
