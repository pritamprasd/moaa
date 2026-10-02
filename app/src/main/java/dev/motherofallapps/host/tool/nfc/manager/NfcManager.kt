package dev.motherofallapps.host.tool.nfc.manager

import android.content.Context
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.nfc.tech.NfcF
import android.nfc.tech.NfcV
import dev.motherofallapps.host.logging.AppLogHub
import dev.motherofallapps.host.logging.LogLevel
import dev.motherofallapps.host.tool.nfc.model.NdefParsedRecord
import dev.motherofallapps.host.tool.nfc.model.NfcTagData
import dev.motherofallapps.host.tool.nfc.model.NfcWritePayload
import java.nio.charset.Charset
import java.util.Locale

object NfcManager {

    enum class NfcHardwareStatus {
        AVAILABLE_ENABLED,
        AVAILABLE_DISABLED,
        NOT_SUPPORTED,
    }

    private val _discoveredTagFlow = kotlinx.coroutines.flow.MutableSharedFlow<Tag>(extraBufferCapacity = 1)
    val discoveredTagFlow: kotlinx.coroutines.flow.SharedFlow<Tag> = _discoveredTagFlow

    fun notifyTagDiscovered(tag: Tag) {
        _discoveredTagFlow.tryEmit(tag)
    }

    fun getHardwareStatus(context: Context): NfcHardwareStatus {
        val adapter = NfcAdapter.getDefaultAdapter(context) ?: return NfcHardwareStatus.NOT_SUPPORTED
        return if (adapter.isEnabled) NfcHardwareStatus.AVAILABLE_ENABLED else NfcHardwareStatus.AVAILABLE_DISABLED
    }

    /**
     * Parses an Android Tag into rich NfcTagData model.
     */
    fun parseTag(tag: Tag): NfcTagData {
        val idBytes = tag.id
        val uidHex = idBytes.joinToString(":") { "%02X".format(it) }
        val reverseUidHex = idBytes.reversedArray().joinToString(":") { "%02X".format(it) }
        val uidDecimal = bytesToDecimalString(idBytes)

        val techList = tag.techList.map { it.substringAfterLast(".") }
        var isNdef = false
        var isWritable = false
        var canMakeReadOnly = false
        var memorySize = 0
        var maxNdefSize = 0
        val parsedRecords = mutableListOf<NdefParsedRecord>()
        var rawHexDump = ""

        // Check NDEF Tech
        val ndef = Ndef.get(tag)
        if (ndef != null) {
            isNdef = true
            try {
                ndef.connect()
                isWritable = ndef.isWritable
                canMakeReadOnly = ndef.canMakeReadOnly()
                maxNdefSize = ndef.maxSize
                val ndefMessage = ndef.ndefMessage
                if (ndefMessage != null) {
                    memorySize = ndefMessage.byteArrayLength
                    parsedRecords.addAll(parseNdefRecords(ndefMessage))
                    rawHexDump = ndefMessage.toByteArray().joinToString(" ") { "%02X".format(it) }
                }
                ndef.close()
            } catch (e: Exception) {
                AppLogHub.log("nfc-tool", "NFC Tool", LogLevel.WARN, "NfcManager", "NDEF read error: ${e.message}")
            }
        }

        // Tag standard and manufacturer identification
        val (standard, manufacturer) = identifyTagModel(tag, uidHex, maxNdefSize)

        val result = NfcTagData(
            uidHex = uidHex,
            uidDecimal = uidDecimal,
            reverseUidHex = reverseUidHex,
            technologies = techList,
            tagStandard = standard,
            icManufacturer = manufacturer,
            memorySizeBytes = if (memorySize > 0) memorySize else maxNdefSize,
            maxNdefSizeBytes = maxNdefSize,
            isNdefSupported = isNdef,
            isWritable = isWritable,
            canMakeReadOnly = canMakeReadOnly,
            records = parsedRecords,
            rawHexDump = rawHexDump
        )

        AppLogHub.log(
            toolId = "nfc-tool",
            toolName = "NFC Tool",
            level = LogLevel.INFO,
            tag = "NfcManager",
            message = "Scanned NFC Tag UID: $uidHex [$standard] (${parsedRecords.size} records)"
        )

        return result
    }

