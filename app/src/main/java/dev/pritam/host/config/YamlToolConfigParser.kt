package dev.pritam.host.config

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Lightweight, zero-dependency YAML parser specifically designed to parse
 * `config.yaml` into [ToolDefinition] records.
 *
 * Supports single source of truth configuration for tool IDs, names,
 * descriptions, short taglines, icons, emojis, accent colors, and permissions.
 */
object YamlToolConfigParser {

    fun parse(inputStream: InputStream): List<ToolDefinition> {
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        return parse(reader.readText())
    }

    fun parse(yamlContent: String): List<ToolDefinition> {
        val tools = mutableListOf<ToolDefinition>()
        val lines = yamlContent.lines()

        var currentToolMap = mutableMapOf<String, Any>()
        var currentListKey: String? = null
        var currentListValues = mutableListOf<String>()
        var inToolsSection = false

        fun commitCurrentTool() {
            if (currentToolMap.isNotEmpty()) {
                if (currentListKey != null) {
                    currentToolMap[currentListKey!!] = currentListValues.toList()
                    currentListKey = null
                    currentListValues = mutableListOf()
                }

                val id = currentToolMap["id"] as? String ?: return
                val name = currentToolMap["name"] as? String ?: id
                val shortTagline = currentToolMap["short_tagline"] as? String ?: ""
                val description = currentToolMap["description"] as? String ?: ""
                val version = currentToolMap["version"] as? String ?: "1.0.0"
                val category = currentToolMap["category"] as? String ?: "General"
                val author = currentToolMap["author"] as? String ?: "MotherOfAllApps Core"
                val iconType = currentToolMap["icon_type"] as? String ?: "generic"
                val emoji = currentToolMap["emoji"] as? String ?: "🔧"
                val accentHexStr = currentToolMap["accent_color"] as? String ?: "0xFF38BDF8"
                val accentColorHex = parseHexColor(accentHexStr)
                @Suppress("UNCHECKED_CAST")
                val permissions = (currentToolMap["required_permissions"] as? List<String>) ?: emptyList()
                val isEnabled = (currentToolMap["enabled"] as? Boolean) ?: true

                tools.add(
                    ToolDefinition(
                        id = id,
                        name = name,
                        shortTagline = shortTagline,
                        description = description,
                        version = version,
                        category = category,
                        author = author,
                        iconType = iconType,
                        emoji = emoji,
                        accentColorHex = accentColorHex,
                        requiredPermissions = permissions,
                        isEnabled = isEnabled
                    )
                )
                currentToolMap = mutableMapOf()
            }
        }

        for (rawLine in lines) {
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue
            }

            if (trimmed == "tools:") {
                inToolsSection = true
                continue
            }

            if (!inToolsSection) {
                continue
            }

            // Indented list element under a list key (e.g. - "INTERNET")
            if (currentListKey != null && trimmed.startsWith("-") && !trimmed.removePrefix("-").trim().contains(":")) {
                val value = stripQuotes(trimmed.removePrefix("-").trim())
                currentListValues.add(value)
                continue
            }

            // Detect new tool list entry, e.g. "- id: \"ftp-server\""
            if (trimmed.startsWith("- ")) {
                commitCurrentTool()
                val itemContent = trimmed.removePrefix("- ").trim()
                if (itemContent.contains(":")) {
                    val (key, value) = parseKeyValue(itemContent)
                    currentToolMap[key] = value
                }
                continue
            }

            // Normal key-value property: "key: value"
            if (trimmed.contains(":")) {
                if (currentListKey != null) {
                    currentToolMap[currentListKey!!] = currentListValues.toList()
                    currentListKey = null
                    currentListValues = mutableListOf()
                }

                val (key, rawValue) = parseKeyValue(trimmed)
                if (rawValue.toString().isEmpty()) {
                    // Possible start of a list like "required_permissions:"
                    currentListKey = key
                    currentListValues = mutableListOf()
                } else {
                    currentToolMap[key] = rawValue
                }
            }
        }

        // Commit the final tool definition
        commitCurrentTool()

        return tools
    }

    private fun parseKeyValue(line: String): Pair<String, Any> {
        val colonIndex = line.indexOf(':')
        val key = line.substring(0, colonIndex).trim()
        val rawValue = line.substring(colonIndex + 1).trim()
        val cleanedValue = stripQuotes(rawValue)

        val parsedValue: Any = when {
            cleanedValue.equals("true", ignoreCase = true) -> true
            cleanedValue.equals("false", ignoreCase = true) -> false
            else -> cleanedValue
        }
        return key to parsedValue
    }

    private fun stripQuotes(input: String): String {
        var str = input.trim()
        if ((str.startsWith("\"") && str.endsWith("\"")) || (str.startsWith("'") && str.endsWith("'"))) {
            if (str.length >= 2) {
                str = str.substring(1, str.length - 1)
            }
        }
        return str
    }

    private fun parseHexColor(hexStr: String): Long {
        val cleaned = stripQuotes(hexStr).trim()
        return try {
            when {
                cleaned.startsWith("0x", ignoreCase = true) -> java.lang.Long.decode(cleaned)
                cleaned.startsWith("#") -> cleaned.removePrefix("#").toLong(16)
                else -> cleaned.toLong(16)
            }
        } catch (_: Exception) {
            0xFF38BDF8 // Default Cyan
        }
    }
}
