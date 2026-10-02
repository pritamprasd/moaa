package dev.motherofallapps.host.tool.llmgateway.storage

import android.content.Context
import android.content.SharedPreferences
import dev.motherofallapps.host.tool.llmgateway.model.LlmProfile
import dev.motherofallapps.host.tool.llmgateway.model.ProfileStatus
import dev.motherofallapps.host.tool.llmgateway.model.ProviderCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class LlmProfileRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _profiles = MutableStateFlow<List<LlmProfile>>(emptyList())
    val profiles: StateFlow<List<LlmProfile>> = _profiles.asStateFlow()

    init {
        loadProfiles()
    }

    fun loadProfiles() {
        val jsonStr = prefs.getString(KEY_PROFILES, null)
        if (jsonStr.isNullOrBlank()) {
            val defaults = createDefaultProfiles()
            saveProfilesInternal(defaults)
            _profiles.value = defaults
        } else {
            val loaded = mutableListOf<LlmProfile>()
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    loaded.add(
                        LlmProfile(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            name = obj.optString("name", "Unnamed Profile"),
                            category = try {
                                ProviderCategory.valueOf(obj.optString("category", ProviderCategory.DESKTOP_LOCAL_HOST.name))
                            } catch (e: Exception) { ProviderCategory.DESKTOP_LOCAL_HOST },
                            providerType = obj.optString("providerType", "OLLAMA_LOCAL"),
                            accountEmail = obj.optString("accountEmail").ifBlank { null },
                            accessToken = obj.optString("accessToken").ifBlank { null },
                            refreshToken = obj.optString("refreshToken").ifBlank { null },
                            expiresAt = if (obj.has("expiresAt")) obj.optLong("expiresAt") else null,
                            apiKey = obj.optString("apiKey").ifBlank { null },
                            hostAddress = obj.optString("hostAddress").ifBlank { null },
                            targetModel = obj.optString("targetModel").ifBlank { null },
                            status = try {
                                ProfileStatus.valueOf(obj.optString("status", ProfileStatus.IDLE.name))
                            } catch (e: Exception) { ProfileStatus.IDLE },
                            latencyMs = obj.optLong("latencyMs", 0L),
                            priorityOrder = obj.optInt("priorityOrder", i),
                            isEnabled = obj.optBoolean("isEnabled", true),
                            quotaRemaining = if (obj.has("quotaRemaining")) obj.optInt("quotaRemaining") else null,
                            quotaLimit = if (obj.has("quotaLimit")) obj.optInt("quotaLimit") else null
                        )
                    )
                }
            } catch (e: Exception) {
                loaded.addAll(createDefaultProfiles())
            }
            val sorted = loaded.sortedBy { it.priorityOrder }
            _profiles.value = sorted
        }
    }

    fun addProfile(profile: LlmProfile) {
        val current = _profiles.value.toMutableList()
        val newProfile = profile.copy(priorityOrder = current.size)
        current.add(newProfile)
        saveProfiles(current)
    }

    fun updateProfile(profile: LlmProfile) {
        val current = _profiles.value.toMutableList()
        val index = current.indexOfFirst { it.id == profile.id }
        if (index != -1) {
            current[index] = profile
            saveProfiles(current)
        }
    }

    fun deleteProfile(profileId: String) {
        val current = _profiles.value.filter { it.id != profileId }.toMutableList()
        current.forEachIndexed { idx, p -> current[idx] = p.copy(priorityOrder = idx) }
        saveProfiles(current)
    }

    fun movePriority(fromIndex: Int, toIndex: Int) {
        val current = _profiles.value.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            current.forEachIndexed { idx, p -> current[idx] = p.copy(priorityOrder = idx) }
            saveProfiles(current)
        }
    }

    fun updateStatus(profileId: String, status: ProfileStatus, latencyMs: Long = 0L) {
        val current = _profiles.value.toMutableList()
        val index = current.indexOfFirst { it.id == profileId }
        if (index != -1) {
            val item = current[index]
            current[index] = item.copy(status = status, latencyMs = if (latencyMs > 0) latencyMs else item.latencyMs)
            _profiles.value = current
            saveProfilesInternal(current)
        }
    }

    private fun saveProfiles(list: List<LlmProfile>) {
        _profiles.value = list
        saveProfilesInternal(list)
    }

    private fun saveProfilesInternal(list: List<LlmProfile>) {
        val array = JSONArray()
        list.forEach { p ->
            val obj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("category", p.category.name)
                put("providerType", p.providerType)
                put("accountEmail", p.accountEmail ?: "")
                put("accessToken", p.accessToken ?: "")
                put("refreshToken", p.refreshToken ?: "")
                p.expiresAt?.let { put("expiresAt", it) }
                put("apiKey", p.apiKey ?: "")
                put("hostAddress", p.hostAddress ?: "")
                put("targetModel", p.targetModel ?: "")
                put("status", p.status.name)
                put("latencyMs", p.latencyMs)
                put("priorityOrder", p.priorityOrder)
                put("isEnabled", p.isEnabled)
                p.quotaRemaining?.let { put("quotaRemaining", it) }
                p.quotaLimit?.let { put("quotaLimit", it) }
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_PROFILES, array.toString()).apply()
    }

    private fun createDefaultProfiles(): List<LlmProfile> {
        return listOf(
            LlmProfile(
                id = "profile-ollama-local",
                name = "Home Desktop Ollama",
                category = ProviderCategory.DESKTOP_LOCAL_HOST,
                providerType = "OLLAMA_LOCAL",
                hostAddress = "http://192.168.1.100:11434",
                targetModel = "llama3.2:latest",
                status = ProfileStatus.IDLE,
                priorityOrder = 0,
                isEnabled = true
            ),
            LlmProfile(
                id = "profile-gemini-cloud",
                name = "Google Gemini 1.5 Flash",
                category = ProviderCategory.CLOUD_OAUTH,
                providerType = "GEMINI_CLOUD",
                targetModel = "gemini-1.5-flash",
                status = ProfileStatus.IDLE,
                priorityOrder = 1,
                isEnabled = true
            ),
            LlmProfile(
                id = "profile-chatgpt-cloud",
                name = "ChatGPT 4o-mini (Cloud)",
                category = ProviderCategory.CLOUD_OAUTH,
                providerType = "CHATGPT_CLOUD",
                targetModel = "gpt-4o-mini",
                status = ProfileStatus.IDLE,
                priorityOrder = 2,
                isEnabled = true
            ),
            LlmProfile(
                id = "profile-lmstudio-local",
                name = "Desktop LM Studio",
                category = ProviderCategory.DESKTOP_LOCAL_HOST,
                providerType = "LM_STUDIO",
                hostAddress = "http://192.168.1.100:1234",
                targetModel = "local-model",
                status = ProfileStatus.IDLE,
                priorityOrder = 3,
                isEnabled = false
            )
        )
    }

    companion object {
        private const val PREFS_NAME = "llm_gateway_profiles_prefs"
        private const val KEY_PROFILES = "saved_profiles_json"
    }
}
