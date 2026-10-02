package dev.pritam.dynamictools.engine

import dev.pritam.dynamictools.model.DynamicToolBundle
import dev.pritam.dynamictools.model.DynamicToolManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class DynamicToolLlmClient(
    private val gatewayBaseUrl: String = "http://127.0.0.1:8080"
) {

    suspend fun generateTool(prompt: String): Result<DynamicToolBundle> = withContext(Dispatchers.IO) {
        val systemPrompt = DynamicToolPromptBuilder.buildSystemPrompt()
        val userPrompt = DynamicToolPromptBuilder.buildUserPrompt(prompt)
        callGateway(systemPrompt, userPrompt)
    }

    suspend fun refineTool(existingBundle: DynamicToolBundle, refinement: String): Result<DynamicToolBundle> = withContext(Dispatchers.IO) {
        val systemPrompt = DynamicToolPromptBuilder.buildSystemPrompt()
        val userPrompt = DynamicToolPromptBuilder.buildRefinePrompt(existingBundle, refinement)
        callGateway(systemPrompt, userPrompt)
    }

    private fun callGateway(systemPrompt: String, userPrompt: String): Result<DynamicToolBundle> {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL("$gatewayBaseUrl/v1/chat/completions")
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 45000
                readTimeout = 90000
                doOutput = true
                doInput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
            }

            val requestBody = JSONObject().apply {
                put("model", "default")
                put("temperature", 0.3)
                put("stream", false)
                val messages = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userPrompt)
                    })
                }
                put("messages", messages)
            }

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            val responseStream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: connection.inputStream
            }

            val responseText = BufferedReader(InputStreamReader(responseStream, "UTF-8")).use { reader ->
                reader.readText()
            }

            if (responseCode !in 200..299) {
                return Result.failure(Exception("LLM Gateway returned HTTP $responseCode: $responseText"))
            }

            val responseJson = JSONObject(responseText)
            val choices = responseJson.optJSONArray("choices")
            if (choices == null || choices.length() == 0) {
                return Result.failure(Exception("No choices returned from LLM Gateway"))
            }

            val rawContent = choices.getJSONObject(0).optJSONObject("message")?.optString("content")
                ?: return Result.failure(Exception("Empty message content from LLM"))

            parseToolBundle(rawContent)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    fun parseToolBundle(rawContent: String): Result<DynamicToolBundle> {
        return try {
            val cleanJsonStr = extractJsonString(rawContent)
            val obj = JSONObject(cleanJsonStr)

            val toolId = obj.optString("tool_id", "custom_tool_${System.currentTimeMillis()}").sanitizeId()
            val displayName = obj.optString("display_name", "Generated Custom Tool")
            val description = obj.optString("description", "Dynamic web tool generated via AI")
            val iconName = obj.optString("icon_name", "tool")
            val accentColorHex = obj.optLong("accent_color_hex", 0xFF38BDF8)
            val html = obj.optString("html", "<div class=\"app\">Hello Dynamic Tool</div>")
            val css = obj.optString("css", "body { background: #0B0F19; color: #FFF; padding: 16px; }")
            val js = obj.optString("js", "// Dynamic Tool Script")

            val manifest = DynamicToolManifest(
                toolId = toolId,
                displayName = displayName,
                description = description,
                iconName = iconName,
                accentColorHex = accentColorHex,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            Result.success(DynamicToolBundle(manifest, html, css, js))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to parse tool JSON: ${e.message}\nRaw output: ${rawContent.take(300)}...", e))
        }
    }

    private fun extractJsonString(raw: String): String {
        var text = raw.trim()

        // Handle ```json ... ``` or ``` ... ``` code blocks
        if (text.startsWith("```json")) {
            text = text.removePrefix("```json").trimStart()
        } else if (text.startsWith("```")) {
            text = text.removePrefix("```").trimStart()
        }

        if (text.endsWith("```")) {
            text = text.removeSuffix("```").trimEnd()
        }

        // If surrounded by extra conversational text, locate outermost braces
        val firstBrace = text.indexOf('{')
        val lastBrace = text.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace != -1 && firstBrace < lastBrace) {
            text = text.substring(firstBrace, lastBrace + 1)
        }

        return text
    }

    private fun String.sanitizeId(): String {
        return lowercase().replace("[^a-z0-9_\\-]".toRegex(), "_")
    }
}
