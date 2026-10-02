package dev.motherofallapps.host.tool.ftpclient.storage

import android.content.Context
import android.content.SharedPreferences
import dev.motherofallapps.host.tool.ftpclient.model.FtpConnectionProfile
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistence manager for saved FTP server profiles and remembered credentials.
 */
class FtpProfileRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadProfiles(): List<FtpConnectionProfile> {
        val jsonStr = prefs.getString(KEY_PROFILES, null)
        if (jsonStr.isNullOrBlank()) {
            val defaults = getDefaultPresets()
            saveAll(defaults)
            return defaults
        }

        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<FtpConnectionProfile>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    FtpConnectionProfile(
                        id = obj.optString("id"),
                        name = obj.optString("name"),
                        host = obj.optString("host"),
                        port = obj.optInt("port", 21),
                        username = obj.optString("username", "anonymous"),
                        password = obj.optString("password", ""),
                        defaultRemotePath = obj.optString("defaultRemotePath", "/"),
                        isPassiveMode = obj.optBoolean("isPassiveMode", true),
                        isAnonymous = obj.optBoolean("isAnonymous", false),
                        lastConnectedMs = obj.optLong("lastConnectedMs", 0L),
                        notes = obj.optString("notes", "")
                    )
                )
            }
            if (list.isEmpty()) {
                val defaults = getDefaultPresets()
                saveAll(defaults)
                defaults
            } else {
                list.sortedByDescending { it.lastConnectedMs }
            }
        } catch (e: Exception) {
            getDefaultPresets()
        }
    }

    fun saveProfile(profile: FtpConnectionProfile): List<FtpConnectionProfile> {
        val current = loadProfiles().toMutableList()
        val existingIndex = current.indexOfFirst { it.id == profile.id }
        if (existingIndex >= 0) {
            current[existingIndex] = profile
        } else {
            current.add(0, profile)
        }
        saveAll(current)
        return current
    }

    fun deleteProfile(profileId: String): List<FtpConnectionProfile> {
        val current = loadProfiles().filter { it.id != profileId }
        saveAll(current)
        return current
    }

    fun updateLastConnected(profileId: String): List<FtpConnectionProfile> {
        val current = loadProfiles().toMutableList()
        val index = current.indexOfFirst { it.id == profileId }
        if (index >= 0) {
            val updated = current[index].copy(lastConnectedMs = System.currentTimeMillis())
            current[index] = updated
            saveAll(current)
        }
        return current.sortedByDescending { it.lastConnectedMs }
    }

    private fun saveAll(profiles: List<FtpConnectionProfile>) {
        val jsonArray = JSONArray()
        for (p in profiles) {
            val obj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("host", p.host)
                put("port", p.port)
                put("username", p.username)
                put("password", p.password)
                put("defaultRemotePath", p.defaultRemotePath)
                put("isPassiveMode", p.isPassiveMode)
                put("isAnonymous", p.isAnonymous)
                put("lastConnectedMs", p.lastConnectedMs)
                put("notes", p.notes)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_PROFILES, jsonArray.toString()).apply()
    }

    private fun getDefaultPresets(): List<FtpConnectionProfile> {
        return listOf(
            FtpConnectionProfile(
                id = "preset-local-server",
                name = "Local Host Server (Loopback)",
                host = "127.0.0.1",
                port = 2121,
                username = "admin",
                password = "password",
                defaultRemotePath = "/",
                isPassiveMode = true,
                isAnonymous = false,
                notes = "Connect directly to MotherOfAllApps LAN FTP Server tool."
            ),
            FtpConnectionProfile(
                id = "preset-lan-nas",
                name = "LAN NAS / Router FTP",
                host = "192.168.1.1",
                port = 21,
                username = "admin",
                password = "",
                defaultRemotePath = "/",
                isPassiveMode = true,
                isAnonymous = false,
                notes = "Default LAN router or network attached storage."
            ),
            FtpConnectionProfile(
                id = "preset-open-mirror",
                name = "Kernel.org Mirror (Anonymous)",
                host = "ftp.kernel.org",
                port = 21,
                username = "anonymous",
                password = "anonymous@",
                defaultRemotePath = "/pub",
                isPassiveMode = true,
                isAnonymous = true,
                notes = "Public Linux kernel archive FTP mirror."
            )
        )
    }

    companion object {
        private const val PREFS_NAME = "ftp_client_profiles_prefs"
        private const val KEY_PROFILES = "saved_ftp_profiles"
    }
}
