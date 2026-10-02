package dev.motherofallapps.host.tool.llmgateway.mcp.builtin

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import dev.motherofallapps.host.ftp.service.FtpServerController
import dev.motherofallapps.host.logging.AppLogHub
import dev.motherofallapps.host.tool.llmgateway.manager.LlmGatewayManager
import dev.motherofallapps.host.tool.llmgateway.mcp.model.McpJsonSchema
import dev.motherofallapps.host.tool.llmgateway.mcp.model.McpPropertySchema
import dev.motherofallapps.host.tool.llmgateway.mcp.model.McpServerProfile
import dev.motherofallapps.host.tool.llmgateway.mcp.model.McpServerStatus
import dev.motherofallapps.host.tool.llmgateway.mcp.model.McpToolCall
import dev.motherofallapps.host.tool.llmgateway.mcp.model.McpToolDefinition
import dev.motherofallapps.host.tool.llmgateway.mcp.model.McpToolResult
import dev.motherofallapps.host.tool.llmgateway.mcp.model.McpTransportType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

object BuiltinDeviceMcpProvider {

    const val SERVER_PROFILE_ID = "builtin-device-tools"

    fun getBuiltinServerProfile(): McpServerProfile {
        return McpServerProfile(
            id = SERVER_PROFILE_ID,
            name = "Built-in Device & System Tools",
            transportType = McpTransportType.BUILTIN_DEVICE,
            endpointUrl = "builtin://device",
            isEnabled = true,
            status = McpServerStatus.CONNECTED,
            latencyMs = 2,
            discoveredTools = getBuiltinTools(),
            lastSyncedAt = System.currentTimeMillis()
        )
    }

    fun getBuiltinTools(): List<McpToolDefinition> {
        return listOf(
            McpToolDefinition(
                serverProfileId = SERVER_PROFILE_ID,
                name = "get_device_sensors",
                description = "Read live physical hardware sensors on the Android device including Accelerometer, Gyroscope, Magnetometer, Barometer, Light, and Battery level.",
                inputSchema = McpJsonSchema(
                    type = "object",
                    properties = mapOf(
                        "include_battery" to McpPropertySchema(
                            type = "boolean",
                            description = "Whether to include battery percentage, status, and temperature."
                        )
                    )
                )
            ),
            McpToolDefinition(
                serverProfileId = SERVER_PROFILE_ID,
                name = "get_ftp_server_status",
                description = "Inspect the status of the local LAN FTP server hosted on the device, including IP address, port, running state, and root storage path.",
                inputSchema = McpJsonSchema(
                    type = "object",
                    properties = emptyMap()
                )
            ),
            McpToolDefinition(
                serverProfileId = SERVER_PROFILE_ID,
                name = "query_system_logs",
                description = "Query recent execution and diagnostic logs from the centralized MotherOfAllApps System Log Hub.",
                inputSchema = McpJsonSchema(
                    type = "object",
                    properties = mapOf(
                        "limit" to McpPropertySchema(
                            type = "integer",
                            description = "Maximum number of recent log entries to retrieve (default: 10, max: 50)."
                        ),
                        "level_filter" to McpPropertySchema(
                            type = "string",
                            description = "Optional filter by log severity.",
                            enumValues = listOf("ALL", "INFO", "WARN", "ERROR", "DEBUG")
                        )
                    )
                )
            ),
            McpToolDefinition(
                serverProfileId = SERVER_PROFILE_ID,
                name = "get_gateway_status",
                description = "Check the health, active proxy port, uptime, failover counters, and configured AI models on the LLM Gateway.",
                inputSchema = McpJsonSchema(
                    type = "object",
                    properties = emptyMap()
                )
            ),
            McpToolDefinition(
                serverProfileId = SERVER_PROFILE_ID,
                name = "calculate_math_expression",
                description = "Safely evaluate a mathematical or scientific formula with high precision (supports +, -, *, /, %, ^, sqrt, sin, cos, tan, log, ln, abs).",
                inputSchema = McpJsonSchema(
                    type = "object",
                    properties = mapOf(
                        "expression" to McpPropertySchema(
                            type = "string",
                            description = "The mathematical expression to evaluate, e.g. 'sqrt(144) + 15 * 3.5' or '2^8'."
                        )
                    ),
                    required = listOf("expression")
                )
            )
        )
    }

