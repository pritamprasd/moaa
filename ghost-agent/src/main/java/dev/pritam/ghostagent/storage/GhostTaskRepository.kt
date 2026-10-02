package dev.pritam.ghostagent.storage

import android.content.Context
import dev.pritam.ghostagent.model.GhostTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Persistent JSON-backed repository for GhostTasks.
 * Tasks are stored in: /data/user/0/<package>/files/ghost_tasks/tasks.json
 *
 * Thread-safe via coroutine Dispatchers.IO. Exposes a StateFlow<List<GhostTask>>
 * so the UI layer reacts to changes automatically.
 */
class GhostTaskRepository(context: Context) {

    private val tasksDir = File(context.filesDir, "ghost_tasks").also { it.mkdirs() }
    private val tasksFile = File(tasksDir, "tasks.json")

    private val _tasks = MutableStateFlow<List<GhostTask>>(emptyList())
    val tasks: StateFlow<List<GhostTask>> = _tasks.asStateFlow()

    init {
        // Load tasks synchronously on first access (repository is singleton via companion)
        _tasks.value = loadFromDisk()
    }

    // ─── Read ────────────────────────────────────────────────────────────────

    fun getTask(taskId: String): GhostTask? = _tasks.value.firstOrNull { it.taskId == taskId }

    // ─── Write ───────────────────────────────────────────────────────────────

    suspend fun saveTask(task: GhostTask) = withContext(Dispatchers.IO) {
        val updated = task.copy(updatedAt = System.currentTimeMillis())
        val current = _tasks.value.toMutableList()
        val idx = current.indexOfFirst { it.taskId == updated.taskId }
        if (idx >= 0) current[idx] = updated else current.add(updated)
        _tasks.value = current
        persistToDisk(current)
    }

    suspend fun deleteTask(taskId: String) = withContext(Dispatchers.IO) {
        val current = _tasks.value.filter { it.taskId != taskId }
        _tasks.value = current
        persistToDisk(current)
        // Also clean up any screenshots associated with this task
        File(tasksDir, "screenshots/$taskId").deleteRecursively()
    }

    suspend fun incrementRunCount(taskId: String) = withContext(Dispatchers.IO) {
        val task = getTask(taskId) ?: return@withContext
        saveTask(task.copy(runCount = task.runCount + 1))
    }

    // ─── Screenshots ─────────────────────────────────────────────────────────

    fun getScreenshotFile(taskId: String, stepId: String): File {
        val dir = File(tasksDir, "screenshots/$taskId").also { it.mkdirs() }
        return File(dir, "step_${stepId}.png")
    }

    // ─── Disk persistence ────────────────────────────────────────────────────

    private fun loadFromDisk(): List<GhostTask> {
        return try {
            if (!tasksFile.exists()) return emptyList()
            val text = tasksFile.readText(Charsets.UTF_8)
            val arr = JSONArray(text)
            (0 until arr.length()).mapNotNull {
                runCatching { GhostTask.fromJson(arr.getJSONObject(it)) }.getOrNull()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun persistToDisk(tasks: List<GhostTask>) {
        try {
            val arr = JSONArray()
            tasks.forEach { arr.put(it.toJson()) }
            tasksFile.writeText(arr.toString(2), Charsets.UTF_8)
        } catch (_: Exception) { /* ignore write failures silently */ }
    }

    // ─── Export / Import ─────────────────────────────────────────────────────

    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        val arr = JSONArray()
        _tasks.value.forEach { arr.put(it.toJson()) }
        arr.toString(2)
    }

    suspend fun importJson(json: String): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val arr = JSONArray(json)
            val imported = (0 until arr.length()).mapNotNull {
                runCatching { GhostTask.fromJson(arr.getJSONObject(it)) }.getOrNull()
            }
            imported.forEach { saveTask(it) }
            imported.size
        }
    }

    companion object {
        @Volatile private var INSTANCE: GhostTaskRepository? = null

        fun getInstance(context: Context): GhostTaskRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: GhostTaskRepository(context.applicationContext).also { INSTANCE = it }
            }
    }
}
