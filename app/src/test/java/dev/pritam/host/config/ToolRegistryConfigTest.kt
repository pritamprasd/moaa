package dev.pritam.host.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolRegistryConfigTest {

    @Test
    fun testYamlToolConfigParserDirectString() {
        val sampleYaml = """
            version: "0.15.0"
            tools:
              - id: "test-tool"
                name: "Test Tool"
                short_tagline: "A test tool tagline"
                description: "Full description of test tool."
                version: "2.1.0"
                category: "Testing"
                author: "Test Author"
                icon_type: "terminal"
                emoji: "🧪"
                accent_color: "0xFF10B981"
                required_permissions:
                  - "INTERNET"
                  - "ACCESS_NETWORK_STATE"
                enabled: true
        """.trimIndent()

        val parsed = YamlToolConfigParser.parse(sampleYaml)
        assertEquals(1, parsed.size)

        val tool = parsed[0]
        assertEquals("test-tool", tool.id)
        assertEquals("Test Tool", tool.name)
        assertEquals("A test tool tagline", tool.shortTagline)
        assertEquals("Full description of test tool.", tool.description)
        assertEquals("2.1.0", tool.version)
        assertEquals("Testing", tool.category)
        assertEquals("Test Author", tool.author)
        assertEquals("terminal", tool.iconType)
        assertEquals("🧪", tool.emoji)
        assertEquals(0xFF10B981, tool.accentColorHex)
        assertEquals(listOf("INTERNET", "ACCESS_NETWORK_STATE"), tool.requiredPermissions)
        assertTrue(tool.isEnabled)
    }

    @Test
    fun testToolRegistryConfigLoadsAllToolsFromSingleSourceYaml() {
        val tools = ToolRegistryConfig.INSTALLED_TOOLS
        assertTrue("Expected at least 13 tools loaded from config.yaml, got ${tools.size}", tools.size >= 13)

        val expectedIds = listOf(
            "ftp-server",
            "nfc-tool",
            "ftp-client",
            "log-viewer",
            "sensors",
            "llm-gateway",
            "llm-chat",
            "dynamic-tools-studio",
            "system-manual",
            "ghost-agent",
            "terminal",
            "system-info",
            "net-topology"
        )

        for (id in expectedIds) {
            val def = ToolRegistryConfig.findToolDefinition(id)
            assertNotNull("Tool with id '$id' should be found in ToolRegistryConfig", def)
            assertTrue("Tool '$id' should have a non-blank name", def!!.name.isNotBlank())
            assertTrue("Tool '$id' should have a non-blank shortTagline", def.shortTagline.isNotBlank())
            assertTrue("Tool '$id' should have a non-blank description", def.description.isNotBlank())
            assertTrue("Tool '$id' should have a non-blank emoji", def.emoji.isNotBlank())
        }
    }

    @Test
    fun testDynamicToolResolution() {
        val dynamicDef = ToolRegistryConfig.findToolDefinition("dynamic_custom_calculator")
        assertNotNull(dynamicDef)
        assertEquals("Custom Calculator", dynamicDef!!.name)
        assertEquals("dynamic-tool", dynamicDef.iconType)
        assertEquals("⚡", dynamicDef.emoji)
    }
}