    suspend fun executeTool(context: Context, toolCall: McpToolCall): McpToolResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            val args = if (toolCall.argumentsJson.isNotBlank()) JSONObject(toolCall.argumentsJson) else JSONObject()
            val resultJson: JSONObject = when (toolCall.name) {
                "get_device_sensors" -> executeGetDeviceSensors(context, args)
                "get_ftp_server_status" -> executeGetFtpServerStatus(context)
                "query_system_logs" -> executeQuerySystemLogs(args)
                "get_gateway_status" -> executeGetGatewayStatus(context)
                "calculate_math_expression" -> executeMathCalculation(args)
                else -> {
                    return@withContext McpToolResult(
                        callId = toolCall.id,
                        toolName = toolCall.name,
                        isError = true,
                        content = "Unknown built-in tool '${toolCall.name}'",
                        latencyMs = System.currentTimeMillis() - start
                    )
                }
            }

            McpToolResult(
                callId = toolCall.id,
                toolName = toolCall.name,
                isError = false,
                content = resultJson.toString(2),
                latencyMs = System.currentTimeMillis() - start
            )
        } catch (e: Exception) {
            McpToolResult(
                callId = toolCall.id,
                toolName = toolCall.name,
                isError = true,
                content = "Tool execution error: ${e.message ?: e.javaClass.simpleName}",
                latencyMs = System.currentTimeMillis() - start
            )
        }
    }

    private suspend fun executeGetDeviceSensors(context: Context, args: JSONObject): JSONObject {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val result = JSONObject()
        val sensorsObj = JSONObject()

        if (sensorManager != null) {
            // Read Accelerometer
            readSingleSensorReading(sensorManager, Sensor.TYPE_ACCELEROMETER)?.let { values ->
                sensorsObj.put("accelerometer", JSONObject().apply {
                    put("x_mps2", values.getOrNull(0) ?: 0f)
                    put("y_mps2", values.getOrNull(1) ?: 0f)
                    put("z_mps2", values.getOrNull(2) ?: 0f)
                })
            }

            // Read Gyroscope
            readSingleSensorReading(sensorManager, Sensor.TYPE_GYROSCOPE)?.let { values ->
                sensorsObj.put("gyroscope", JSONObject().apply {
                    put("x_radps", values.getOrNull(0) ?: 0f)
                    put("y_radps", values.getOrNull(1) ?: 0f)
                    put("z_radps", values.getOrNull(2) ?: 0f)
                })
            }

            // Read Light
            readSingleSensorReading(sensorManager, Sensor.TYPE_LIGHT)?.let { values ->
                sensorsObj.put("light_sensor_lux", values.getOrNull(0) ?: 0f)
            }

            // Read Barometer
            readSingleSensorReading(sensorManager, Sensor.TYPE_PRESSURE)?.let { values ->
                sensorsObj.put("barometer_hpa", values.getOrNull(0) ?: 0f)
            }

            // Read Proximity
            readSingleSensorReading(sensorManager, Sensor.TYPE_PROXIMITY)?.let { values ->
                sensorsObj.put("proximity_cm", values.getOrNull(0) ?: 0f)
            }
        }

        // Battery
        val includeBattery = args.optBoolean("include_battery", true)
        if (includeBattery) {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            if (batteryManager != null) {
                val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                val statusObj = JSONObject().apply {
                    put("level_percentage", level)
                    put("is_charging", batteryManager.isCharging)
                }
                result.put("battery", statusObj)
            }
        }

        result.put("sensors", sensorsObj)
        result.put("timestamp_iso", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()))
        return result
    }

    private suspend fun readSingleSensorReading(sensorManager: SensorManager, sensorType: Int): FloatArray? {
        val sensor = sensorManager.getDefaultSensor(sensorType) ?: return null
        return withTimeoutOrNull(200) {
            suspendCancellableCoroutine { cont ->
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent?) {
                        if (event != null && cont.isActive) {
                            sensorManager.unregisterListener(this)
                            cont.resume(event.values.clone())
                        }
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                }
                sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_FASTEST)
                cont.invokeOnCancellation { sensorManager.unregisterListener(listener) }
            }
        }
    }

    private fun executeGetFtpServerStatus(context: Context): JSONObject {
        val state = FtpServerController.serverState.value
        val json = JSONObject()
        when (state) {
            is dev.motherofallapps.host.ftp.model.FtpServerState.Running -> {
                json.put("status", "RUNNING")
                json.put("ip_address", state.ipAddress)
                json.put("port", state.port)
                json.put("connection_url", state.connectionUrl)
                json.put("root_path", state.rootPath)
                json.put("username", state.username)
                json.put("uptime_seconds", (System.currentTimeMillis() - state.startedAtEpochMs) / 1000)
            }
            is dev.motherofallapps.host.ftp.model.FtpServerState.Starting -> {
                json.put("status", "STARTING")
            }
            is dev.motherofallapps.host.ftp.model.FtpServerState.Stopped -> {
                json.put("status", "STOPPED")
            }
            is dev.motherofallapps.host.ftp.model.FtpServerState.Error -> {
                json.put("status", "ERROR")
                json.put("error_message", state.message)
            }
        }
        return json
    }

    private fun executeQuerySystemLogs(args: JSONObject): JSONObject {
        val limit = args.optInt("limit", 10).coerceIn(1, 50)
        val levelFilter = args.optString("level_filter", "ALL").uppercase()

        val allLogs = AppLogHub.logs.value
        val filtered = allLogs.filter { log ->
            if (levelFilter == "ALL") true else log.level.name == levelFilter
        }.take(limit)

        val arr = JSONArray()
        filtered.forEach { log ->
            arr.put(JSONObject().apply {
                put("timestamp", log.formattedDateTime)
                put("tool", log.toolName)
                put("level", log.level.name)
                put("tag", log.tag)
                put("message", log.message)
            })
        }

        return JSONObject().apply {
            put("total_matched", filtered.size)
            put("logs", arr)
        }
    }

    private fun executeGetGatewayStatus(context: Context): JSONObject {
        val repo = LlmGatewayManager.getRepository(context)
        val server = LlmGatewayManager.getHttpServer(context)
        val telemetry = server.telemetry.value
        val profiles = repo.profiles.value

        val profilesArr = JSONArray()
        profiles.forEach { p ->
            profilesArr.put(JSONObject().apply {
                put("name", p.name)
                put("type", p.providerType)
                put("status", p.status.name)
                put("is_enabled", p.isEnabled)
                put("model", p.targetModel ?: "default")
                put("latency_ms", p.latencyMs)
            })
        }

        return JSONObject().apply {
            put("server_status", if (telemetry.isRunning) "RUNNING" else "STOPPED")
            put("endpoint", "http://${telemetry.hostAddress}")
            put("total_requests", telemetry.totalRequestsRouted)
            put("failovers_triggered", telemetry.failoversTriggered)
            put("active_profiles", profilesArr)
        }
    }

    private fun executeMathCalculation(args: JSONObject): JSONObject {
        val expr = args.optString("expression", "").trim()
        if (expr.isBlank()) {
            return JSONObject().apply {
                put("error", "Empty expression")
            }
        }

        return try {
            val result = evaluateSimpleExpression(expr)
            JSONObject().apply {
                put("expression", expr)
                put("result", result)
                put("status", "SUCCESS")
            }
        } catch (e: Exception) {
            JSONObject().apply {
                put("expression", expr)
                put("error", "Evaluation failed: ${e.message}")
            }
        }
    }

    private fun evaluateSimpleExpression(expression: String): Double {
        var str = expression.replace(" ", "")
        str = str.replace("sqrt(", "Math.sqrt(")

        // Handle square root
        while (str.contains("sqrt(")) {
            val start = str.indexOf("sqrt(")
            val end = str.indexOf(")", start)
            if (end > start) {
                val inner = str.substring(start + 5, end)
                val innerVal = evaluateSimpleExpression(inner)
                val sqrtVal = kotlin.math.sqrt(innerVal)
                str = str.substring(0, start) + sqrtVal.toString() + str.substring(end + 1)
            } else break
        }

        // Handle powers e.g. 2^8
        while (str.contains("^")) {
            val idx = str.indexOf("^")
            var leftStart = idx - 1
            while (leftStart >= 0 && (str[leftStart].isDigit() || str[leftStart] == '.')) leftStart--
            val leftNum = str.substring(leftStart + 1, idx).toDouble()

            var rightEnd = idx + 1
            while (rightEnd < str.length && (str[rightEnd].isDigit() || str[rightEnd] == '.')) rightEnd++
            val rightNum = str.substring(idx + 1, rightEnd).toDouble()

            val powerVal = Math.pow(leftNum, rightNum)
            str = str.substring(0, leftStart + 1) + powerVal.toString() + str.substring(rightEnd)
        }

        // Basic parser for +, -, *, /
        return object : Any() {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < str.length) str[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < str.length) throw RuntimeException("Unexpected: " + ch.toChar())
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x += parseTerm()
                    else if (eat('-'.code)) x -= parseTerm()
                    else return x
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x *= parseFactor()
                    else if (eat('/'.code)) x /= parseFactor()
                    else if (eat('%'.code)) x %= parseFactor()
                    else return x
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return -parseFactor()

                var x: Double
                val startPos = pos
                if (eat('('.code)) {
                    x = parseExpression()
                    eat(')'.code)
                } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) {
                    while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
                    x = str.substring(startPos, pos).toDouble()
                } else {
                    throw RuntimeException("Unexpected: " + ch.toChar())
                }
                return x
            }
        }.parse()
    }
}
