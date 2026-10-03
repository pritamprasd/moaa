package dev.pritam.host.tool.terminal.manager

import android.content.Context
import android.os.Environment
import dev.pritam.host.logging.AppLogHub
import dev.pritam.host.logging.LogLevel
import dev.pritam.host.tool.terminal.model.CommandExecutionResult
import dev.pritam.host.tool.terminal.model.OutputLineType
import dev.pritam.host.tool.terminal.model.TerminalOutputLine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.atomic.AtomicReference

class TerminalSessionManager(private val context: Context) {

    private val _workingDir = MutableStateFlow<File>(resolveInitialDirectory())
    val workingDir: StateFlow<File> = _workingDir.asStateFlow()

    private var previousDir: File = _workingDir.value

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _activeCommand = MutableStateFlow<String?>(null)
    val activeCommand: StateFlow<String?> = _activeCommand.asStateFlow()

    private val currentProcess = AtomicReference<Process?>(null)
    private var executionJob: Job? = null

    private val environmentVariables = mutableMapOf<String, String>().apply {
        val defaultPath = buildString {
            append("/product/bin:")
            append("/apex/com.android.runtime/bin:")
            append("/apex/com.android.art/bin:")
            append("/system_ext/bin:")
            append("/system/bin:")
            append("/system/xbin:")
            append("/odm/bin:")
            append("/vendor/bin:")
            append("/vendor/xbin:")
            append("/data/local/tmp:")
            append(System.getenv("PATH") ?: "")
        }
        put("PATH", defaultPath)
        put("HOME", context.filesDir.absolutePath)
        put("TERM", "xterm-256color")
        put("SHELL", "/system/bin/sh")
        put("USER", "u0_moaa")
        put("TMPDIR", context.cacheDir.absolutePath)
    }

    private val commandHistory = mutableListOf<String>()

    private fun resolveInitialDirectory(): File {
        return try {
            val external = Environment.getExternalStorageDirectory()
            if (external != null && external.exists() && external.canRead()) {
                external
            } else {
                context.filesDir
            }
        } catch (e: Exception) {
            context.filesDir
        }
    }

    suspend fun execute(
        rawCommand: String,
        onLine: (TerminalOutputLine) -> Unit
    ): CommandExecutionResult = withContext(Dispatchers.IO) {
        val trimmed = rawCommand.trim()
        if (trimmed.isEmpty()) {
            return@withContext CommandExecutionResult(rawCommand, 0, 0, 0)
        }

        commandHistory.add(trimmed)
        _isRunning.value = true
        _activeCommand.value = trimmed

        val startTime = System.currentTimeMillis()
        var lineCount = 0

        // Emit prompt line
        val promptPrefix = "${getShortWorkingDir()} $ "
        onLine(
            TerminalOutputLine(
                text = promptPrefix + trimmed,
                type = OutputLineType.COMMAND
            )
        )

        try {
            // Check for built-in command handlers
            val handledBuiltIn = handleBuiltInCommand(trimmed, onLine)
            if (handledBuiltIn != null) {
                val duration = System.currentTimeMillis() - startTime
                _isRunning.value = false
                _activeCommand.value = null
                return@withContext CommandExecutionResult(trimmed, handledBuiltIn, duration, lineCount)
            }

            // Shell execution
            val processBuilder = ProcessBuilder("/system/bin/sh", "-c", trimmed)
                .directory(_workingDir.value)

            val env = processBuilder.environment()
            env.putAll(environmentVariables)
            env["PWD"] = _workingDir.value.absolutePath

            val process = processBuilder.start()
            currentProcess.set(process)

            AppLogHub.log(
                toolId = "terminal",
                toolName = "Terminal",
                level = LogLevel.INFO,
                tag = "Shell",
                message = "EXEC: $trimmed [dir: ${_workingDir.value.absolutePath}]"
            )

            // Concurrently capture stdout and stderr
            val stdoutReader = BufferedReader(InputStreamReader(process.inputStream))
            val stderrReader = BufferedReader(InputStreamReader(process.errorStream))

            val stdoutJob = launch {
                var line: String?
                while (stdoutReader.readLine().also { line = it } != null) {
                    line?.let {
                        lineCount++
                        onLine(TerminalOutputLine(text = it, type = OutputLineType.STDOUT))
                    }
                }
            }

            val stderrJob = launch {
                var errLine: String?
                while (stderrReader.readLine().also { errLine = it } != null) {
                    errLine?.let {
                        lineCount++
                        onLine(TerminalOutputLine(text = it, type = OutputLineType.STDERR))
                    }
                }
            }

            val exitCode = process.waitFor()
            stdoutJob.join()
            stderrJob.join()

            val duration = System.currentTimeMillis() - startTime
            val statusType = if (exitCode == 0) OutputLineType.SUCCESS else OutputLineType.ERROR
            onLine(
                TerminalOutputLine(
                    text = "[Exit code $exitCode | ${duration}ms]",
                    type = statusType
                )
            )

            AppLogHub.log(
                toolId = "terminal",
                toolName = "Terminal",
                level = if (exitCode == 0) LogLevel.INFO else LogLevel.WARN,
                tag = "Shell",
                message = "FINISHED: $trimmed -> exitCode $exitCode in ${duration}ms"
            )

            CommandExecutionResult(trimmed, exitCode, duration, lineCount)
        } catch (e: CancellationException) {
            currentProcess.get()?.destroy()
            val duration = System.currentTimeMillis() - startTime
            onLine(
                TerminalOutputLine(
                    text = "[Process aborted by user]",
                    type = OutputLineType.SYSTEM_INFO
                )
            )
            CommandExecutionResult(trimmed, 130, duration, lineCount)
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            onLine(
                TerminalOutputLine(
                    text = "sh: error: ${e.message ?: e.javaClass.simpleName}",
                    type = OutputLineType.ERROR
                )
            )
            CommandExecutionResult(trimmed, -1, duration, lineCount)
        } finally {
            currentProcess.set(null)
            _isRunning.value = false
            _activeCommand.value = null
        }
    }

