package dev.pritam.dynamictools.engine

import dev.pritam.dynamictools.model.DynamicToolBundle

object DynamicToolPromptBuilder {

    fun buildSystemPrompt(): String {
        return """
            You are the Master Web Tool Engineer for "MotherOfAllApps" (MOAA), an elite Android utility suite.
            Your task is to generate complete, single-page, responsive, interactive web tools using HTML, CSS, and pure JavaScript based on user natural language prompts.

            ### OUTPUT FORMAT (CRITICAL):
            You MUST reply ONLY with a raw, valid JSON object without any introductory or concluding markdown conversational text.
            The JSON object MUST follow this EXACT schema:
            {
              "tool_id": "alphanumeric_snake_case_id",
              "display_name": "Human Readable Title (e.g. Scientific Calculator)",
              "description": "Clear 1-2 sentence description of what the tool does.",
              "icon_name": "calculator | convert | code | timer | terminal | chart | tool",
              "accent_color_hex": 3816782712,
              "html": "<div class=\"app-container\">...inner HTML elements only (no head/body tags required)...</div>",
              "css": ":root { --bg: #0B0F19; --card: #1E293B; --cyan: #38BDF8; ... } * { box-sizing: border-box; } body { font-family: system-ui, -apple-system, sans-serif; background: #0B0F19; color: #F8FAFC; padding: 12px; } ...",
              "js": "// Pure JavaScript logic for event listeners, state, computations, and DOM updates\nfunction init() { ... }\nwindow.onload = init;"
            }

            ### DESIGN & STYLING RULES:
            1. **Palette & Theme**: Must use rich, modern dark-mode glassmorphism matching MOAA:
               - Background: `#0B0F19` / Deep Space `#030712`
               - Card / Surface: `rgba(30, 41, 59, 0.7)` with `backdrop-filter: blur(12px)` and subtle `1px solid rgba(148, 163, 184, 0.15)` borders
               - Accents: Neon Cyan `#38BDF8`, Violet `#A78BFA`, Rose `#F472B6`, Emerald `#34D399`, Amber `#F59E0B`
               - Typography: `system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif` for UI, `monospace` for numbers/code.
            2. **Mobile-First UX**:
               - Touch targets must be at least 44px height with active feedback (`transform: scale(0.96); filter: brightness(1.2);`).
               - Full viewport height (`height: 100vh; overflow-y: auto;`).
               - Zero horizontal scrollbars (`max-width: 600px; margin: 0 auto; width: 100%;`).
            3. **Native Android Bridge Capabilities**:
               You can interact with native Android APIs via `window.AndroidBridge`:
               - `window.AndroidBridge.log(message)`: Broadcasts diagnostic info to System Logs.
               - `window.AndroidBridge.copyToClipboard(text)`: Copies text to the Android clipboard.
               - `window.AndroidBridge.showToast(message)`: Displays native Android toast.
               - `window.AndroidBridge.vibrate(milliseconds)`: Triggers tactile haptic feedback (e.g. 20ms for button taps, 300ms for alerts).
               - `window.AndroidBridge.getDeviceInfo()`: Returns JSON string with device model and battery info.
               *Always check `if (window.AndroidBridge && window.AndroidBridge.methodName) { ... }` before invoking.*
            4. **Self-Contained**:
               - DO NOT use external CDN scripts or remote stylesheet links (no remote jQuery/Bootstrap).
               - Use pure Vanilla ES6+ JavaScript and modern CSS (flexbox, CSS grid).
               - Add rich interactive capabilities (instant feedback, copy buttons, clear buttons, calculation history, live preview).
        """.trimIndent()
    }

    fun buildUserPrompt(userRequest: String): String {
        return """
            Generate a complete, dynamic web tool for the following user request:
            "$userRequest"

            Ensure the generated tool is intuitive, aesthetically stunning with dark glass styling, and fully operational with all calculations, handlers, and edge cases implemented.
            Output ONLY valid JSON matching the specified schema.
        """.trimIndent()
    }

    fun buildRefinePrompt(existingBundle: DynamicToolBundle, refinementRequest: String): String {
        return """
            Refine and update the existing dynamic web tool based on this request:
            "$refinementRequest"

            ### Existing Tool Manifest:
            ${existingBundle.manifest.toJson().toString(2)}

            ### Existing HTML:
            ```html
            ${existingBundle.html}
            ```

            ### Existing CSS:
            ```css
            ${existingBundle.css}
            ```

            ### Existing JS:
            ```javascript
            ${existingBundle.js}
            ```

            Return the COMPLETE updated tool in the required JSON schema, preserving working functionality while adding the requested improvements.
            Output ONLY the valid JSON object.
        """.trimIndent()
    }
}
