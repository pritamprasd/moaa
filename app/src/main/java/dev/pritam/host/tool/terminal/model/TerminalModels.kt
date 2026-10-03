package dev.pritam.host.tool.terminal.model

import java.util.UUID

enum class OutputLineType {
    COMMAND,
    STDOUT,
    STDERR,
    SYSTEM_INFO,
    SUCCESS,
    ERROR
}

data class TerminalOutputLine(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val type: OutputLineType,
    val timestamp: Long = System.currentTimeMillis()
)

data class SavedCommand(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val command: String,
    val category: String = "General",
    val description: String = "",
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class CommandExecutionResult(
    val command: String,
    val exitCode: Int,
    val executionTimeMs: Long,
    val linesCount: Int
)
