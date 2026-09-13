package dev.motherofallapps.pluginapi

data class ToolInfo(
    val id: ToolId,
    val name: String,
    val description: String,
    val version: String,
    val state: ToolState,
)