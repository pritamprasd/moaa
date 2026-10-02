package dev.pritam.host.tool.llmgateway.mcp.client

import dev.pritam.host.logging.AppLogHub
import dev.pritam.host.logging.LogLevel
import dev.pritam.host.tool.llmgateway.mcp.model.McpJsonSchema
import dev.pritam.host.tool.llmgateway.mcp.model.McpServerProfile
import dev.pritam.host.tool.llmgateway.mcp.model.McpToolCall
import dev.pritam.host.tool.llmgateway.mcp.model.McpToolDefinition
import dev.pritam.host.tool.llmgateway.mcp.model.McpToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class McpClient {

    suspend fun ping(server: McpServerProfile): Pair<Boolean, Long> = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            val endpoint = server.endpointUrl.trim()
            val url = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json, text/event-stream")
            server.headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            conn.doOutput = true

            val rpcReq = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("id", UUID.randomUUID().toString())
                put("method", "ping")
                put("params", JSONObject())
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(rpcReq.toString()) }
            val code = conn.responseCode
            val isSuccess = code in 200..299
            val latency = System.currentTimeMillis() - start
            Pair(isSuccess, latency)
        } catch (e: Exception) {
            Pair(false, System.currentTimeMillis() - start)
        }
    }

    suspend fun listTools(server: McpServerProfile): List<McpToolDefinition> = withContext(Dispatchers.IO) {
        val endpoint = server.endpointUrl.trim()
        val url = URL(endpoint)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.connectTimeout = 6000
        conn.readTimeout = 8000
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json")
        server.headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
        conn.doOutput = true

        val rpcReq = JSONObject().apply {
            put("jsonrpc", "2.0")
            put("id", "list-tools-${UUID.randomUUID()}")
            put("method", "tools/list")
            put("params", JSONObject())
        }

        OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(rpcReq.toString()) }
        val code = conn.responseCode
        if (code !in 200..299) {
            val errStream = conn.errorStream ?: conn.inputStream
            val errText = BufferedReader(InputStreamReader(errStream, "UTF-8")).readText()
            throw RuntimeException("HTTP $code from MCP server: $errText")
        }

        val respText = BufferedReader(InputStreamReader(conn.inputStream, "UTF-8")).readText()
        val root = JSONObject(respText)
        if (root.has("error") && !root.isNull("error")) {
            val errObj = root.getJSONObject("error")
            throw RuntimeException("JSON-RPC Error [${errObj.optInt("code")}]: ${errObj.optString("message")}")
        }

        val resultObj = root.optJSONObject("result") ?: JSONObject()
        val toolsArray = resultObj.optJSONArray("tools") ?: JSONArray()
        val toolsList = mutableListOf<McpToolDefinition>()

        for (i in 0 until toolsArray.length()) {
            val tObj = toolsArray.getJSONObject(i)
            val name = tObj.optString("name", "")
            val desc = tObj.optString("description", "")
            val schemaObj = tObj.optJSONObject("inputSchema") ?: JSONObject()
            val schema = McpJsonSchema.fromJson(schemaObj)

            if (name.isNotBlank()) {
                toolsList.add(
                    McpToolDefinition(
                        serverProfileId = server.id,
                        name = name,
                        description = desc,
                        inputSchema = schema,
                        isEnabled = true
                    )
                )
            }
        }

        AppLogHub.log(
            toolId = "llm-gateway",
            toolName = "LLM Gateway",
            level = LogLevel.INFO,
            tag = "MCP",
            message = "MCP CLIENT [DISCOVERY] Successfully retrieved ${toolsList.size} tools from '${server.name}' ($endpoint)"
        )

        toolsList
    }

    suspend fun callTool(server: McpServerProfile, toolCall: McpToolCall): McpToolResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            val endpoint = server.endpointUrl.trim()
            val url = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 10000
            conn.readTimeout = 20000
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json")
            server.headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            conn.doOutput = true

            val argsObj = if (toolCall.argumentsJson.isNotBlank()) JSONObject(toolCall.argumentsJson) else JSONObject()
            val rpcReq = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("id", toolCall.id)
                put("method", "tools/call")
                put("params", JSONObject().apply {
                    put("name", toolCall.name)
                    put("arguments", argsObj)
                })
            }

            OutputStreamWriter(conn.outputStream, "UTF-8").use { it.write(rpcReq.toString()) }
            val code = conn.responseCode
            val isSuccess = code in 200..299
            val stream = if (isSuccess) conn.inputStream else conn.errorStream ?: conn.inputStream
            val respText = BufferedReader(InputStreamReader(stream, "UTF-8")).readText()

            if (!isSuccess) {
                return@withContext McpToolResult(
                    callId = toolCall.id,
                    toolName = toolCall.name,
                    isError = true,
                    content = "HTTP $code from MCP server: $respText",
                    latencyMs = System.currentTimeMillis() - start
                )
            }

            val root = JSONObject(respText)
            if (root.has("error") && !root.isNull("error")) {
                val errObj = root.getJSONObject("error")
                return@withContext McpToolResult(
                    callId = toolCall.id,
                    toolName = toolCall.name,
                    isError = true,
                    content = "JSON-RPC Error [${errObj.optInt("code")}]: ${errObj.optString("message")}",
                    latencyMs = System.currentTimeMillis() - start
                )
            }

            val resultObj = root.optJSONObject("result") ?: JSONObject()
            val contentArr = resultObj.optJSONArray("content")
            val isError = resultObj.optBoolean("isError", false)

            val formattedContent = if (contentArr != null && contentArr.length() > 0) {
                val sb = StringBuilder()
                for (i in 0 until contentArr.length()) {
                    val item = contentArr.getJSONObject(i)
                    val text = item.optString("text")
                    if (text.isNotEmpty()) {
                        sb.append(text).append("\n")
                    } else {
                        sb.append(item.toString(2)).append("\n")
                    }
                }
                sb.toString().trim()
            } else {
                resultObj.toString(2)
            }

            AppLogHub.log(
                toolId = "llm-gateway",
                toolName = "LLM Gateway",
                level = if (isError) LogLevel.WARN else LogLevel.INFO,
                tag = "MCP",
                message = "MCP TOOL EXECUTE [${toolCall.name}] Server: '${server.name}' (${System.currentTimeMillis() - start}ms)"
            )

            McpToolResult(
                callId = toolCall.id,
                toolName = toolCall.name,
                isError = isError,
                content = formattedContent,
                latencyMs = System.currentTimeMillis() - start
            )
        } catch (e: Exception) {
            McpToolResult(
                callId = toolCall.id,
                toolName = toolCall.name,
                isError = true,
                content = "Remote tool call exception: ${e.message ?: e.javaClass.simpleName}",
                latencyMs = System.currentTimeMillis() - start
            )
        }
    }
}