    private fun handleBuiltInCommand(
        cmd: String,
        onLine: (TerminalOutputLine) -> Unit
    ): Int? {
        val parts = cmd.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        if (parts.isEmpty()) return 0

        val commandName = parts[0]

        when (commandName) {
            "cd" -> {
                val target = if (parts.size > 1) parts[1] else "~"
                return executeCd(target, onLine)
            }
            "pwd" -> {
                onLine(TerminalOutputLine(text = _workingDir.value.absolutePath, type = OutputLineType.STDOUT))
                onLine(TerminalOutputLine(text = "[Exit code 0 | 1ms]", type = OutputLineType.SUCCESS))
                return 0
            }
            "clear" -> {
                // Signal clear to UI by emitting special output line
                onLine(TerminalOutputLine(text = "__CLEAR_CONSOLE__", type = OutputLineType.SYSTEM_INFO))
                return 0
            }
            "help" -> {
                printHelp(onLine)
                return 0
            }
            "history" -> {
                commandHistory.forEachIndexed { index, h ->
                    onLine(TerminalOutputLine(text = "  ${index + 1}  $h", type = OutputLineType.STDOUT))
                }
                onLine(TerminalOutputLine(text = "[Exit code 0 | 1ms]", type = OutputLineType.SUCCESS))
                return 0
            }
            "export" -> {
                if (parts.size == 1) {
                    environmentVariables.forEach { (k, v) ->
                        onLine(TerminalOutputLine(text = "export $k=\"$v\"", type = OutputLineType.STDOUT))
                    }
                    onLine(TerminalOutputLine(text = "[Exit code 0 | 1ms]", type = OutputLineType.SUCCESS))
                    return 0
                } else {
                    val assignment = parts.subList(1, parts.size).joinToString(" ")
                    val eqIndex = assignment.indexOf('=')
                    if (eqIndex != -1) {
                        val key = assignment.substring(0, eqIndex).trim()
                        val value = assignment.substring(eqIndex + 1).trim().removeSurrounding("\"", "\"").removeSurrounding("'", "'")
                        environmentVariables[key] = value
                        onLine(TerminalOutputLine(text = "[Exported $key]", type = OutputLineType.SUCCESS))
                        return 0
                    } else {
                        onLine(TerminalOutputLine(text = "export: usage: export VAR=value", type = OutputLineType.ERROR))
                        return 1
                    }
                }
            }
            "exit" -> {
                onLine(TerminalOutputLine(text = "Session active. Use back button to return to dashboard.", type = OutputLineType.SYSTEM_INFO))
                return 0
            }
        }
        return null
    }

