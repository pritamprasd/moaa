package dev.motherofallapps.host.tool.nfc.ui

import android.content.Context
import android.nfc.Tag
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.motherofallapps.host.logging.AppLogHub
import dev.motherofallapps.host.logging.LogLevel
import dev.motherofallapps.host.tool.nfc.manager.NfcManager
import dev.motherofallapps.host.tool.nfc.model.GamingPresetCatalog
import dev.motherofallapps.host.tool.nfc.model.GamingTagPreset
import dev.motherofallapps.host.tool.nfc.model.NdefParsedRecord
import dev.motherofallapps.host.tool.nfc.model.NfcTagData
import dev.motherofallapps.host.tool.nfc.model.NfcWritePayload
import dev.motherofallapps.host.tool.nfc.model.NfcWriteType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class NfcTab(val label: String, val icon: String) {
    READ("SCAN & INSPECT", "📡"),
    WRITE("WRITE STUDIO", "✍️"),
    MEMORY_MAP("SILICON PAGES", "🧠"),
    SECTOR_ANALYZER("MIFARE DECODER", "🔐"),
    GAMING_PRESETS("GAMING / AMIIBO", "🎮"),
    POPULAR_APPS("APPS REFERENCE", "🌟"),
    HISTORY("TAG LOGS", "📜"),
}

data class NfcUiState(
    val selectedTab: NfcTab = NfcTab.READ,
    val hardwareStatus: NfcManager.NfcHardwareStatus = NfcManager.NfcHardwareStatus.AVAILABLE_ENABLED,
    val currentTag: NfcTagData? = null,
    val writePayload: NfcWritePayload = NfcWritePayload(),
    val isWaitingForTagToWrite: Boolean = false,
    val lastWriteResult: String? = null,
    val isWriteSuccess: Boolean = false,
    val tagHistory: List<NfcTagData> = emptyList(),
    val isBatchModeActive: Boolean = false,
    val batchCount: Int = 0,
)

class NfcViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(NfcUiState())
    val uiState: StateFlow<NfcUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            NfcManager.discoveredTagFlow.collect { tag ->
                onTagDiscovered(tag)
            }
        }
    }

    fun refreshHardwareStatus(context: Context) {
        val status = NfcManager.getHardwareStatus(context)
        _uiState.update { it.copy(hardwareStatus = status) }
    }

    fun selectTab(tab: NfcTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun onTagDiscovered(tag: Tag) {
        val tagData = NfcManager.parseTag(tag)

        if (_uiState.value.isWaitingForTagToWrite) {
            // Write Mode
            val result = NfcManager.writeTag(tag, _uiState.value.writePayload)
            if (result.isSuccess) {
                val newBatchCount = _uiState.value.batchCount + 1
                val keepWaiting = _uiState.value.isBatchModeActive

                _uiState.update {
                    it.copy(
                        isWaitingForTagToWrite = keepWaiting,
                        lastWriteResult = result.getOrNull() ?: "Write successful!",
                        isWriteSuccess = true,
                        currentTag = tagData,
                        batchCount = newBatchCount
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isWaitingForTagToWrite = false,
                        lastWriteResult = "Write Error: ${result.exceptionOrNull()?.message}",
                        isWriteSuccess = false
                    )
                }
            }
        } else {
            // Read Mode
            _uiState.update { current ->
                val history = (listOf(tagData) + current.tagHistory.filter { it.uidHex != tagData.uidHex }).take(30)
                current.copy(
                    currentTag = tagData,
                    tagHistory = history
                )
            }
        }
    }

    fun updateWritePayload(payload: NfcWritePayload) {
        _uiState.update { it.copy(writePayload = payload) }
    }

    fun startWaitingForTagToWrite() {
        _uiState.update {
            it.copy(
                isWaitingForTagToWrite = true,
                lastWriteResult = null
            )
        }
        AppLogHub.log("nfc-tool", "NFC Tool", LogLevel.INFO, "NfcViewModel", "Antenna armed: Waiting for NFC tag...")
    }

    fun cancelWaitingForTagToWrite() {
        _uiState.update { it.copy(isWaitingForTagToWrite = false) }
    }

    fun toggleBatchMode(enabled: Boolean) {
        _uiState.update { it.copy(isBatchModeActive = enabled, batchCount = 0) }
    }

    /**
     * Clones the current scanned tag's payload into the Write Studio.
     */
    fun cloneTagToWriter(tagData: NfcTagData) {
        val firstRecord = tagData.records.firstOrNull()
        val payload = when (firstRecord) {
            is NdefParsedRecord.Uri -> NfcWritePayload(writeType = NfcWriteType.URI, uriString = firstRecord.uriString)
            is NdefParsedRecord.Text -> NfcWritePayload(writeType = NfcWriteType.TEXT, textContent = firstRecord.text)
            is NdefParsedRecord.WifiConfig -> NfcWritePayload(
                writeType = NfcWriteType.WIFI,
                wifiSsid = firstRecord.ssid,
                wifiPassword = firstRecord.password,
                wifiAuthType = firstRecord.authType
            )
            is NdefParsedRecord.Contact -> NfcWritePayload(
                writeType = NfcWriteType.CONTACT_VCARD,
                contactName = firstRecord.name,
                contactPhone = firstRecord.phone,
                contactEmail = firstRecord.email,
                contactOrganization = firstRecord.organization
            )
            is NdefParsedRecord.ApplicationLauncher -> NfcWritePayload(
                writeType = NfcWriteType.APP_LAUNCHER,
                appPackageName = firstRecord.packageName
            )
            else -> NfcWritePayload(
                writeType = NfcWriteType.RAW_HEX,
                rawHexPayload = tagData.rawHexDump.ifBlank { "00 01 02 03 04 05" }
            )
        }

        _uiState.update {
            it.copy(
                writePayload = payload,
                selectedTab = NfcTab.WRITE
            )
        }
        AppLogHub.log("nfc-tool", "NFC Tool", LogLevel.INFO, "NfcViewModel", "Cloned tag ${tagData.uidHex} to Write Studio")
    }

    /**
     * Loads a gaming preset into the Write Studio and arms the antenna.
     */
    fun selectGamingPresetAndWrite(preset: GamingTagPreset) {
        val payload = NfcWritePayload(
            writeType = NfcWriteType.GAMING_NTAG215,
            selectedGamingPresetId = preset.id
        )
        _uiState.update {
            it.copy(
                writePayload = payload,
                selectedTab = NfcTab.WRITE
            )
        }
    }

    /**
     * Arms antenna for blank tag erase.
     */
    fun armEraseTag() {
        val erasePayload = NfcWritePayload(writeType = NfcWriteType.ERASE_FORMAT)
        _uiState.update {
            it.copy(
                writePayload = erasePayload,
                isWaitingForTagToWrite = true,
                lastWriteResult = null
            )
        }
        AppLogHub.log("nfc-tool", "NFC Tool", LogLevel.INFO, "NfcViewModel", "Antenna armed: Ready to erase/format NFC tag")
    }

    /**
     * Arms antenna for permanent tag lock.
     */
    fun armLockTag() {
        val lockPayload = _uiState.value.writePayload.copy(makeReadOnly = true)
        _uiState.update {
            it.copy(
                writePayload = lockPayload,
                isWaitingForTagToWrite = true,
                lastWriteResult = null
            )
        }
        AppLogHub.log("nfc-tool", "NFC Tool", LogLevel.WARN, "NfcViewModel", "Antenna armed: Ready to lock NFC tag to Read-Only")
    }

    fun clearTag() {
        _uiState.update { it.copy(currentTag = null) }
    }

    fun clearHistory() {
        _uiState.update { it.copy(tagHistory = emptyList()) }
    }

    /**
     * Loads realistic demo tags for immediate exploration.
     */
    fun loadDemoTag(type: String = "ntag215") {
        val demo = when (type) {
            "ntag215" -> {
                val demoPages = listOf(
                    dev.motherofallapps.host.tool.nfc.model.NfcMemoryPage(0, "04 8F A2 48", "....", dev.motherofallapps.host.tool.nfc.model.NfcMemoryPage.MemoryPageType.HEADER_UID, "UID 0-2 & BCC0 Checksum"),
                    dev.motherofallapps.host.tool.nfc.model.NfcMemoryPage(1, "3B 5C 8D 90", ";\\..", dev.motherofallapps.host.tool.nfc.model.NfcMemoryPage.MemoryPageType.HEADER_UID, "UID 3-6 Serial Number"),
                    dev.motherofallapps.host.tool.nfc.model.NfcMemoryPage(2, "88 48 00 00", ".H..", dev.motherofallapps.host.tool.nfc.model.NfcMemoryPage.MemoryPageType.HEADER_UID, "BCC1 Checksum, Static Lock Bytes"),
                    dev.motherofallapps.host.tool.nfc.model.NfcMemoryPage(3, "E1 10 6D 00", "..m.", dev.motherofallapps.host.tool.nfc.model.NfcMemoryPage.MemoryPageType.CAPABILITY_CONTAINER, "Capability Container (CC) · 504B"),
                    dev.motherofallapps.host.tool.nfc.model.NfcMemoryPage(4, "03 28 D1 01", ".(..", dev.motherofallapps.host.tool.nfc.model.NfcMemoryPage.MemoryPageType.USER_DATA, "NDEF TLV Header & Record 1"),
                    dev.motherofallapps.host.tool.nfc.model.NfcMemoryPage(5, "24 55 04 67", "\$U.g", dev.motherofallapps.host.tool.nfc.model.NfcMemoryPage.MemoryPageType.USER_DATA, "URI Prefix: https://github..."),
                    dev.motherofallapps.host.tool.nfc.model.NfcMemoryPage(6, "69 74 68 75", "ithu", dev.motherofallapps.host.tool.nfc.model.NfcMemoryPage.MemoryPageType.USER_DATA, "URL Payload ASCII string"),
                    dev.motherofallapps.host.tool.nfc.model.NfcMemoryPage(130, "00 00 00 BD", "....", dev.motherofallapps.host.tool.nfc.model.NfcMemoryPage.MemoryPageType.DYNAMIC_LOCK, "Dynamic Lock Bytes"),
                    dev.motherofallapps.host.tool.nfc.model.NfcMemoryPage(131, "FF FF FF FF", "....", dev.motherofallapps.host.tool.nfc.model.NfcMemoryPage.MemoryPageType.CONFIG_AND_PWD, "Configuration Byte (CFG0)"),
                    dev.motherofallapps.host.tool.nfc.model.NfcMemoryPage(132, "00 00 00 00", "....", dev.motherofallapps.host.tool.nfc.model.NfcMemoryPage.MemoryPageType.CONFIG_AND_PWD, "PWD Access Password (32-bit)")
                )

                NfcTagData(
                    uidHex = "04:8F:A2:3B:5C:8D:90",
                    uidDecimal = "1282938491823",
                    reverseUidHex = "90:8D:5C:3B:A2:8F:04",
                    technologies = listOf("Ndef", "NfcA", "MifareUltralight"),
                    tagStandard = "NTAG215 (504 bytes user memory - Amiibo Standard)",
                    icManufacturer = "NXP Semiconductors",
                    memorySizeBytes = 504,
                    maxNdefSizeBytes = 492,
                    isNdefSupported = true,
                    isWritable = true,
                    canMakeReadOnly = true,
                    records = listOf(
                        NdefParsedRecord.Uri("https://github.com/mother-of-all-apps"),
                        NdefParsedRecord.Text("Smart Desk Profile · Wi-Fi Office Link", languageCode = "en"),
                        NdefParsedRecord.WifiConfig(ssid = "Workspace_5G", password = "SecurePassword2026", authType = "WPA2")
                    ),
                    rawHexDump = "00 00 E1 10 3E 00 03 24 D1 01 20 54 02 65 6E ...",
                    memoryPages = demoPages
                )
            }
            else -> {
                val demoSectors = (0..15).map { sec ->
                    val startBlock = sec * 4
                    val endBlock = startBlock + 3
                    dev.motherofallapps.host.tool.nfc.model.MifareSectorInfo(
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
                }

                NfcTagData(
                    uidHex = "1A:3F:82:D4",
                    uidDecimal = "440378068",
                    reverseUidHex = "D4:82:3F:1A",
                    technologies = listOf("MifareClassic", "NfcA"),
                    tagStandard = "MIFARE Classic 1K",
                    icManufacturer = "NXP Semiconductors",
                    memorySizeBytes = 1024,
                    maxNdefSizeBytes = 716,
                    isNdefSupported = true,
                    isWritable = true,
                    canMakeReadOnly = false,
                    records = listOf(
                        NdefParsedRecord.Contact(name = "Alex Vance", phone = "+1 (555) 019-2834", email = "alex@lab.internal", organization = "Black Mesa")
                    ),
                    rawHexDump = "A0 A1 A2 A3 A4 A5 FF 07 80 69 B0 B1 B2 B3 B4 B5",
                    mifareSectors = demoSectors
                )
            }
        }

        _uiState.update { current ->
            current.copy(
                currentTag = demo,
                tagHistory = (listOf(demo) + current.tagHistory.filter { it.uidHex != demo.uidHex }).take(30)
            )
        }
    }
}
