package dev.pritam.host.host

import dev.pritam.pluginapi.ToolId
import dev.pritam.pluginapi.ToolInfo
import dev.pritam.pluginapi.ToolState
import org.junit.Assert.assertEquals
import org.junit.Test

class ToolOrderingTest {

    private fun createMockTool(id: String, name: String) = ToolInfo(
        id = ToolId(id),
        name = name,
        description = "Test Description",
        version = "1.0.0",
        state = ToolState.INSTALLED
    )

    @Test
    fun testApplyOrderingEmptyOrder() {
        val tools = listOf(
            createMockTool("ftp-server", "FTP Server"),
            createMockTool("terminal", "Terminal"),
            createMockTool("net-topology", "Network Topology")
        )
        val ordered = HostAppState.applyOrdering(tools, emptyList())
        assertEquals(tools.map { it.id.value }, ordered.map { it.id.value })
    }

    @Test
    fun testApplyOrderingCustomOrder() {
        val tools = listOf(
            createMockTool("ftp-server", "FTP Server"),
            createMockTool("terminal", "Terminal"),
            createMockTool("net-topology", "Network Topology"),
            createMockTool("sensors", "Sensors")
        )
        val order = listOf("net-topology", "sensors", "ftp-server")
        val ordered = HostAppState.applyOrdering(tools, order)
        
        // Custom-ordered tools come first in specified order, unmentioned tools come afterwards preserving relative order
        assertEquals(
            listOf("net-topology", "sensors", "ftp-server", "terminal"),
            ordered.map { it.id.value }
        )
    }

    @Test
    fun testApplyOrderingWithDynamicTools() {
        val tools = listOf(
            createMockTool("ftp-server", "FTP Server"),
            createMockTool("terminal", "Terminal"),
            createMockTool("dynamic_tool_1", "Custom Tool 1"),
            createMockTool("dynamic_tool_2", "Custom Tool 2")
        )
        val order = listOf("dynamic_tool_2", "terminal")
        val ordered = HostAppState.applyOrdering(tools, order)

        assertEquals(
            listOf("dynamic_tool_2", "terminal", "ftp-server", "dynamic_tool_1"),
            ordered.map { it.id.value }
        )
    }
}
