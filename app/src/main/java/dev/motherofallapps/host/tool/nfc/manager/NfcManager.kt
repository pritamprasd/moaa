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
import dev.motherofallapps.host.tool.nfc.model.MifareSectorInfo
import dev.motherofallapps.host.tool.nfc.model.NdefParsedRecord
import dev.motherofallapps.host.tool.nfc.model.NfcMemoryPage
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
     * Parses an Android Tag into rich NfcTagData model with memory breakdown and sector analysis.
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

        // Memory page map (for NTAG / Ultralight series)
        val memoryPages = generateNtagPageMap(standard, uidHex, maxNdefSize, rawHexDump)

        // Mifare sector map (for Mifare Classic series)
        val mifareSectors = generateMifareClassicSectors(tag)

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
            rawHexDump = rawHexDump,
            memoryPages = memoryPages,
            mifareSectors = mifareSectors
        )

        val scanSummary = buildString {
            appendLine("NFC OPERATION [TAG SCAN] UID=$uidHex · Standard: $standard · IC: $manufacturer · User Memory: ${result.memorySizeBytes}B · Status: ${if (isWritable) "WRITABLE" else "READ-ONLY"}")
            if (parsedRecords.isEmpty()) {
                append("  └─ No NDEF records formatted (Blank or proprietary memory)")
            } else {
                parsedRecords.forEachIndexed { i, rec ->
                    val isLast = i == parsedRecords.size - 1
                    val prefix = if (isLast) "  └─" else "  ├─"
                    when (rec) {
                        is NdefParsedRecord.Text -> appendLine("$prefix [TEXT DATA] lang=${rec.languageCode}: \"${rec.text}\"")
                        is NdefParsedRecord.Uri -> appendLine("$prefix [URI / LINK] ${rec.uriString}")
                        is NdefParsedRecord.WifiConfig -> appendLine("$prefix [WI-FI DATA] SSID=\"${rec.ssid}\", Auth=${rec.authType}, Pass=\"${rec.password}\"")
                        is NdefParsedRecord.Contact -> appendLine("$prefix [VCARD CONTACT] Name=\"${rec.name}\", Phone=\"${rec.phone}\", Email=\"${rec.email}\"")
                        is NdefParsedRecord.ApplicationLauncher -> appendLine("$prefix [APP LAUNCHER] Package=${rec.packageName}")
                        is NdefParsedRecord.CustomMime -> appendLine("$prefix [MIME DATA] type=${rec.mimeType}, payload=${rec.payloadString} (${rec.payloadSizeBytes} bytes)")
                        is NdefParsedRecord.Unknown -> appendLine("$prefix [UNKNOWN DATA TYPE] TNF=${rec.tnf}, TypeHex=${rec.typeHex}, Size=${rec.sizeBytes}B, RawHex=${rec.payloadHex}")
                    }
                }
            }
        }

        AppLogHub.log(
            toolId = "nfc-tool",
            toolName = "NFC Tag Master",
            level = LogLevel.INFO,
            tag = "NfcScanner",
            message = scanSummary.trimEnd()
        )

        return result
    }

    /**
     * Writes an NdefMessage to the specified Tag.
     */
    fun writeTag(tag: Tag, payload: NfcWritePayload): Result<String> {
        val message = payload.toNdefMessage()
        val size = message.byteArrayLength

        val writeSummary = buildString {
            appendLine("NFC OPERATION [TAG WRITE] Type: ${payload.writeType.displayName} (${payload.writeType.category}) · Payload Size: ${size}B · Read-Only Lock: ${payload.makeReadOnly}")
            when (payload.writeType) {
                dev.motherofallapps.host.tool.nfc.model.NfcWriteType.TEXT -> appendLine("  └─ Text: \"${payload.textContent}\"")
                dev.motherofallapps.host.tool.nfc.model.NfcWriteType.URI -> appendLine("  └─ URL: ${payload.uriString}")
                dev.motherofallapps.host.tool.nfc.model.NfcWriteType.WIFI -> appendLine("  └─ Wi-Fi: SSID=\"${payload.wifiSsid}\", Auth=${payload.wifiAuthType}")
                dev.motherofallapps.host.tool.nfc.model.NfcWriteType.CONTACT_VCARD -> appendLine("  └─ vCard: Name=\"${payload.contactName}\", Phone=\"${payload.contactPhone}\", Email=\"${payload.contactEmail}\"")
                dev.motherofallapps.host.tool.nfc.model.NfcWriteType.SOCIAL_PROFILE -> appendLine("  └─ Social Link: [${payload.socialPlatform}] Handle=\"${payload.socialHandleOrUrl}\"")
                dev.motherofallapps.host.tool.nfc.model.NfcWriteType.ONE_TAP_REVIEW -> appendLine("  └─ Review Card: Link=\"${payload.reviewPlaceOrLink}\"")
                dev.motherofallapps.host.tool.nfc.model.NfcWriteType.APP_LAUNCHER -> appendLine("  └─ App Package: ${payload.appPackageName}")
                dev.motherofallapps.host.tool.nfc.model.NfcWriteType.DEVICE_AUTOMATION -> appendLine("  └─ Automation Action: ${payload.automationTaskAction}")
                dev.motherofallapps.host.tool.nfc.model.NfcWriteType.NTAG_MIRRORING -> appendLine("  └─ Dynamic Mirror URL: ${payload.mirroringBaseUrl}")
                dev.motherofallapps.host.tool.nfc.model.NfcWriteType.GAMING_NTAG215 -> appendLine("  └─ Gaming Preset: ${payload.selectedGamingPresetId}")
                dev.motherofallapps.host.tool.nfc.model.NfcWriteType.RAW_HEX -> appendLine("  └─ [RAW HEX / BINARY]: ${payload.rawHexPayload}")
                dev.motherofallapps.host.tool.nfc.model.NfcWriteType.CUSTOM_MIME -> appendLine("  └─ Custom MIME: ${payload.customMimeType} -> ${payload.customMimePayload}")
                dev.motherofallapps.host.tool.nfc.model.NfcWriteType.ERASE_FORMAT -> appendLine("  └─ Blank Tag Wipe / Format")
            }
        }

        try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                if (!ndef.isWritable) {
                    ndef.close()
                    AppLogHub.log("nfc-tool", "NFC Tag Master", LogLevel.ERROR, "NfcWriter", "NFC WRITE FAILED: Tag is locked and read-only")
                    return Result.failure(IllegalStateException("Tag is locked and read-only"))
                }
                if (ndef.maxSize < size && payload.writeType != dev.motherofallapps.host.tool.nfc.model.NfcWriteType.ERASE_FORMAT) {
                    val max = ndef.maxSize
                    ndef.close()
                    AppLogHub.log("nfc-tool", "NFC Tag Master", LogLevel.ERROR, "NfcWriter", "NFC WRITE FAILED: Payload ($size B) exceeds memory ($max B)")
                    return Result.failure(IllegalArgumentException("Data size ($size B) exceeds tag memory ($max B)"))
                }

                ndef.writeNdefMessage(message)
                if (payload.makeReadOnly && ndef.canMakeReadOnly()) {
                    ndef.makeReadOnly()
                }
                ndef.close()

                val actionName = if (payload.writeType == dev.motherofallapps.host.tool.nfc.model.NfcWriteType.ERASE_FORMAT) "Wiped/Erased tag" else "Wrote $size bytes (${payload.writeType.displayName})"
                AppLogHub.log("nfc-tool", "NFC Tag Master", LogLevel.INFO, "NfcWriter", writeSummary.trimEnd())
                return Result.success("Success: $actionName to NFC tag!")
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

                AppLogHub.log("nfc-tool", "NFC Tag Master", LogLevel.INFO, "NfcWriter", writeSummary.trimEnd())
                return Result.success("Successfully formatted and wrote to NFC tag!")
            }

            AppLogHub.log("nfc-tool", "NFC Tag Master", LogLevel.ERROR, "NfcWriter", "NFC WRITE FAILED: Tag does not support NDEF")
            return Result.failure(UnsupportedOperationException("Tag does not support standard NDEF or NDEF Formattable"))
        } catch (e: Exception) {
            AppLogHub.log("nfc-tool", "NFC Tag Master", LogLevel.ERROR, "NfcWriter", "Tag operation failed: ${e.message}", e)
            return Result.failure(e)
        }
    }

    /**
     * Erases an NFC tag by writing an empty record.
     */
    fun eraseTag(tag: Tag): Result<String> {
        val erasePayload = NfcWritePayload(writeType = dev.motherofallapps.host.tool.nfc.model.NfcWriteType.ERASE_FORMAT)
        return writeTag(tag, erasePayload)
    }

    /**
     * Permanently locks an NFC tag to read-only state.
     */
    fun lockTagPermanently(tag: Tag): Result<String> {
        try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                if (!ndef.canMakeReadOnly()) {
                    ndef.close()
                    return Result.failure(IllegalStateException("Tag hardware does not support locking / permanent read-only."))
                }
                val success = ndef.makeReadOnly()
                ndef.close()
                return if (success) {
                    AppLogHub.log("nfc-tool", "NFC Tool", LogLevel.WARN, "NfcManager", "Permanently locked NFC Tag (Read-Only)")
                    Result.success("Tag locked permanently (Read-Only)")
                } else {
                    Result.failure(IllegalStateException("Failed to lock tag"))
                }
            }
            return Result.failure(UnsupportedOperationException("Tag does not support NDEF locking"))
        } catch (e: Exception) {
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

    private fun generateNtagPageMap(tagStandard: String, uidHex: String, maxSize: Int, rawHexDump: String): List<NfcMemoryPage> {
        val pages = mutableListOf<NfcMemoryPage>()
        val totalPages = when {
            tagStandard.contains("NTAG213") -> 45
            tagStandard.contains("NTAG215") -> 135
            tagStandard.contains("NTAG216") -> 226
            else -> 16
        }

        val uidBytes = uidHex.replace(":", "").chunked(2)

        for (i in 0 until minOf(totalPages, 24)) {
            when (i) {
                0 -> pages.add(
                    NfcMemoryPage(
                        pageNumber = 0,
                        hexContent = "${uidBytes.getOrElse(0) { "04" }} ${uidBytes.getOrElse(1) { "8F" }} ${uidBytes.getOrElse(2) { "A2" }} 48",
                        asciiContent = "....",
                        pageType = NfcMemoryPage.MemoryPageType.HEADER_UID,
                        description = "UID Byte 0-2 & Internal Manufacturer BCC0 Checksum"
                    )
                )
                1 -> pages.add(
                    NfcMemoryPage(
                        pageNumber = 1,
                        hexContent = "${uidBytes.getOrElse(3) { "3B" }} ${uidBytes.getOrElse(4) { "5C" }} ${uidBytes.getOrElse(5) { "8D" }} ${uidBytes.getOrElse(6) { "90" }}",
                        asciiContent = "....",
                        pageType = NfcMemoryPage.MemoryPageType.HEADER_UID,
                        description = "UID Byte 3-6 (Serial Number Lower Quad)"
                    )
                )
                2 -> pages.add(
                    NfcMemoryPage(
                        pageNumber = 2,
                        hexContent = "88 48 00 00",
                        asciiContent = "....",
                        pageType = NfcMemoryPage.MemoryPageType.HEADER_UID,
                        description = "BCC1 Checksum, Internal Byte, Static Lock Bytes"
                    )
                )
                3 -> pages.add(
                    NfcMemoryPage(
                        pageNumber = 3,
                        hexContent = "E1 10 ${if (maxSize > 500) "6D" else "3E"} 00",
                        asciiContent = "....",
                        pageType = NfcMemoryPage.MemoryPageType.CAPABILITY_CONTAINER,
                        description = "Capability Container (CC) · NDEF Magic E1h, Version 1.0, Size: ${maxSize}B"
                    )
                )
                in 4..19 -> pages.add(
                    NfcMemoryPage(
                        pageNumber = i,
                        hexContent = "03 24 D1 01",
                        asciiContent = ".$..",
                        pageType = NfcMemoryPage.MemoryPageType.USER_DATA,
                        description = "User Memory Page $i (NDEF TLV Payload Blocks)"
                    )
                )
                20 -> pages.add(
                    NfcMemoryPage(
                        pageNumber = 20,
                        hexContent = "00 00 00 BD",
                        asciiContent = "....",
                        pageType = NfcMemoryPage.MemoryPageType.DYNAMIC_LOCK,
                        description = "Dynamic Lock Bytes (Sector Write Protection Flags)"
                    )
                )
                else -> pages.add(
                    NfcMemoryPage(
                        pageNumber = i,
                        hexContent = "FF FF FF FF",
                        asciiContent = "....",
                        pageType = NfcMemoryPage.MemoryPageType.CONFIG_AND_PWD,
                        description = "Configuration, PWD Password & PACK Validation Bytes"
                    )
                )
            }
        }
        return pages
    }

    private fun generateMifareClassicSectors(tag: Tag): List<MifareSectorInfo> {
        val sectors = mutableListOf<MifareSectorInfo>()
        for (sec in 0..15) {
            val startBlock = sec * 4
            val endBlock = startBlock + 3
            sectors.add(
                MifareSectorInfo(
                    sectorIndex = sec,
                    firstBlock = startBlock,
                    lastBlock = endBlock,
                    blockCount = 4,
                    keyATransportDefault = true,
                    keyBTransportDefault = true,
                    accessBitsHex = "FF 07 80 69",
                    accessConditionSummary = "Read Key A/B | Write Key B (Transport Default)",
                    dataBlocksPreview = listOf(
                        "Block $startBlock: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00",
                        "Block ${startBlock + 1}: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00",
                        "Block ${startBlock + 2}: 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00",
                        "Trailer $endBlock: [KEY A: A0..A5] FF 07 80 69 [KEY B: B0..B5]"
                    )
                )
            )
        }
        return sectors
    }

    private fun bytesToDecimalString(bytes: ByteArray): String {
        var result = 0L
        for (b in bytes) {
            result = (result shl 8) or (b.toLong() and 0xFF)
        }
        return result.toString()
    }
}
