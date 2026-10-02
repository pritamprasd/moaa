package dev.pritam.host.tool.nfc.model

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import java.util.Locale

enum class NfcWriteType(val displayName: String, val category: String) {
    TEXT("Plain Text", "Standard NDEF"),
    URI("Web URL / Link", "Standard NDEF"),
    WIFI("Wi-Fi Quick Connect", "Connectivity"),
    CONTACT_VCARD("vCard Business Card", "Identity & Contacts"),
    SOCIAL_PROFILE("Social Media Link", "Identity & Marketing"),
    ONE_TAP_REVIEW("One-Tap Review Card", "Marketing & Business"),
    APP_LAUNCHER("App Launch Trigger", "Automation"),
    DEVICE_AUTOMATION("Device Task Trigger", "Automation"),
    NTAG_MIRRORING("NTAG Dynamic Mirror URL", "Industrial / NXP"),
    GAMING_NTAG215("Gaming / Amiibo Preset", "Gaming & Simulation"),
    RAW_HEX("Raw Hex Payload", "Advanced Developer"),
    CUSTOM_MIME("Custom MIME Stream", "Advanced Developer"),
    ERASE_FORMAT("Erase / Format to Blank", "Management & Operations"),
}

data class NfcWritePayload(
    val writeType: NfcWriteType = NfcWriteType.TEXT,

    // Plain Text & URI
    val textContent: String = "Hello from MotherOfAllApps NFC!",
    val uriString: String = "https://github.com",

    // Wi-Fi
    val wifiSsid: String = "MyHomeWiFi",
    val wifiPassword: String = "",
    val wifiAuthType: String = "WPA2", // OPEN, WPA, WPA2, WPA3

    // vCard / Contact
    val contactName: String = "Alex Vance",
    val contactPhone: String = "+1 555-0199",
    val contactEmail: String = "alex@example.com",
    val contactOrganization: String = "Cyber Systems Lab",
    val contactTitle: String = "Chief Engineer",

    // Social & Marketing
    val socialPlatform: String = "INSTAGRAM", // INSTAGRAM, LINKEDIN, X_TWITTER, YOUTUBE, TIKTOK, WHATSAPP, GITHUB
    val socialHandleOrUrl: String = "alex_vance_official",

    // One-Tap Review
    val reviewPlatform: String = "GOOGLE", // GOOGLE, TRUSTPILOT, YELP
    val reviewPlaceOrLink: String = "https://g.page/r/your-business/review",

    // Automation & Device Tasks
    val appPackageName: String = "com.google.android.youtube",
    val automationTaskAction: String = "TOGGLE_WIFI", // TOGGLE_WIFI, TOGGLE_BT, TOGGLE_FLASHLIGHT, SILENT_MODE, HOTSPOT_ON

    // NTAG Dynamic Mirroring (NXP TagWriter feature)
    val mirroringBaseUrl: String = "https://verify.domain.com/check?uid={UID}&cnt={CNT}",

    // Gaming / Amiibo Preset (TagMo feature)
    val selectedGamingPresetId: String = "zelda-link-totk",

    // Developer / Low Level
    val rawHexPayload: String = "00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F",
    val customMimeType: String = "application/vnd.motherofallapps.custom",
    val customMimePayload: String = "{\"app\":\"mother_of_all_apps\",\"command\":\"ping\"}",

    // Tag Operations
    val makeReadOnly: Boolean = false,
    val isBatchMode: Boolean = false,
    val batchSuccessCount: Int = 0,
) {
    /**
     * Converts this configuration into standard Android NdefMessage.
     */
    fun toNdefMessage(): NdefMessage {
        val records = mutableListOf<NdefRecord>()

        when (writeType) {
            NfcWriteType.TEXT -> {
                val lang = Locale.getDefault().language.toByteArray(Charsets.US_ASCII)
                val text = textContent.toByteArray(Charsets.UTF_8)
                val payload = ByteArray(1 + lang.size + text.size)
                payload[0] = lang.size.toByte()
                System.arraycopy(lang, 0, payload, 1, lang.size)
                System.arraycopy(text, 0, payload, 1 + lang.size, text.size)
                records.add(NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, ByteArray(0), payload))
            }
            NfcWriteType.URI -> {
                records.add(NdefRecord.createUri(uriString))
            }
            NfcWriteType.WIFI -> {
                val wifiConfigPayload = "WIFI:S:$wifiSsid;T:$wifiAuthType;P:$wifiPassword;;"
                records.add(NdefRecord.createMime("application/vnd.wfa.wsc", wifiConfigPayload.toByteArray(Charsets.UTF_8)))
            }
            NfcWriteType.CONTACT_VCARD -> {
                val vCard = buildString {
                    appendLine("BEGIN:VCARD")
                    appendLine("VERSION:3.0")
                    appendLine("N:;$contactName;;;")
                    appendLine("FN:$contactName")
                    if (contactOrganization.isNotBlank()) appendLine("ORG:$contactOrganization")
                    if (contactTitle.isNotBlank()) appendLine("TITLE:$contactTitle")
                    if (contactPhone.isNotBlank()) appendLine("TEL;TYPE=CELL:$contactPhone")
                    if (contactEmail.isNotBlank()) appendLine("EMAIL:$contactEmail")
                    appendLine("END:VCARD")
                }
                records.add(NdefRecord.createMime("text/vcard", vCard.toByteArray(Charsets.UTF_8)))
            }
            NfcWriteType.SOCIAL_PROFILE -> {
                val url = buildSocialUrl(socialPlatform, socialHandleOrUrl)
                records.add(NdefRecord.createUri(url))
            }
            NfcWriteType.ONE_TAP_REVIEW -> {
                records.add(NdefRecord.createUri(reviewPlaceOrLink))
            }
            NfcWriteType.APP_LAUNCHER -> {
                records.add(NdefRecord.createApplicationRecord(appPackageName))
            }
            NfcWriteType.DEVICE_AUTOMATION -> {
                // NFC Tools / NFC Tasks compatible intent action payload
                val automationPayload = "tasker://action/$automationTaskAction"
                records.add(NdefRecord.createUri(automationPayload))
                // Also embed an informative text payload
                records.add(NdefRecord.createTextRecord("en", "Task Trigger: $automationTaskAction"))
            }
            NfcWriteType.NTAG_MIRRORING -> {
                // NXP NTAG213/215/216 UID & Counter ASCII mirror template
                val mirrorUri = mirroringBaseUrl
                    .replace("{UID}", "00000000000000")
                    .replace("{CNT}", "000000")
                records.add(NdefRecord.createUri(mirrorUri))
            }
            NfcWriteType.GAMING_NTAG215 -> {
                val preset = GamingPresetCatalog.PRESETS.find { it.id == selectedGamingPresetId }
                    ?: GamingPresetCatalog.PRESETS.first()
                val mimePayload = "amiibo:${preset.amiiboHexId};series:${preset.gameSeries};name:${preset.name}"
                records.add(NdefRecord.createMime("application/x-amiibo-tag", mimePayload.toByteArray(Charsets.UTF_8)))
                records.add(NdefRecord.createTextRecord("en", "Amiibo: ${preset.name} (${preset.gameSeries})"))
            }
            NfcWriteType.CUSTOM_MIME -> {
                records.add(NdefRecord.createMime(customMimeType, customMimePayload.toByteArray(Charsets.UTF_8)))
            }
            NfcWriteType.RAW_HEX -> {
                val cleanHex = rawHexPayload.replace(" ", "").replace(":", "")
                val bytes = try {
                    cleanHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                } catch (e: Exception) {
                    byteArrayOf(0x00)
                }
                records.add(NdefRecord.createMime("application/octet-stream", bytes))
            }
            NfcWriteType.ERASE_FORMAT -> {
                // Blank 0-byte record for formatting back to factory blank state
                records.add(NdefRecord(NdefRecord.TNF_EMPTY, ByteArray(0), ByteArray(0), ByteArray(0)))
            }
        }

        return NdefMessage(records.toTypedArray())
    }

    private fun buildSocialUrl(platform: String, handle: String): String {
        val clean = handle.trim().removePrefix("@")
        if (clean.startsWith("http://") || clean.startsWith("https://")) return clean
        return when (platform.uppercase()) {
            "INSTAGRAM" -> "https://instagram.com/$clean"
            "LINKEDIN" -> "https://linkedin.com/in/$clean"
            "X_TWITTER", "TWITTER" -> "https://x.com/$clean"
            "YOUTUBE" -> "https://youtube.com/@$clean"
            "TIKTOK" -> "https://tiktok.com/@$clean"
            "WHATSAPP" -> "https://wa.me/${clean.filter { it.isDigit() }}"
            "GITHUB" -> "https://github.com/$clean"
            else -> "https://$clean"
        }
    }
}