    private fun executeCd(target: String, onLine: (TerminalOutputLine) -> Unit): Int {
        val destFile: File = when {
            target == "~" || target.isEmpty() -> context.filesDir
            target == "-" -> previousDir
            target.startsWith("/") -> File(target)
            target.startsWith("~/") -> File(context.filesDir, target.removePrefix("~/"))
            else -> File(_workingDir.value, target)
        }

        val canonical = try {
            destFile.canonicalFile
        } catch (e: Exception) {
            destFile.absoluteFile
        }

        if (!canonical.exists()) {
            onLine(TerminalOutputLine(text = "cd: $target: No such file or directory", type = OutputLineType.ERROR))
            onLine(TerminalOutputLine(text = "[Exit code 1 | 2ms]", type = OutputLineType.ERROR))
            return 1
        }

        if (!canonical.isDirectory) {
            onLine(TerminalOutputLine(text = "cd: $target: Not a directory", type = OutputLineType.ERROR))
            onLine(TerminalOutputLine(text = "[Exit code 1 | 2ms]", type = OutputLineType.ERROR))
            return 1
        }

        previousDir = _workingDir.value
        _workingDir.value = canonical
        onLine(TerminalOutputLine(text = "[Directory changed to ${canonical.absolutePath}]", type = OutputLineType.SUCCESS))
        return 0
    }

    private fun printHelp(onLine: (TerminalOutputLine) -> Unit) {
        val helpLines = listOf(
            "============================================================",
            "  MOAA CYBER TERMINAL · LINUX / UBUNTU SHELL EMULATOR",
            "============================================================",
            "",
            "BUILT-IN COMMANDS:",
            "  cd <path>        Change directory (~, .., -, /sdcard, relative)",
            "  pwd              Print current working directory",
            "  clear            Clear terminal screen output buffer",
            "  export VAR=VAL   Set custom shell environment variables",
            "  history          Display session command history",
            "  help             Display this help guide",
            "",
            "COMMON LINUX COMMANDS:",
            "  curl <url>       Fetch HTTP/HTTPS API data and send REST requests",
            "  ping <host>      Test network ICMP connectivity (e.g. ping -c 4 8.8.8.8)",
            "  ip addr show     Inspect network interfaces & Wi-Fi IP address",
            "  uname -a         Display Linux kernel version and hardware arch",
            "  df -h            Inspect storage filesystem disk space usage",
            "  ls -lah          List files with permissions, hidden dotfiles & sizes",
            "  cat <file>       Concatenate and display file content",
            "  ps -A            List running system and app processes",
            "  env              Print active environment variables",
            "",
            "SAVED COMMANDS & SHORTCUTS:",
            "  - Tap '⚡ Saved' in the top bar to browse preloaded and custom snippets.",
            "  - Tap '💾' on the input bar to save your current command with a custom name.",
            "  - Use the quick virtual keys row (Tab, |, -, /, ~, $, >) for fast typing.",
            "============================================================"
        )
        helpLines.forEach { onLine(TerminalOutputLine(text = it, type = OutputLineType.STDOUT)) }
        onLine(TerminalOutputLine(text = "[Exit code 0 | 1ms]", type = OutputLineType.SUCCESS))
    }

    fun cancelRunningCommand() {
        try {
            currentProcess.get()?.destroy()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getShortWorkingDir(): String {
        val path = _workingDir.value.absolutePath
        val home = context.filesDir.absolutePath
        val sdcard = Environment.getExternalStorageDirectory()?.absolutePath ?: "/sdcard"

        return when {
            path == home -> "~"
            path.startsWith(home) -> "~/" + path.removePrefix(home).removePrefix("/")
            path == sdcard -> "/sdcard"
            path.startsWith(sdcard) -> "/sdcard/" + path.removePrefix(sdcard).removePrefix("/")
            else -> path
        }
    }
}
