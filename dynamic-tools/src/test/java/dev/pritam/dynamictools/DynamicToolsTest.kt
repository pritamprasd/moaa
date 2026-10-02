package dev.pritam.dynamictools

import dev.pritam.dynamictools.engine.DynamicToolLlmClient
import dev.pritam.dynamictools.model.DynamicToolBundle
import dev.pritam.dynamictools.model.DynamicToolManifest
import dev.pritam.dynamictools.storage.DefaultToolsSeed
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DynamicToolsTest {

    @Test
    fun testManifestJsonSerialization() {
        val manifest = DynamicToolManifest(
            toolId = "unit_converter",
            displayName = "Universal Unit Converter",
            description = "Convert Length, Weight, Temperature, and Speed.",
            iconName = "convert",
            version = "1.2.0",
            createdAt = 1000L,
            updatedAt = 2000L,
            accentColorHex = 0xFF34D399,
            author = "AI Generator",
            tags = listOf("Converter", "Math")
        )

        val json = manifest.toJson()
        val restored = DynamicToolManifest.fromJson(json)

        assertEquals("unit_converter", restored.toolId)
        assertEquals("Universal Unit Converter", restored.displayName)
        assertEquals("Convert Length, Weight, Temperature, and Speed.", restored.description)
        assertEquals("convert", restored.iconName)
        assertEquals("1.2.0", restored.version)
        assertEquals(1000L, restored.createdAt)
        assertEquals(2000L, restored.updatedAt)
        assertEquals(0xFF34D399, restored.accentColorHex)
        assertEquals(2, restored.tags.size)
        assertEquals("Converter", restored.tags[0])
    }

    @Test
    fun testBundleHtmlInlining() {
        val manifest = DynamicToolManifest(
            toolId = "test_tool",
            displayName = "Test App",
            description = "Test description"
        )
        val bundle = DynamicToolBundle(
            manifest = manifest,
            html = "<div id=\"root\"><h1>Hello Test</h1></div>",
            css = "body { background: #0B0F19; color: #FFF; }",
            js = "console.log('App started');"
        )

        val inlined = bundle.buildInlinedHtml()
        assertTrue(inlined.contains("<!DOCTYPE html>"))
        assertTrue(inlined.contains("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0"))
        assertTrue(inlined.contains("body { background: #0B0F19; color: #FFF; }"))
        assertTrue(inlined.contains("<div id=\"root\"><h1>Hello Test</h1></div>"))
        assertTrue(inlined.contains("console.log('App started');"))
    }

    @Test
    fun testLlmClientJsonParsingWithMarkdownFences() {
        val client = DynamicToolLlmClient()
        val rawLlmResponse = """
            Here is your requested tool:
            ```json
            {
              "tool_id": "crypto_hasher",
              "display_name": "Crypto Hash Lab",
              "description": "Compute SHA-256 and MD5 hashes.",
              "icon_name": "chart",
              "accent_color_hex": 4287114674,
              "html": "<div class=\"hasher\">Hash App</div>",
              "css": "body { background: #000; }",
              "js": "function hash() { return 1; }"
            }
            ```
            Hope this helps!
        """.trimIndent()

        val result = client.parseToolBundle(rawLlmResponse)
        assertTrue(result.isSuccess)
        val bundle = result.getOrNull()
        assertNotNull(bundle)
        assertEquals("crypto_hasher", bundle!!.manifest.toolId)
        assertEquals("Crypto Hash Lab", bundle.manifest.displayName)
        assertEquals("<div class=\"hasher\">Hash App</div>", bundle.html)
        assertEquals("body { background: #000; }", bundle.css)
        assertEquals("function hash() { return 1; }", bundle.js)
    }

    @Test
    fun testDefaultToolsSeed() {
        val seeds = DefaultToolsSeed.getSeedTools()
        assertTrue(seeds.size >= 2)
        val calc = seeds.first { it.manifest.toolId == "scientific_calculator" }
        assertEquals("Cyber Calculator", calc.manifest.displayName)
        assertTrue(calc.html.contains("calculator-app"))
        assertTrue(calc.js.contains("calculate()"))

        val regex = seeds.first { it.manifest.toolId == "regex_text_lab" }
        assertEquals("Regex & Text Studio", regex.manifest.displayName)
        assertTrue(regex.html.contains("regex-app"))
        assertTrue(regex.js.contains("testRegex()"))
    }
}
