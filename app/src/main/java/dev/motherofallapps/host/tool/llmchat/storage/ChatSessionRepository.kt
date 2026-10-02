package dev.motherofallapps.host.tool.llmchat.storage

import android.content.Context
import dev.motherofallapps.host.logging.AppLogHub
import dev.motherofallapps.host.logging.LogLevel
import dev.motherofallapps.host.tool.llmchat.model.ChatMessage
import dev.motherofallapps.host.tool.llmchat.model.ChatSession
import dev.motherofallapps.host.tool.llmchat.model.PersonaPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class ChatSessionRepository(private val context: Context) {

    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val storageFile: File by lazy { File(context.filesDir, "llm_chat_sessions.json") }

    private val _sessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val sessions: StateFlow<List<ChatSession>> = _sessions.asStateFlow()

    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSessionId: StateFlow<String?> = _activeSessionId.asStateFlow()

    init {
        loadFromDisk()
    }

    private fun loadFromDisk() {
        if (!storageFile.exists()) {
            // Initialize with default session
            val initialSession = createDefaultSession()
            _sessions.value = listOf(initialSession)
            _activeSessionId.value = initialSession.id
            saveToDisk(_sessions.value)
            return
        }

        try {
            val jsonStr = storageFile.readText()
            val root = JSONObject(jsonStr)
            val sessionsArray = root.optJSONArray("sessions") ?: JSONArray()
            val parsedSessions = mutableListOf<ChatSession>()

            for (i in 0 until sessionsArray.length()) {
                val sObj = sessionsArray.getJSONObject(i)
                val messagesArray = sObj.optJSONArray("messages") ?: JSONArray()
                val parsedMessages = mutableListOf<ChatMessage>()

                for (j in 0 until messagesArray.length()) {
                    val mObj = messagesArray.getJSONObject(j)
                    val trailArray = mObj.optJSONArray("failoverTrail") ?: JSONArray()
                    val trail = mutableListOf<String>()
                    for (k in 0 until trailArray.length()) {
                        trail.add(trailArray.getString(k))
                    }

                    parsedMessages.add(
                        ChatMessage(
                            id = mObj.optString("id", UUID.randomUUID().toString()),
                            role = mObj.optString("role", "user"),
                            content = mObj.optString("content", ""),
                            timestamp = mObj.optLong("timestamp", System.currentTimeMillis()),
                            modelUsed = mObj.optString("modelUsed").ifBlank { null },
                            providerUsed = mObj.optString("providerUsed").ifBlank { null },
                            latencyMs = mObj.optLong("latencyMs", 0),
                            failoverTrail = trail,
                            isStreaming = false,
                            isError = mObj.optBoolean("isError", false)
                        )
                    )
                }

                parsedSessions.add(
                    ChatSession(
                        id = sObj.optString("id", UUID.randomUUID().toString()),
                        title = sObj.optString("title", "Conversation"),
                        personaId = sObj.optString("personaId", PersonaPreset.DEFAULT_ASSISTANT.id),
                        systemPrompt = sObj.optString("systemPrompt", PersonaPreset.DEFAULT_ASSISTANT.systemPrompt),
                        temperature = sObj.optDouble("temperature", 0.7).toFloat(),
                        targetModelOverride = sObj.optString("targetModelOverride").ifBlank { null },
                        createdAt = sObj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = sObj.optLong("updatedAt", System.currentTimeMillis()),
                        messages = parsedMessages
                    )
                )
            }

            if (parsedSessions.isEmpty()) {
                val def = createDefaultSession()
                _sessions.value = listOf(def)
                _activeSessionId.value = def.id
            } else {
                _sessions.value = parsedSessions.sortedByDescending { it.updatedAt }
                _activeSessionId.value = root.optString("lastActiveSessionId").ifBlank { parsedSessions.first().id }
            }
        } catch (e: Exception) {
            val def = createDefaultSession()
            _sessions.value = listOf(def)
            _activeSessionId.value = def.id
        }
    }

    private fun saveToDisk(list: List<ChatSession>) {
        scope.launch {
            mutex.withLock {
                try {
                    val root = JSONObject()
                    root.put("lastActiveSessionId", _activeSessionId.value)
                    val arr = JSONArray()

                    list.forEach { session ->
                        val sObj = JSONObject()
                        sObj.put("id", session.id)
                        sObj.put("title", session.title)
                        sObj.put("personaId", session.personaId)
                        sObj.put("systemPrompt", session.systemPrompt)
                        sObj.put("temperature", session.temperature.toDouble())
                        sObj.put("targetModelOverride", session.targetModelOverride ?: "")
                        sObj.put("createdAt", session.createdAt)
                        sObj.put("updatedAt", session.updatedAt)

                        val msgArr = JSONArray()
                        session.messages.forEach { msg ->
                            val mObj = JSONObject()
                            mObj.put("id", msg.id)
                            mObj.put("role", msg.role)
                            mObj.put("content", msg.content)
                            mObj.put("timestamp", msg.timestamp)
                            mObj.put("modelUsed", msg.modelUsed ?: "")
                            mObj.put("providerUsed", msg.providerUsed ?: "")
                            mObj.put("latencyMs", msg.latencyMs)
                            mObj.put("isError", msg.isError)

                            val trailArr = JSONArray()
                            msg.failoverTrail.forEach { trailArr.put(it) }
                            mObj.put("failoverTrail", trailArr)

                            msgArr.put(mObj)
                        }
                        sObj.put("messages", msgArr)
                        arr.put(sObj)
                    }

                    root.put("sessions", arr)
                    storageFile.writeText(root.toString(2))
                } catch (e: Exception) {
                    AppLogHub.log(
                        toolId = "llm-chat",
                        toolName = "LLM Chat",
                        level = LogLevel.ERROR,
                        tag = "Storage",
                        message = "LLM CHAT [STORAGE ERROR] Failed to save chat sessions: ${e.message}"
                    )
                }
            }
        }
    }

    private fun createDefaultSession(): ChatSession {
        return ChatSession(
            id = "session-${UUID.randomUUID()}",
            title = "Welcome Chat",
            personaId = PersonaPreset.DEFAULT_ASSISTANT.id,
            systemPrompt = PersonaPreset.DEFAULT_ASSISTANT.systemPrompt,
            temperature = PersonaPreset.DEFAULT_ASSISTANT.defaultTemperature,
            messages = listOf(
                ChatMessage(
                    role = "assistant",
                    content = "👋 Welcome to **CyberChat Studio**!\n\nI am connected directly to your local **LLM Gateway** (`http://127.0.0.1:8080`). I can query Cloud Gemini/ChatGPT as well as local LAN desktop instances (Ollama, LM Studio) with automatic zero-downtime failover.\n\nHow can I help you today?",
                    modelUsed = "LLM Gateway Router",
                    providerUsed = "Local Gateway"
                )
            )
        )
    }

    fun selectSession(sessionId: String) {
        _activeSessionId.value = sessionId
        saveToDisk(_sessions.value)
    }

    fun createNewSession(preset: PersonaPreset = PersonaPreset.DEFAULT_ASSISTANT): ChatSession {
        val newSession = ChatSession(
            id = "session-${UUID.randomUUID()}",
            title = "New ${preset.name} Chat",
            personaId = preset.id,
            systemPrompt = preset.systemPrompt,
            temperature = preset.defaultTemperature,
            messages = emptyList()
        )
        val updated = listOf(newSession) + _sessions.value
        _sessions.value = updated
        _activeSessionId.value = newSession.id
        saveToDisk(updated)
        return newSession
    }

    fun deleteSession(sessionId: String) {
        val current = _sessions.value.filterNot { it.id == sessionId }
        val updated = if (current.isEmpty()) {
            listOf(createDefaultSession())
        } else current

        _sessions.value = updated
        if (_activeSessionId.value == sessionId) {
            _activeSessionId.value = updated.first().id
        }
        saveToDisk(updated)
    }

    fun renameSession(sessionId: String, newTitle: String) {
        val updated = _sessions.value.map { session ->
            if (session.id == sessionId) {
                session.copy(title = newTitle.trim(), updatedAt = System.currentTimeMillis())
            } else session
        }
        _sessions.value = updated
        saveToDisk(updated)
    }

    fun updateSessionSettings(
        sessionId: String,
        personaId: String,
        systemPrompt: String,
        temperature: Float,
        targetModelOverride: String?
    ) {
        val updated = _sessions.value.map { session ->
            if (session.id == sessionId) {
                session.copy(
                    personaId = personaId,
                    systemPrompt = systemPrompt,
                    temperature = temperature,
                    targetModelOverride = targetModelOverride?.trim()?.ifBlank { null },
                    updatedAt = System.currentTimeMillis()
                )
            } else session
        }
        _sessions.value = updated
        saveToDisk(updated)
    }

    fun addMessage(sessionId: String, message: ChatMessage) {
        val updated = _sessions.value.map { session ->
            if (session.id == sessionId) {
                val newMessages = session.messages + message
                val newTitle = if (session.messages.isEmpty() && message.role == "user") {
                    val candidate = message.content.take(30).trim()
                    if (candidate.isNotBlank()) candidate else session.title
                } else session.title

                session.copy(
                    title = newTitle,
                    messages = newMessages,
                    updatedAt = System.currentTimeMillis()
                )
            } else session
        }
        _sessions.value = updated
        saveToDisk(updated)
    }

    fun updateMessage(sessionId: String, messageId: String, update: (ChatMessage) -> ChatMessage) {
        val updated = _sessions.value.map { session ->
            if (session.id == sessionId) {
                val newMessages = session.messages.map { msg ->
                    if (msg.id == messageId) update(msg) else msg
                }
                session.copy(messages = newMessages, updatedAt = System.currentTimeMillis())
            } else session
        }
        _sessions.value = updated
        saveToDisk(updated)
    }

    fun clearMessages(sessionId: String) {
        val updated = _sessions.value.map { session ->
            if (session.id == sessionId) {
                session.copy(messages = emptyList(), updatedAt = System.currentTimeMillis())
            } else session
        }
        _sessions.value = updated
        saveToDisk(updated)
    }

    fun getActiveSession(): ChatSession? {
        val activeId = _activeSessionId.value ?: return _sessions.value.firstOrNull()
        return _sessions.value.firstOrNull { it.id == activeId } ?: _sessions.value.firstOrNull()
    }
}
