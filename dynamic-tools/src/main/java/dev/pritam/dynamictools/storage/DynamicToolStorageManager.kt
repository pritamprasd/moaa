package dev.pritam.dynamictools.storage

import android.content.Context
import dev.pritam.dynamictools.model.DynamicToolBundle
import dev.pritam.dynamictools.model.DynamicToolManifest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

/**
 * Sandboxed storage engine for dynamic web-based tools.
 * All files are isolated in `context.filesDir/custom_tools/<tool_id>/`.
 */
class DynamicToolStorageManager private constructor(private val context: Context) {

    private val baseDir: File
        get() = File(context.filesDir, "custom_tools").apply {
            if (!exists()) mkdirs()
        }

    private val _toolsFlow = MutableStateFlow<List<DynamicToolBundle>>(emptyList())
    val toolsFlow: StateFlow<List<DynamicToolBundle>> = _toolsFlow.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        loadAllTools()
    }

    fun loadAllTools() {
        scope.launch {
            val list = mutableListOf<DynamicToolBundle>()
            val root = baseDir

            val toolDirs = root.listFiles { file -> file.isDirectory } ?: emptyArray()

            // If empty, seed default tools
            if (toolDirs.isEmpty()) {
                val seeds = DefaultToolsSeed.getSeedTools()
                seeds.forEach { seed ->
                    saveInternal(seed)
                }
            }

            // Read all tool directories
            root.listFiles { file -> file.isDirectory }?.forEach { dir ->
                readBundleFromDirectory(dir)?.let { list.add(it) }
            }

            _toolsFlow.value = list.sortedByDescending { it.manifest.updatedAt }
        }
    }

    fun getTool(toolId: String): DynamicToolBundle? {
        val toolDir = File(baseDir, sanitizeToolId(toolId))
        if (!toolDir.exists() || !toolDir.isDirectory) return null
        return readBundleFromDirectory(toolDir)
    }

    fun saveTool(bundle: DynamicToolBundle): Boolean {
        val success = saveInternal(bundle)
        if (success) {
            loadAllTools()
        }
        return success
    }

    private fun saveInternal(bundle: DynamicToolBundle): Boolean {
        return try {
            val toolDir = File(baseDir, sanitizeToolId(bundle.manifest.toolId))
            if (!toolDir.exists()) toolDir.mkdirs()

            // Write manifest.json
            val manifestFile = File(toolDir, "manifest.json")
            manifestFile.writeText(bundle.manifest.toJson().toString(2))

            // Write index.html
            val htmlFile = File(toolDir, "index.html")
            htmlFile.writeText(bundle.html)

            // Write styles.css
            val cssFile = File(toolDir, "styles.css")
            cssFile.writeText(bundle.css)

            // Write app.js
            val jsFile = File(toolDir, "app.js")
            jsFile.writeText(bundle.js)

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun updateToolCode(toolId: String, html: String, css: String, js: String): Boolean {
        val toolDir = File(baseDir, sanitizeToolId(toolId))
        if (!toolDir.exists()) return false

        return try {
            val manifestFile = File(toolDir, "manifest.json")
            val manifest = if (manifestFile.exists()) {
                DynamicToolManifest.fromJson(JSONObject(manifestFile.readText())).copy(updatedAt = System.currentTimeMillis())
            } else {
                DynamicToolManifest(toolId = toolId, displayName = toolId, description = "Updated tool")
            }

            manifestFile.writeText(manifest.toJson().toString(2))
            File(toolDir, "index.html").writeText(html)
            File(toolDir, "styles.css").writeText(css)
            File(toolDir, "app.js").writeText(js)

            loadAllTools()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun deleteTool(toolId: String): Boolean {
        val toolDir = File(baseDir, sanitizeToolId(toolId))
        if (!toolDir.exists()) return false

        return try {
            toolDir.deleteRecursively()
            loadAllTools()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun exportToolBundleJson(toolId: String): String? {
        val bundle = getTool(toolId) ?: return null
        val exportJson = JSONObject().apply {
            put("manifest", bundle.manifest.toJson())
            put("html", bundle.html)
            put("css", bundle.css)
            put("js", bundle.js)
        }
        return exportJson.toString(2)
    }

    fun importToolFromJson(jsonStr: String): Result<DynamicToolBundle> {
        return try {
            val root = JSONObject(jsonStr)
            val manifestObj = root.getJSONObject("manifest")
            val manifest = DynamicToolManifest.fromJson(manifestObj).copy(
                toolId = manifestObj.optString("tool_id", "imported_${System.currentTimeMillis()}"),
                updatedAt = System.currentTimeMillis()
            )
            val html = root.optString("html", "<div>Imported Tool</div>")
            val css = root.optString("css", "body { background: #0B0F19; color: #FFF; }")
            val js = root.optString("js", "// Imported Tool Logic")

            val bundle = DynamicToolBundle(manifest, html, css, js)
            saveTool(bundle)
            Result.success(bundle)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun readBundleFromDirectory(dir: File): DynamicToolBundle? {
        return try {
            val manifestFile = File(dir, "manifest.json")
            val manifest = if (manifestFile.exists()) {
                DynamicToolManifest.fromJson(JSONObject(manifestFile.readText()))
            } else {
                DynamicToolManifest(
                    toolId = dir.name,
                    displayName = dir.name.replace("_", " ").capitalizeWords(),
                    description = "Dynamic Web Tool"
                )
            }

            val htmlFile = File(dir, "index.html")
            val cssFile = File(dir, "styles.css")
            val jsFile = File(dir, "app.js")

            val html = if (htmlFile.exists()) htmlFile.readText() else "<div>No HTML</div>"
            val css = if (cssFile.exists()) cssFile.readText() else ""
            val js = if (jsFile.exists()) jsFile.readText() else ""

            DynamicToolBundle(
                manifest = manifest,
                html = html,
                css = css,
                js = js,
                rootDirectoryPath = dir.absolutePath
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun sanitizeToolId(id: String): String {
        return id.lowercase().replace("[^a-z0-9_\\-]".toRegex(), "_")
    }

    private fun String.capitalizeWords(): String {
        return split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }

    companion object {
        @Volatile
        private var INSTANCE: DynamicToolStorageManager? = null

        fun getInstance(context: Context): DynamicToolStorageManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DynamicToolStorageManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
