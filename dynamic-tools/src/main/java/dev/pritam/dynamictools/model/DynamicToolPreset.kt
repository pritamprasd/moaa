package dev.pritam.dynamictools.model

/**
 * Curated presets for dynamic web tools.
 */
data class DynamicToolPreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val prompt: String,
    val iconName: String,
    val accentColorHex: Long
) {
    companion object {
        val PRESETS = listOf(
            DynamicToolPreset(
                id = "scientific_calculator",
                title = "Scientific Calculator",
                subtitle = "Trigonometry, logarithms, power, and calculation tape",
                prompt = "Build a sleek scientific calculator with responsive touch keys, display tape history, trigonometrics (sin, cos, tan, rad/deg), sqrt, powers, factorials, and an Android clipboard copy button using AndroidBridge.copyToClipboard().",
                iconName = "calculator",
                accentColorHex = 0xFF38BDF8 // Cyan
            ),
            DynamicToolPreset(
                id = "unit_converter",
                title = "Unit Converter",
                subtitle = "Length, weight, temperature, data storage, and speed",
                prompt = "Build a fast multi-category unit converter supporting Length (m, km, ft, in, mi), Weight (kg, g, lb, oz), Temperature (C, F, K), Data (B, KB, MB, GB, TB), and Speed (km/h, mph, m/s). Include instant bidirectional conversion and swap button.",
                iconName = "convert",
                accentColorHex = 0xFF34D399 // Emerald
            ),
            DynamicToolPreset(
                id = "regex_tester",
                title = "Regex & Text Lab",
                subtitle = "Live regex matching, capture groups, and text transformations",
                prompt = "Build an interactive Regex Tester with real-time matching highlights, capture group explorer, match counter, flags toggles (g, i, m, s), string replacer, and preset common patterns (email, URL, IP address, UUID, phone).",
                iconName = "code",
                accentColorHex = 0xFFA78BFA // Violet
            ),
            DynamicToolPreset(
                id = "pomodoro_timer",
                title = "Focus Pomodoro Timer",
                subtitle = "Work/break intervals, ambient progress circle, and haptics",
                prompt = "Build a Cyberpunk Pomodoro Focus Timer with circular SVG countdown progress, configurable Work (25m), Short Break (5m), and Long Break (15m) sessions, sound/haptic alert on finish using AndroidBridge.vibrate(500), and completed session counter.",
                iconName = "timer",
                accentColorHex = 0xFFF472B6 // Rose
            ),
            DynamicToolPreset(
                id = "json_formatter",
                title = "JSON Formatter & Validator",
                subtitle = "Prettify, minify, tree view, and schema validator",
                prompt = "Build a JSON Formatter & Validator tool with syntax highlighting, collapsible nested tree view, error pinpointing with line numbers, minify/prettify toggle, and one-tap copy to clipboard using AndroidBridge.copyToClipboard().",
                iconName = "terminal",
                accentColorHex = 0xFFF59E0B // Amber
            ),
            DynamicToolPreset(
                id = "crypto_hash_lab",
                title = "Base64 & Hash Studio",
                subtitle = "Base64 encode/decode, SHA-256, MD5, and URL encoding",
                prompt = "Build a Base64, Hex, and Hash Crypto Studio with live text encoder/decoder for Base64, URL encoding, Hex bytes, and SHA-256 / SHA-1 / MD5 hash generation with instant copy buttons.",
                iconName = "chart",
                accentColorHex = 0xFF818CF8 // Indigo
            )
        )
    }
}
