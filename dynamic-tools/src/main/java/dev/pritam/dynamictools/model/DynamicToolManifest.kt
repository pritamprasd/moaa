package dev.pritam.dynamictools.model

import org.json.JSONObject

/**
 * Metadata for a dynamic web-based tool.
 */
data class DynamicToolManifest(
    val toolId: String,
    val displayName: String,
    val description: String,
    val iconName: String = "tool",
    val version: String = "1.0.0",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val accentColorHex: Long = 0xFF38BDF8, // Default Sky Cyan
    val author: String = "Dynamic AI Generator",
    val entryPoint: String = "index.html",
    val tags: List<String> = emptyList()
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("tool_id", toolId)
            put("display_name", displayName)
            put("description", description)
            put("icon_name", iconName)
            put("version", version)
            put("created_at", createdAt)
            put("updated_at", updatedAt)
            put("accent_color_hex", accentColorHex)
            put("author", author)
            put("entry_point", entryPoint)
            put("tags", org.json.JSONArray(tags))
        }
    }

    companion object {
        fun fromJson(json: JSONObject): DynamicToolManifest {
            val tagsList = mutableListOf<String>()
            val tagsArray = json.optJSONArray("tags")
            if (tagsArray != null) {
                for (i in 0 until tagsArray.length()) {
                    tagsList.add(tagsArray.getString(i))
                }
            }

            return DynamicToolManifest(
                toolId = json.optString("tool_id", "custom_tool_${System.currentTimeMillis()}"),
                displayName = json.optString("display_name", "Custom Web Tool"),
                description = json.optString("description", "Dynamic runtime tool"),
                iconName = json.optString("icon_name", "tool"),
                version = json.optString("version", "1.0.0"),
                createdAt = json.optLong("created_at", System.currentTimeMillis()),
                updatedAt = json.optLong("updated_at", System.currentTimeMillis()),
                accentColorHex = json.optLong("accent_color_hex", 0xFF38BDF8),
                author = json.optString("author", "Dynamic AI Generator"),
                entryPoint = json.optString("entry_point", "index.html"),
                tags = tagsList
            )
        }
    }
}

/**
 * Complete bundle containing manifest and files.
 */
data class DynamicToolBundle(
    val manifest: DynamicToolManifest,
    val html: String,
    val css: String,
    val js: String,
    val rootDirectoryPath: String = ""
) {
    /**
     * Builds the self-contained HTML bundle with inlined CSS and JS
     * for robust offline rendering and instant loading.
     */
    fun buildInlinedHtml(): String {
        return buildString {
            append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n")
            append("  <meta charset=\"UTF-8\">\n")
            append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no\">\n")
            append("  <title>${escapeHtml(manifest.displayName)}</title>\n")
            append("  <style>\n")
            append("""
                :root {
                  --bg-root: #090a0e;
                  --surface-base: #121319;
                  --surface-elevated: #181a22;
                  --surface-active: #202330;
                  --border-default: #222531;
                  --border-subtle: #181b24;
                  --text-primary: #f4f4f6;
                  --text-secondary: #9ca3af;
                  --text-muted: #64748b;
                  --accent-primary: #38bdf8;
                  --accent-secondary: #818cf8;
                  --accent-tertiary: #34d399;
                  --status-success: #10b981;
                  --status-warning: #f59e0b;
                  --status-error: #ef4444;
                  --font-sans: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                  --font-mono: ui-monospace, SFMono-Regular, "JetBrains Mono", Menlo, Consolas, monospace;
                }
                * { box-sizing: border-box; }
                body {
                  margin: 0;
                  padding: 14px;
                  background-color: var(--bg-root);
                  color: var(--text-primary);
                  font-family: var(--font-sans);
                  -webkit-font-smoothing: antialiased;
                }
            """.trimIndent())
            append("\n")
            append(css)
            append("\n  </style>\n")
            append("</head>\n<body>\n")
            // If the HTML snippet already contains full <html>/<body> tags, extract inner body
            val cleanHtml = extractInnerHtml(html)
            append(cleanHtml)
            append("\n  <script>\n")
            append(js)
            append("\n  </script>\n")
            append("</body>\n</html>")
        }
    }

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
    }

    private fun extractInnerHtml(raw: String): String {
        val lower = raw.lowercase()
        if (lower.contains("<body")) {
            val bodyStart = raw.indexOf(">", lower.indexOf("<body")) + 1
            val bodyEnd = raw.lastIndexOf("</body>", ignoreCase = true)
            if (bodyStart in 1 until bodyEnd) {
                return raw.substring(bodyStart, bodyEnd).trim()
            }
        }
        return raw.trim()
    }
}
