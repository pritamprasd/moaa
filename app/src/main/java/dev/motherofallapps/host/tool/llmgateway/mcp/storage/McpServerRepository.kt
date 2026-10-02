package dev.motherofallapps.host.tool.llmgateway.mcp.storage

import android.content.Context
import dev.motherofallapps.host.logging.AppLogHub
import dev.motherofallapps.host.logging.LogLevel
import dev.motherofallapps.host.tool.llmgateway.mcp.builtin.BuiltinDeviceMcpProvider
import dev.motherofallapps.host.tool.llmgateway.mcp.client.McpClient
import dev.motherofallapps.host.tool.llmgateway.mcp.model.McpJsonSchema
import dev.motherofallapps.host.tool.llmgateway.mcp.model.McpServerProfile
import dev.motherofallapps.host.tool.llmgateway.mcp.model.McpServerStatus
import dev.motherofallapps.host.tool.llmgateway.mcp.model.McpToolCall
import dev.motherofallapps.host.tool.llmgateway.mcp.model.McpToolDefinition
import dev.motherofallapps.host.tool.llmgateway.mcp.model.McpToolResult
import dev.motherofallapps.host.tool.llmgateway.mcp.model.McpTransportType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class McpServerRepository(private val context: Context) {

    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val storageFile: File by lazy { File(context.filesDir, "mcp_server_configs.json") }
    private val mcpClient = McpClient()

    private val _servers = MutableStateFlow<List<McpServerProfile>>(emptyList())
    val servers: StateFlow<List<McpServerProfile>> = _servers.asStateFlow()

    init {
        loadFromDisk()
    }

    private fun loadFromDisk() {
        if (!storageFile.exists()) {
            val defaults = listOf(
                BuiltinDeviceMcpProvider.getBuiltinServerProfile(),
                McpServerProfile(
                    id = "sample-desktop-mcp",
                    name = "Desktop Local LAN MCP",
                    transportType = McpTransportType.HTTP_JSONRPC,
                    endpointUrl = "http://192.168.1.50:8000/mcp",
                    isEnabled = false,
                    status = McpServerStatus.DISCONNECTED,
                    discoveredTools = emptyList()
                )
            )
            _servers.value = defaults
            saveToDisk(defaults)
            return
        }

        try {
            val jsonStr = storageFile.readText()
            val root = JSONObject(jsonStr)
            val arr = root.optJSONArray("servers") ?: JSONArray()
            val parsedList = mutableListOf<McpServerProfile>()

            for (i in 0 until arr.length()) {
                val sObj = arr.getJSONObject(i)
                val id = sObj.optString("id", UUID.randomUUID().toString())
                val name = sObj.optString("name", "MCP Server")
                val transport = try {
                    McpTransportType.valueOf(sObj.optString("transportType", "HTTP_JSONRPC"))
                } catch (e: Exception) {
                    McpTransportType.HTTP_JSONRPC
                }
                val endpoint = sObj.optString("endpointUrl", "")
                val isEnabled = sObj.optBoolean("isEnabled", true)
                val status = try {
                    McpServerStatus.valueOf(sObj.optString("status", "DISCONNECTED"))
                } catch (e: Exception) {
                    McpServerStatus.DISCONNECTED
                }
                val latency = sObj.optLong("latencyMs", 0L)
                val lastSynced = sObj.optLong("lastSyncedAt", 0L)

                // Headers
                val headersMap = mutableMapOf<String, String>()
                val hObj = sObj.optJSONObject("headers")
                if (hObj != null) {
                    val kIter = hObj.keys()
                    while (kIter.hasNext()) {
                        val k = kIter.next()
                        headersMap[k] = hObj.getString(k)
                    }
                }

                // Discovered Tools
                val toolsList = mutableListOf<McpToolDefinition>()
                if (transport == McpTransportType.BUILTIN_DEVICE) {
                    toolsList.addAll(BuiltinDeviceMcpProvider.getBuiltinTools())
                } else {
                    val toolsArr = sObj.optJSONArray("discoveredTools") ?: JSONArray()
                    for (j in 0 until toolsArr.length()) {
                        val tObj = toolsArr.getJSONObject(j)
                        val tName = tObj.optString("name", "")
                        val tDesc = tObj.optString("description", "")
                        val schemaObj = tObj.optJSONObject("inputSchema") ?: JSONObject()
                        val schema = McpJsonSchema.fromJson(schemaObj)
                        val tEnabled = tObj.optBoolean("isEnabled", true)
                        if (tName.isNotBlank()) {
                            toolsList.add(
                                McpToolDefinition(
                                    serverProfileId = id,
                                    name = tName,
                                    description = tDesc,
                                    inputSchema = schema,
                                    isEnabled = tEnabled
                                )
                            )
                        }
                    }
                }

                parsedList.add(
                    McpServerProfile(
                        id = id,
                        name = name,
                        transportType = transport,
                        endpointUrl = endpoint,
                        headers = headersMap,
                        isEnabled = isEnabled,
                        status = status,
                        latencyMs = latency,
                        discoveredTools = toolsList,
                        lastSyncedAt = lastSynced
                    )
                )
            }

            // Ensure Builtin device profile exists
            if (parsedList.none { it.transportType == McpTransportType.BUILTIN_DEVICE }) {
                parsedList.add(0, BuiltinDeviceMcpProvider.getBuiltinServerProfile())
            }

            _servers.value = parsedList
        } catch (e: Exception) {
            val defaults = listOf(BuiltinDeviceMcpProvider.getBuiltinServerProfile())
            _servers.value = defaults
        }
    }

    private fun saveToDisk(list: List<McpServerProfile>) {
        scope.launch {
            mutex.withLock {
                try {
                    val root = JSONObject()
                    val arr = JSONArray()

                    list.forEach { server ->
                        val sObj = JSONObject()
                        sObj.put("id", server.id)
                        sObj.put("name", server.name)
                        sObj.put("transportType", server.transportType.name)
                        sObj.put("endpointUrl", server.endpointUrl)
                        sObj.put("isEnabled", server.isEnabled)
                        sObj.put("status", server.status.name)
                        sObj.put("latencyMs", server.latencyMs)
                        sObj.put("lastSyncedAt", server.lastSyncedAt)

                        val hObj = JSONObject()
                        server.headers.forEach { (k, v) -> hObj.put(k, v) }
                        sObj.put("headers", hObj)

                        val tArr = JSONArray()
                        server.discoveredTools.forEach { t ->
                            val tObj = JSONObject()
                            tObj.put("name", t.name)
                            tObj.put("description", t.description)
                            tObj.put("inputSchema", t.inputSchema.toJson())
                            tObj.put("isEnabled", t.isEnabled)
                            tArr.put(tObj)
                        }
                        sObj.put("discoveredTools", tArr)

                        arr.put(sObj)
                    }

                    root.put("servers", arr)
                    storageFile.writeText(root.toString(2))
                } catch (e: Exception) {
                    AppLogHub.log(
                        toolId = "llm-gateway",
                        toolName = "LLM Gateway",
                        level = LogLevel.ERROR,
                        tag = "MCP",
                        message = "MCP STORAGE [ERROR] Failed to save MCP servers: ${e.message}"
                    )
                }
            }
        }
    }

    fun addServer(server: McpServerProfile) {
        val updated = _servers.value + server
        _servers.value = updated
        saveToDisk(updated)
    }

    fun updateServer(server: McpServerProfile) {
        val updated = _servers.value.map { if (it.id == server.id) server else it }
        _servers.value = updated
        saveToDisk(updated)
    }

    fun deleteServer(serverId: String) {
        val updated = _servers.value.filterNot { it.id == serverId && it.transportType != McpTransportType.BUILTIN_DEVICE }
        _servers.value = updated
        saveToDisk(updated)
    }

    fun toggleServerEnabled(serverId: String, isEnabled: Boolean) {
        val updated = _servers.value.map {
            if (it.id == serverId) it.copy(isEnabled = isEnabled) else it
        }
        _servers.value = updated
        saveToDisk(updated)
    }

    fun toggleToolEnabled(serverId: String, toolName: String, isEnabled: Boolean) {
        val updated = _servers.value.map { server ->
            if (server.id == serverId) {
                val updatedTools = server.discoveredTools.map { tool ->
                    if (tool.name == toolName) tool.copy(isEnabled = isEnabled) else tool
                }
                server.copy(discoveredTools = updatedTools)
            } else server
        }
        _servers.value = updated
        saveToDisk(updated)
    }

    suspend fun syncToolsForServer(serverId: String): McpServerProfile {
        val server = _servers.value.firstOrNull { it.id == serverId } ?: return BuiltinDeviceMcpProvider.getBuiltinServerProfile()

        if (server.transportType == McpTransportType.BUILTIN_DEVICE) {
            val updated = server.copy(
                status = McpServerStatus.CONNECTED,
                latencyMs = 2,
                discoveredTools = BuiltinDeviceMcpProvider.getBuiltinTools(),
                lastSyncedAt = System.currentTimeMillis(),
                errorMessage = null
            )
            updateServer(updated)
            return updated
        }

        // Set connecting state
        updateServer(server.copy(status = McpServerStatus.CONNECTING))

        val start = System.currentTimeMillis()
        return try {
            val tools = mcpClient.listTools(server)
            val updated = server.copy(
                status = McpServerStatus.CONNECTED,
                latencyMs = System.currentTimeMillis() - start,
                discoveredTools = tools,
                lastSyncedAt = System.currentTimeMillis(),
                errorMessage = null
            )
            updateServer(updated)
            updated
        } catch (e: Exception) {
            val errorMsg = e.message ?: e.javaClass.simpleName
            val updated = server.copy(
                status = McpServerStatus.ERROR,
                latencyMs = System.currentTimeMillis() - start,
                errorMessage = errorMsg
            )
            updateServer(updated)
            updated
        }
    }

    suspend fun pingServer(serverId: String): McpServerProfile {
        val server = _servers.value.firstOrNull { it.id == serverId } ?: return BuiltinDeviceMcpProvider.getBuiltinServerProfile()

        if (server.transportType == McpTransportType.BUILTIN_DEVICE) {
            val updated = server.copy(status = McpServerStatus.CONNECTED, latencyMs = 1)
            updateServer(updated)
            return updated
        }

        val (isSuccess, latency) = mcpClient.ping(server)
        val newStatus = if (isSuccess) McpServerStatus.CONNECTED else McpServerStatus.ERROR
        val updated = server.copy(status = newStatus, latencyMs = latency)
        updateServer(updated)
        return updated
    }

    fun getAllActiveTools(): List<McpToolDefinition> {
        val activeServers = _servers.value.filter { it.isEnabled }
        val tools = mutableListOf<McpToolDefinition>()
        activeServers.forEach { server ->
            tools.addAll(server.discoveredTools.filter { it.isEnabled })
        }
        return tools
    }

    suspend fun executeToolCall(toolCall: McpToolCall): McpToolResult {
        val allActiveTools = getAllActiveTools()
        val toolDef = allActiveTools.firstOrNull { it.name == toolCall.name }

        if (toolDef == null) {
            return McpToolResult(
                callId = toolCall.id,
                toolName = toolCall.name,
                isError = true,
                content = "No enabled MCP tool found with name '${toolCall.name}'"
            )
        }

        val server = _servers.value.firstOrNull { it.id == toolDef.serverProfileId }
        if (server == null) {
            return McpToolResult(
                callId = toolCall.id,
                toolName = toolCall.name,
                isError = true,
                content = "Parent MCP server not found for tool '${toolCall.name}'"
            )
        }

        return if (server.transportType == McpTransportType.BUILTIN_DEVICE) {
            BuiltinDeviceMcpProvider.executeTool(context, toolCall)
        } else {
            mcpClient.callTool(server, toolCall)
        }
    }
}
