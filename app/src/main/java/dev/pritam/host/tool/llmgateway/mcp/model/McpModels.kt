package dev.pritam.host.tool.llmgateway.mcp.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class McpTransportType(val displayName: String, val badge: String) {
    BUILTIN_DEVICE("Built-in Device Tools", "📱"),
    HTTP_JSONRPC("HTTP JSON-RPC 2.0", "🌐"),
    SSE("Server-Sent Events (SSE)", "⚡")
}

enum class McpServerStatus(val displayName: String, val badgeColorHex: Long) {
    CONNECTED("Connected", 0xFF34D399),       // Emerald
    CONNECTING("Connecting...", 0xFF38BDF8),  // Sky
    DISCONNECTED("Disconnected", 0xFF94A3B8), // Slate
    ERROR("Connection Error", 0xFFF43F5E)     // Rose
}

data class McpPropertySchema(
    val type: String, // "string", "number", "integer", "boolean", "object", "array"
    val description: String = "",
    val enumValues: List<String> = emptyList(),
    val default: Any? = null
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("type", type)
        if (description.isNotBlank()) obj.put("description", description)
        if (enumValues.isNotEmpty()) {
            val arr = JSONArray()
            enumValues.forEach { arr.put(it) }
            obj.put("enum", arr)
        }
        return obj
    }

    companion object {
        fun fromJson(json: JSONObject): McpPropertySchema {
            val type = json.optString("type", "string")
            val desc = json.optString("description", "")
            val enumList = mutableListOf<String>()
            val enumArr = json.optJSONArray("enum")
            if (enumArr != null) {
                for (i in 0 until enumArr.length()) {
                    enumList.add(enumArr.getString(i))
                }
            }
            return McpPropertySchema(type = type, description = desc, enumValues = enumList)
        }
    }
}

data class McpJsonSchema(
    val type: String = "object",
    val properties: Map<String, McpPropertySchema> = emptyMap(),
    val required: List<String> = emptyList(),
    val description: String = ""
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("type", type)
        if (description.isNotBlank()) obj.put("description", description)
        val propsObj = JSONObject()
        properties.forEach { (k, v) -> propsObj.put(k, v.toJson()) }
        obj.put("properties", propsObj)
        if (required.isNotEmpty()) {
            val reqArr = JSONArray()
            required.forEach { reqArr.put(it) }
            obj.put("required", reqArr)
        }
        return obj
    }

    companion object {
        fun fromJson(json: JSONObject): McpJsonSchema {
            val type = json.optString("type", "object")
            val desc = json.optString("description", "")
            val props = mutableMapOf<String, McpPropertySchema>()
            val propsObj = json.optJSONObject("properties")
            if (propsObj != null) {
                val keys = propsObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val pObj = propsObj.getJSONObject(k)
                    props[k] = McpPropertySchema.fromJson(pObj)
                }
            }
            val req = mutableListOf<String>()
            val reqArr = json.optJSONArray("required")
            if (reqArr != null) {
                for (i in 0 until reqArr.length()) {
                    req.add(reqArr.getString(i))
                }
            }
            return McpJsonSchema(type = type, properties = props, required = req, description = desc)
        }
    }
}

data class McpToolDefinition(
    val serverProfileId: String,
    val name: String,
    val description: String,
    val inputSchema: McpJsonSchema = McpJsonSchema(),
    val isEnabled: Boolean = true
) {
    fun toOpenAiToolJson(): JSONObject {
        val fnObj = JSONObject().apply {
            put("name", name)
            put("description", description)
            put("parameters", inputSchema.toJson())
        }
        return JSONObject().apply {
            put("type", "function")
            put("function", fnObj)
        }
    }
}

data class McpServerProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val transportType: McpTransportType,
    val endpointUrl: String,
    val headers: Map<String, String> = emptyMap(),
    val isEnabled: Boolean = true,
    val status: McpServerStatus = McpServerStatus.DISCONNECTED,
    val latencyMs: Long = 0L,
    val discoveredTools: List<McpToolDefinition> = emptyList(),
    val lastSyncedAt: Long = 0L,
    val errorMessage: String? = null
)

data class McpToolCall(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val argumentsJson: String
)

data class McpToolResult(
    val callId: String,
    val toolName: String,
    val isError: Boolean,
    val content: String,
    val latencyMs: Long = 0L
)