    /**
     * Writes an NdefMessage to the specified Tag.
     */
    fun writeTag(tag: Tag, payload: NfcWritePayload): Result<String> {
        val message = payload.toNdefMessage()
        val size = message.byteArrayLength

        try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                if (!ndef.isWritable) {
                    ndef.close()
                    return Result.failure(IllegalStateException("Tag is locked and read-only"))
                }
                if (ndef.maxSize < size) {
                    val max = ndef.maxSize
                    ndef.close()
                    return Result.failure(IllegalArgumentException("Data size ($size B) exceeds tag memory ($max B)"))
                }

                ndef.writeNdefMessage(message)
                if (payload.makeReadOnly && ndef.canMakeReadOnly()) {
                    ndef.makeReadOnly()
                }
                ndef.close()

                AppLogHub.log("nfc-tool", "NFC Tool", LogLevel.INFO, "NfcManager", "Successfully wrote $size bytes to NDEF tag")
                return Result.success("Successfully wrote $size bytes to NFC tag!")
            }

            // Try formatting unformatted tag (NdefFormatable)
            val formattable = NdefFormatable.get(tag)
            if (formattable != null) {
                formattable.connect()
                if (payload.makeReadOnly) {
                    formattable.formatReadOnly(message)
                } else {
                    formattable.format(message)
                }
                formattable.close()

                AppLogHub.log("nfc-tool", "NFC Tool", LogLevel.INFO, "NfcManager", "Formatted and wrote $size bytes to unformatted tag")
                return Result.success("Successfully formatted and wrote $size bytes to NFC tag!")
            }

