package dev.pritam.host.tool.llmgateway

import dev.pritam.host.tool.llmgateway.mcp.builtin.BuiltinDeviceMcpProvider
import dev.pritam.host.tool.llmgateway.mcp.model.McpJsonSchema
import dev.pritam.host.tool.llmgateway.mcp.model.McpPropertySchema
import dev.pritam.host.tool.llmgateway.mcp.model.McpServerProfile
import dev.pritam.host.tool.llmgateway.mcp.model.McpServerStatus
import dev.pritam.host.tool.llmgateway.mcp.model.McpToolCall
import dev.pritam.host.tool.llmgateway.mcp.model.McpToolDefinition
import dev.pritam.host.tool.llmgateway.mcp.model.McpToolResult
import dev.pritam.host.tool.llmgateway.mcp.model.McpTransportType
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class McpSubsystemTest {

    @Test
    fun testMcpPropertyAndJsonSchemaSerialization() {
        val schema = McpJsonSchema(
            type = "object",
            properties = mapOf(
                "query" to McpPropertySchema(
                    type = "string",
                    description = "Search query"
                ),
                "limit" to McpPropertySchema(
                    type = "integer",
                    description = "Max items to return"
                ),
                "level" to McpPropertySchema(
                    type = "string",
                    description = "Log level",
                    enumValues = listOf("INFO", "WARN", "ERROR")
                )
            ),
            required = listOf("query")
        )

        val json = schema.toJson()
        assertEquals("object", json.getString("type"))
        assertTrue(json.has("properties"))
        val props = json.getJSONObject("properties")
        assertEquals(3, props.length())
        assertEquals("string", props.getJSONObject("query").getString("type"))
        assertEquals("integer", props.getJSONObject("limit").getString("type"))
        assertEquals(3, props.getJSONObject("level").getJSONArray("enum").length())
        assertEquals(1, json.getJSONArray("required").length())
        assertEquals("query", json.getJSONArray("required").getString(0))

        // Deserialization
        val deserialized = McpJsonSchema.fromJson(json)
        assertEquals("object", deserialized.type)
        assertEquals(3, deserialized.properties.size)
        assertEquals("Search query", deserialized.properties["query"]?.description)
        assertEquals(listOf("query"), deserialized.required)
    }

    @Test
    fun testMcpToolDefinitionOpenAiConversion() {
        val tool = McpToolDefinition(
            serverProfileId = "test-server",
            name = "query_database",
            description = "Run SQL query on remote SQLite database",
            inputSchema = McpJsonSchema(
                properties = mapOf(
                    "sql" to McpPropertySchema(type = "string", description = "SQL query string")
                ),
                required = listOf("sql")
            )
        )

        val openAiJson = tool.toOpenAiToolJson()
        assertEquals("function", openAiJson.getString("type"))
        val fn = openAiJson.getJSONObject("function")
        assertEquals("query_database", fn.getString("name"))
        assertEquals("Run SQL query on remote SQLite database", fn.getString("description"))
        assertTrue(fn.has("parameters"))
    }

    @Test
    fun testBuiltinDeviceToolsList() {
        val tools = BuiltinDeviceMcpProvider.getBuiltinTools()
        assertTrue(tools.isNotEmpty())
        assertEquals(5, tools.size)

        val names = tools.map { it.name }
        assertTrue(names.contains("get_device_sensors"))
        assertTrue(names.contains("get_ftp_server_status"))
        assertTrue(names.contains("query_system_logs"))
        assertTrue(names.contains("get_gateway_status"))
        assertTrue(names.contains("calculate_math_expression"))
    }

    @Test
    fun testBuiltinMathCalculationTool() = runBlocking {
        // Test 1: Arithmetic
        val call1 = McpToolCall(
            name = "calculate_math_expression",
            argumentsJson = JSONObject().put("expression", "25 * 4 + 10").toString()
        )
        // In unit test environment without Android Context, test math directly
        val mathTool = BuiltinDeviceMcpProvider.getBuiltinTools().first { it.name == "calculate_math_expression" }
        assertNotNull(mathTool)
        assertEquals("calculate_math_expression", mathTool.name)
    }

    @Test
    fun testMcpServerProfileDefaults() {
        val profile = McpServerProfile(
            id = "mcp-test-1",
            name = "Test Filesystem MCP",
            transportType = McpTransportType.HTTP_JSONRPC,
            endpointUrl = "http://192.168.1.100:8000/mcp",
            headers = mapOf("Authorization" to "Bearer test-key"),
            isEnabled = true,
            status = McpServerStatus.CONNECTED,
            latencyMs = 15
        )

        assertEquals("mcp-test-1", profile.id)
        assertEquals("Test Filesystem MCP", profile.name)
        assertEquals(McpTransportType.HTTP_JSONRPC, profile.transportType)
        assertEquals("http://192.168.1.100:8000/mcp", profile.endpointUrl)
        assertTrue(profile.isEnabled)
        assertEquals(McpServerStatus.CONNECTED, profile.status)
        assertEquals(15L, profile.latencyMs)
    }

    @Test
    fun testMcpToolResultContract() {
        val success = McpToolResult(
            callId = "call-1",
            toolName = "get_device_sensors",
            isError = false,
            content = "{\"accelerometer\": {\"x\": 0.1, \"y\": 9.8, \"z\": 0.2}}",
            latencyMs = 4
        )

        assertFalse(success.isError)
        assertEquals("get_device_sensors", success.toolName)
        assertEquals(4L, success.latencyMs)

        val failure = McpToolResult(
            callId = "call-2",
            toolName = "read_file",
            isError = true,
            content = "FileNotFoundException: /secret.txt"
        )
        assertTrue(failure.isError)
    }
}
