package dev.pritam.host.tool.terminal.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.pritam.host.tool.terminal.manager.SavedCommandsRepository
import dev.pritam.host.tool.terminal.manager.TerminalSessionManager
import dev.pritam.host.tool.terminal.model.OutputLineType
import dev.pritam.host.tool.terminal.model.SavedCommand
import dev.pritam.host.tool.terminal.model.TerminalOutputLine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class TerminalViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = TerminalSessionManager(application)
    private val savedCommandsRepo = SavedCommandsRepository(application)

    private val _outputLines = MutableStateFlow<List<TerminalOutputLine>>(emptyList())
    val outputLines: StateFlow<List<TerminalOutputLine>> = _outputLines.asStateFlow()

    val workingDir: StateFlow<File> = sessionManager.workingDir
    val isRunning: StateFlow<Boolean> = sessionManager.isRunning
    val activeCommand: StateFlow<String?> = sessionManager.activeCommand

    val savedCommands: StateFlow<List<SavedCommand>> = savedCommandsRepo.savedCommands

    private val _commandHistory = MutableStateFlow<List<String>>(emptyList())
    val commandHistory: StateFlow<List<String>> = _commandHistory.asStateFlow()

    private var historyCursor = -1

    val inputCommand = MutableStateFlow("")
    val searchFilter = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("All")

    init {
        // Initial welcome banner
        emitWelcomeBanner()
    }

    private fun emitWelcomeBanner() {
        val banner = listOf(
            TerminalOutputLine(
                text = "┌────────────────────────────────────────────────────────┐",
                type = OutputLineType.SYSTEM_INFO
            ),
            TerminalOutputLine(
                text = "│  MOAA CYBER TERMINAL v1.0.0 · LINUX / ANDROID SHELL   │",
                type = OutputLineType.SYSTEM_INFO
            ),
            TerminalOutputLine(
                text = "│  Type 'help' for built-in commands · 'clear' to reset │",
                type = OutputLineType.SYSTEM_INFO
            ),
            TerminalOutputLine(
                text = "│  Save templates with '💾' · Quick run with '⚡ Saved'   │",
                type = OutputLineType.SYSTEM_INFO
            ),
            TerminalOutputLine(
                text = "└────────────────────────────────────────────────────────┘",
                type = OutputLineType.SYSTEM_INFO
            )
        )
        _outputLines.value = banner
    }

    fun execute(overrideCommand: String? = null) {
        val cmd = (overrideCommand ?: inputCommand.value).trim()
        if (cmd.isEmpty()) return

        // If from input box, clear the input box
        if (overrideCommand == null) {
            inputCommand.value = ""
        }

        // Add to history
        val updatedHistory = _commandHistory.value.toMutableList().apply {
            if (isEmpty() || last() != cmd) add(cmd)
        }
        _commandHistory.value = updatedHistory
        historyCursor = updatedHistory.size

        viewModelScope.launch {
            sessionManager.execute(cmd) { line ->
                if (line.text == "__CLEAR_CONSOLE__") {
                    _outputLines.value = emptyList()
                } else {
                    _outputLines.value = _outputLines.value + line
                }
            }
        }
    }

    fun cancel() {
        sessionManager.cancelRunningCommand()
    }

    fun clear() {
        _outputLines.value = emptyList()
    }

    fun setInput(text: String) {
        inputCommand.value = text
    }

    fun appendToken(token: String) {
        val current = inputCommand.value
        if (token == "Tab") {
            inputCommand.value = current + "    "
        } else if (token == "clear") {
            execute("clear")
        } else if (token == "curl") {
            inputCommand.value = if (current.isEmpty()) "curl -s " else "$current curl -s "
        } else {
            inputCommand.value = current + token
        }
    }

    fun navigateHistoryUp() {
        val history = _commandHistory.value
        if (history.isEmpty()) return
        if (historyCursor > 0) {
            historyCursor--
            inputCommand.value = history[historyCursor]
        } else if (historyCursor == 0) {
            inputCommand.value = history[0]
        }
    }

    fun navigateHistoryDown() {
        val history = _commandHistory.value
        if (history.isEmpty()) return
        if (historyCursor < history.size - 1) {
            historyCursor++
            inputCommand.value = history[historyCursor]
        } else {
            historyCursor = history.size
            inputCommand.value = ""
        }
    }

    fun saveCommand(
        name: String,
        command: String,
        category: String = "General",
        description: String = "",
        isFavorite: Boolean = false
    ) {
        savedCommandsRepo.saveCommand(name, command, category, description, isFavorite)
    }

    fun deleteSavedCommand(id: String) {
        savedCommandsRepo.deleteCommand(id)
    }

    fun toggleFavorite(id: String) {
        savedCommandsRepo.toggleFavorite(id)
    }

    fun resetSavedCommandsToDefaults() {
        savedCommandsRepo.resetToDefaults()
    }

    fun getShortWorkingDir(): String {
        return sessionManager.getShortWorkingDir()
    }

    fun getAllOutputAsText(): String {
        return _outputLines.value.joinToString("\n") { it.text }
    }
}
