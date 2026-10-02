package dev.motherofallapps.host.tool.nfc.model

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import java.nio.charset.Charset
import java.util.Locale

enum class NfcWriteType {
    TEXT,
    URI,
    WIFI,
    CONTACT_VCARD,
    APP_LAUNCHER,
    CUSTOM_MIME,
}

data class NfcWritePayload(
    val writeType: NfcWriteType = NfcWriteType.TEXT,
    val textContent: String = "Hello from MotherOfAllApps NFC!",
    val uriString: String = "https://github.com",
    val wifiSsid: String = "MyHomeWiFi",
    val wifiPassword: String = "",
    val wifiAuthType: String = "WPA2", // OPEN, WPA, WPA2, WPA3
    val contactName: String = "John Doe",
    val contactPhone: String = "+1 555-0199",
    val contactEmail: String = "john@example.com",
    val appPackageName: String = "com.google.android.youtube",
    val customMimeType: String = "application/vnd.custom.data",
    val customMimePayload: String = "{\"device\":\"android\",\"status\":\"active\"}",
    val makeReadOnly: Boolean = false,
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
                val record = NdefRecord.createUri(uriString)
                records.add(record)
            }
            NfcWriteType.WIFI -> {
                // Standard Wi-Fi Simple Configuration (WSC) MIME representation or Wi-Fi URI format
                val wifiConfigPayload = "WIFI:S:$wifiSsid;T:$wifiAuthType;P:$wifiPassword;;"
                val record = NdefRecord.createMime("application/vnd.wfa.wsc", wifiConfigPayload.toByteArray(Charsets.UTF_8))
                records.add(record)
            }
            NfcWriteType.CONTACT_VCARD -> {
                val vCard = buildString {
                    appendLine("BEGIN:VCARD")
                    appendLine("VERSION:3.0")
                    appendLine("N:;$contactName;;;")
                    appendLine("FN:$contactName")
                    if (contactPhone.isNotBlank()) appendLine("TEL;TYPE=CELL:$contactPhone")
                    if (contactEmail.isNotBlank()) appendLine("EMAIL:$contactEmail")
                    appendLine("END:VCARD")
                }
                val record = NdefRecord.createMime("text/vcard", vCard.toByteArray(Charsets.UTF_8))
                records.add(record)
            }
            NfcWriteType.APP_LAUNCHER -> {
                val record = NdefRecord.createApplicationRecord(appPackageName)
                records.add(record)
            }
            NfcWriteType.CUSTOM_MIME -> {
                val record = NdefRecord.createMime(customMimeType, customMimePayload.toByteArray(Charsets.UTF_8))
                records.add(record)
            }
        }

        return NdefMessage(records.toTypedArray())
    }
}
