package dev.pritam.host.tool.terminal.manager

import android.content.Context
import android.content.SharedPreferences
import dev.pritam.host.tool.terminal.model.SavedCommand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Manages user-saved terminal commands and preloaded command templates
 * (e.g., Ollama tags, LLM Gateway, curl tests, network utilities).
 * Persists commands in SharedPreferences.
 */
class SavedCommandsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _savedCommands = MutableStateFlow<List<SavedCommand>>(emptyList())
    val savedCommands: StateFlow<List<SavedCommand>> = _savedCommands.asStateFlow()

    init {
        loadSavedCommands()
    }

    private fun loadSavedCommands() {
        val jsonString = prefs.getString(KEY_COMMANDS, null)
        if (jsonString.isNullOrBlank()) {
            val defaults = getDefaultCommands()
            _savedCommands.value = defaults
            persist(defaults)
        } else {
            try {
                val array = JSONArray(jsonString)
                val list = mutableListOf<SavedCommand>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        SavedCommand(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            name = obj.optString("name", "Unnamed Command"),
                            command = obj.optString("command", ""),
                            category = obj.optString("category", "General"),
                            description = obj.optString("description", ""),
                            isFavorite = obj.optBoolean("isFavorite", false),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
                if (list.isEmpty()) {
                    val defaults = getDefaultCommands()
                    _savedCommands.value = defaults
                    persist(defaults)
                } else {
                    _savedCommands.value = list
                }
            } catch (e: Exception) {
                val defaults = getDefaultCommands()
                _savedCommands.value = defaults
                persist(defaults)
            }
        }
    }

    private fun persist(list: List<SavedCommand>) {
        try {
            val array = JSONArray()
            list.forEach { cmd ->
                val obj = JSONObject().apply {
                    put("id", cmd.id)
                    put("name", cmd.name)
                    put("command", cmd.command)
                    put("category", cmd.category)
                    put("description", cmd.description)
                    put("isFavorite", cmd.isFavorite)
                    put("createdAt", cmd.createdAt)
                }
                array.put(obj)
            }
            prefs.edit().putString(KEY_COMMANDS, array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveCommand(
        name: String,
        command: String,
        category: String = "General",
        description: String = "",
        isFavorite: Boolean = false
    ): SavedCommand {
        val newCmd = SavedCommand(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            command = command.trim(),
            category = category.trim().ifEmpty { "General" },
            description = description.trim(),
            isFavorite = isFavorite,
            createdAt = System.currentTimeMillis()
        )
        val updated = listOf(newCmd) + _savedCommands.value.filter { it.id != newCmd.id }
        _savedCommands.value = updated
        persist(updated)
        return newCmd
    }

    fun updateCommand(command: SavedCommand) {
        val updated = _savedCommands.value.map {
            if (it.id == command.id) command else it
        }
        _savedCommands.value = updated
        persist(updated)
    }

    fun deleteCommand(id: String) {
        val updated = _savedCommands.value.filter { it.id != id }
        _savedCommands.value = updated
        persist(updated)
    }

    fun toggleFavorite(id: String) {
        val updated = _savedCommands.value.map {
            if (it.id == id) it.copy(isFavorite = !it.isFavorite) else it
        }
        _savedCommands.value = updated
        persist(updated)
    }

    fun resetToDefaults() {
        val defaults = getDefaultCommands()
        _savedCommands.value = defaults
        persist(defaults)
    }

    companion object {
        private const val PREFS_NAME = "terminal_saved_commands_prefs"
        private const val KEY_COMMANDS = "saved_commands_json"

        fun getDefaultCommands(): List<SavedCommand> = listOf(
            SavedCommand(
                id = "default_ollama_tags",
                name = "Ollama Local Tags",
                command = "curl -s http://127.0.0.1:11434/api/tags",
                category = "AI & LLM",
                description = "Query local desktop/device Ollama server to list downloaded models",
                isFavorite = true
            ),
            SavedCommand(
                id = "default_gateway_models",
                name = "LLM Gateway Models",
                command = "curl -s http://127.0.0.1:8080/v1/models",
                category = "AI & LLM",
                description = "Inspect embedded loopback LLM Gateway active models and routing endpoints",
                isFavorite = true
            ),
            SavedCommand(
                id = "default_ip_addr",
                name = "Network Interfaces & IP",
                command = "ip addr show || ifconfig",
                category = "Networking",
                description = "List all network interfaces, Wi-Fi wlan0 IP, and cellular subnets",
                isFavorite = true
            ),
            SavedCommand(
                id = "default_ping_dns",
                name = "Ping Google DNS",
                command = "ping -c 4 8.8.8.8",
                category = "Networking",
                description = "Send 4 ICMP ping packets to 8.8.8.8 to verify internet connectivity",
                isFavorite = true
            ),
            SavedCommand(
                id = "default_sys_uname",
                name = "Kernel & System Info",
                command = "uname -a",
                category = "System",
                description = "Print Linux kernel release, architecture (aarch64), and build details",
                isFavorite = true
            ),
            SavedCommand(
                id = "default_disk_df",
                name = "Storage & Disk Usage",
                command = "df -h",
                category = "Storage",
                description = "Display filesystem disk space usage in human-readable megabytes/gigabytes",
                isFavorite = false
            ),
            SavedCommand(
                id = "default_ps_top",
                name = "Active Processes",
                command = "ps -A | head -n 35",
                category = "System",
                description = "List top running system and app processes with PID and CPU usage",
                isFavorite = false
            ),
            SavedCommand(
                id = "default_ls_detailed",
                name = "Detailed Directory List",
                command = "ls -lah",
                category = "Storage",
                description = "List all files including hidden dotfiles with permissions and timestamps",
                isFavorite = false
            ),
            SavedCommand(
                id = "default_env_vars",
                name = "Environment Variables",
                command = "env | sort",
                category = "System",
                description = "Dump current shell environment variables, PATH, and export configurations",
                isFavorite = false
            ),
            SavedCommand(
                id = "default_meminfo",
                name = "RAM & Memory Stats",
                command = "cat /proc/meminfo | head -n 15",
                category = "Hardware",
                description = "Read kernel memory stats: MemTotal, MemFree, MemAvailable, Buffers & Cached",
                isFavorite = false
            ),
            SavedCommand(
                id = "default_cpuinfo",
                name = "CPU & Hardware Cores",
                command = "cat /proc/cpuinfo | grep -E 'processor|model name|Hardware|BogoMIPS' | head -n 20",
                category = "Hardware",
                description = "Inspect device SoC, processor cores, and clock frequencies",
                isFavorite = false
            ),
            SavedCommand(
                id = "default_open_sockets",
                name = "Listening Ports & Sockets",
                command = "cat /proc/net/tcp /proc/net/tcp6 2>/dev/null | head -n 25 || netstat -tlpn",
                category = "Networking",
                description = "Check active TCP sockets and listening local server ports",
                isFavorite = false
            )
        )
    }
}
