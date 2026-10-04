package dev.pritam.host.settings.model

/**
 * Represents an open-source dependency or framework used within the Mother of All Apps system.
 * Updated in each development iteration to maintain transparency and compliance.
 */
data class OpenSourceLibrary(
    val name: String,
    val version: String,
    val license: String,
    val category: String,
    val description: String,
    val projectUrl: String = ""
)

object OpenSourceRegistry {
    val libraries: List<OpenSourceLibrary> = listOf(
        OpenSourceLibrary(
            name = "Jetpack Compose UI & Foundation",
            version = "BOM 2026.09.00",
            license = "Apache-2.0",
            category = "UI Framework",
            description = "Declarative Android UI toolkit, hardware-accelerated canvas rendering, layout system, and input dispatch.",
            projectUrl = "https://developer.android.com/jetpack/compose"
        ),
        OpenSourceLibrary(
            name = "Material 3 (Compose)",
            version = "BOM 2026.09.00",
            license = "Apache-2.0",
            category = "Design System",
            description = "Material Design 3 components, color palettes, elevated surfaces, typography tokens, and theming.",
            projectUrl = "https://m3.material.io"
        ),
        OpenSourceLibrary(
            name = "AndroidX Material Icons Extended",
            version = "BOM 2026.09.00",
            license = "Apache-2.0",
            category = "Icons & Glyphs",
            description = "Vector drawable iconography for system tools, hardware sensors, networking, and developer diagnostics.",
            projectUrl = "https://fonts.google.com/icons"
        ),
        OpenSourceLibrary(
            name = "Kotlinx Coroutines Android",
            version = "1.11.0",
            license = "Apache-2.0",
            category = "Concurrency",
            description = "Asynchronous non-blocking concurrency, structured concurrency scopes, Flow telemetry streaming, and dispatchers.",
            projectUrl = "https://github.com/Kotlin/kotlinx.coroutines"
        ),
        OpenSourceLibrary(
            name = "AndroidX Navigation Compose",
            version = "2.10.1",
            license = "Apache-2.0",
            category = "Architecture",
            description = "Single-activity navigation graph routing, deep-linking, animated screen transitions, and backstack management.",
            projectUrl = "https://developer.android.com/guide/navigation"
        ),
        OpenSourceLibrary(
            name = "AndroidX Lifecycle & Activity Compose",
            version = "2.11.0 / 1.13.0",
            license = "Apache-2.0",
            category = "Architecture",
            description = "Lifecycle-aware state collection (collectAsStateWithLifecycle), ViewModel providers, and activity composition.",
            projectUrl = "https://developer.android.com/jetpack/androidx/releases/lifecycle"
        ),
        OpenSourceLibrary(
            name = "Kotlin Standard Library & Compiler",
            version = "2.4.20",
            license = "Apache-2.0",
            category = "Core Language",
            description = "First-party Kotlin programming language standard library, collections, reflection, and JVM target compilation.",
            projectUrl = "https://kotlinlang.org"
        ),
        OpenSourceLibrary(
            name = "JSON in Java (org.json)",
            version = "20240303",
            license = "JSON License",
            category = "Data Parsing",
            description = "Strict JSON object and array parsing for Model Context Protocol (MCP) schemas and LLM Gateway streaming payloads.",
            projectUrl = "https://github.com/stleary/JSON-java"
        ),
        OpenSourceLibrary(
            name = "JUnit 4",
            version = "4.13.2",
            license = "EPL-1.0",
            category = "Testing",
            description = "Unit test runner framework for sensor telemetry calculations, settings state flow, and protocol validation.",
            projectUrl = "https://junit.org/junit4/"
        ),
        OpenSourceLibrary(
            name = "Android Open Source Project (AOSP)",
            version = "API 34..37",
            license = "Apache-2.0",
            category = "Operating System",
            description = "Hardware abstraction layer, SensorManager subsystem, Wi-Fi P2P Wi-Fi Direct, Camera2, and AudioRecord interfaces.",
            projectUrl = "https://source.android.com"
        )
    )
}