            return Result.failure(UnsupportedOperationException("Tag does not support NDEF or NDEF Formattable"))
        } catch (e: Exception) {
            AppLogHub.log("nfc-tool", "NFC Tool", LogLevel.ERROR, "NfcManager", "Tag write failed: ${e.message}", e)
            return Result.failure(e)
        }
    }

    private fun parseNdefRecords(message: NdefMessage): List<NdefParsedRecord> {
        val result = mutableListOf<NdefParsedRecord>()

        for (record in message.records) {
            val tnf = record.tnf
            val type = record.type
            val payload = record.payload

            when {
                // Text Record
                tnf == NdefRecord.TNF_WELL_KNOWN && type.contentEquals(NdefRecord.RTD_TEXT) -> {
                    try {
                        val statusByte = payload[0].toInt()
                        val isUtf8 = (statusByte and 0x80) == 0
                        val langLength = statusByte and 0x3F
                        val lang = String(payload, 1, langLength, Charsets.US_ASCII)
                        val encoding = if (isUtf8) Charsets.UTF_8 else Charsets.UTF_16
                        val text = String(payload, 1 + langLength, payload.size - 1 - langLength, encoding)
                        result.add(NdefParsedRecord.Text(text = text, languageCode = lang, isUtf8 = isUtf8))
                    } catch (e: Exception) {
                        result.add(NdefParsedRecord.Text(text = String(payload, Charsets.UTF_8)))
                    }
                }
                // URI Record
                tnf == NdefRecord.TNF_WELL_KNOWN && type.contentEquals(NdefRecord.RTD_URI) -> {
                    val uri = parseUriRecord(payload)
                    result.add(NdefParsedRecord.Uri(uriString = uri))
                }
                // Application Launch Record
                tnf == NdefRecord.TNF_EXTERNAL_TYPE && String(type, Charsets.US_ASCII) == "android.com:pkg" -> {
                    val pkg = String(payload, Charsets.UTF_8)
                    result.add(NdefParsedRecord.ApplicationLauncher(packageName = pkg))
                }
                // MIME: Wi-Fi
                tnf == NdefRecord.TNF_MIME_MEDIA && String(type, Charsets.US_ASCII) == "application/vnd.wfa.wsc" -> {
                    val content = String(payload, Charsets.UTF_8)
                    result.add(parseWifiRecord(content))
                }
                // MIME: vCard
                tnf == NdefRecord.TNF_MIME_MEDIA && (String(type, Charsets.US_ASCII) == "text/vcard" || String(type, Charsets.US_ASCII) == "text/x-vcard") -> {
                    val vcardText = String(payload, Charsets.UTF_8)
                    result.add(parseVCard(vcardText))
                }
                // Generic MIME
                tnf == NdefRecord.TNF_MIME_MEDIA -> {
                    val mime = String(type, Charsets.US_ASCII)
                    val str = try { String(payload, Charsets.UTF_8) } catch (e: Exception) { "<binary data>" }
                    result.add(NdefParsedRecord.CustomMime(mimeType = mime, payloadString = str, payloadSizeBytes = payload.size))
                }
                else -> {
                    val typeHex = type.joinToString("") { "%02X".format(it) }
                    val payloadHex = payload.take(32).joinToString(" ") { "%02X".format(it) }
                    result.add(NdefParsedRecord.Unknown(tnf = tnf, typeHex = typeHex, payloadHex = payloadHex, sizeBytes = payload.size))
                }
            }
        }
        return result
    }

    private fun parseUriRecord(payload: ByteArray): String {
        if (payload.isEmpty()) return ""
        val prefixCode = payload[0].toInt()
        val prefixes = listOf(
            "", "http://www.", "https://www.", "http://", "https://",
            "tel:", "mailto:", "ftp://anonymous:anonymous@", "ftp://ftp.",
            "ftps://", "sftp://", "smb://", "nfs://", "ftp://", "dav://",
            "news:", "telnet://", "imap:", "rtsp://", "urn:", "pop:",
            "sip:", "sips:", "tftp:", "btspp://", "btl2cap://", "btgoep://",
            "tcpobex://", "irdaobex://", "file://", "urn:epc:id:", "urn:epc:tag:",
            "urn:epc:pat:", "urn:epc:raw:", "urn:epc:", "urn:nfc:"
        )
        val prefix = if (prefixCode in prefixes.indices) prefixes[prefixCode] else ""
        val rest = String(payload, 1, payload.size - 1, Charsets.UTF_8)
        return prefix + rest
    }

    private fun parseWifiRecord(content: String): NdefParsedRecord.WifiConfig {
        var ssid = "Unknown"
        var pass = ""
        var auth = "WPA2"

        val parts = content.split(";")
        for (p in parts) {
            val trimmed = p.trim()
            if (trimmed.startsWith("WIFI:S:") || trimmed.startsWith("S:")) {
                ssid = trimmed.substringAfter("S:")
            } else if (trimmed.startsWith("P:")) {
                pass = trimmed.substringAfter("P:")
            } else if (trimmed.startsWith("T:")) {
                auth = trimmed.substringAfter("T:")
            }
        }
        return NdefParsedRecord.WifiConfig(ssid = ssid, password = pass, authType = auth)
    }

    private fun parseVCard(vcard: String): NdefParsedRecord.Contact {
        var name = "Contact"
        var phone = ""
        var email = ""
        var org = ""

        vcard.lines().forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("FN:") -> name = trimmed.removePrefix("FN:")
                trimmed.startsWith("TEL") -> phone = trimmed.substringAfter(":")
                trimmed.startsWith("EMAIL") -> email = trimmed.substringAfter(":")
                trimmed.startsWith("ORG:") -> org = trimmed.removePrefix("ORG:")
            }
        }
        return NdefParsedRecord.Contact(name = name, phone = phone, email = email, organization = org)
    }

    private fun identifyTagModel(tag: Tag, uidHex: String, maxSize: Int): Pair<String, String> {
        val tech = tag.techList.map { it.substringAfterLast(".") }

        return when {
            tech.contains("MifareClassic") -> "MIFARE Classic 1K / 4K" to "NXP Semiconductors"
            tech.contains("MifareUltralight") -> {
                when {
                    maxSize in 130..150 -> "NTAG213 (144 bytes)" to "NXP Semiconductors"
                    maxSize in 490..520 -> "NTAG215 (504 bytes - Amiibo Standard)" to "NXP Semiconductors"
                    maxSize in 870..900 -> "NTAG216 (888 bytes)" to "NXP Semiconductors"
                    else -> "Mifare Ultralight / NTAG Series" to "NXP Semiconductors"
                }
            }
            tech.contains("IsoDep") -> "ISO 14443-4 Type A/B (DESFire / Smart Card)" to "NXP / Infineon"
            tech.contains("NfcV") -> "ISO 15693 (Vicinity Tag / ICODE)" to "NXP / STMicroelectronics"
            tech.contains("NfcF") -> "Sony FeliCa (JIS X 6319-4)" to "Sony Corporation"
            else -> "ISO 14443 Type A Standard Tag" to "Generic NFC"
        }
    }

    private fun bytesToDecimalString(bytes: ByteArray): String {
        var result = 0L
        for (b in bytes) {
            result = (result shl 8) or (b.toLong() and 0xFF)
        }
        return result.toString()
    }
}
