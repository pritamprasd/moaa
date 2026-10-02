package dev.motherofallapps.host.tool.nfc.ui

import android.content.Context
import android.nfc.Tag
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.motherofallapps.host.logging.AppLogHub
import dev.motherofallapps.host.logging.LogLevel
import dev.motherofallapps.host.tool.nfc.manager.NfcManager
import dev.motherofallapps.host.tool.nfc.model.NdefParsedRecord
import dev.motherofallapps.host.tool.nfc.model.NfcTagData
import dev.motherofallapps.host.tool.nfc.model.NfcWritePayload
import dev.motherofallapps.host.tool.nfc.model.NfcWriteType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class NfcTab {
    READ,
    WRITE,
    POPULAR_APPS,
    HISTORY,
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
                _uiState.update {
                    it.copy(
                        isWaitingForTagToWrite = false,
                        lastWriteResult = result.getOrNull() ?: "Write successful!",
                        isWriteSuccess = true,
                        currentTag = tagData
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
        AppLogHub.log("nfc-tool", "NFC Tool", LogLevel.INFO, "NfcViewModel", "Antenna armed: Waiting for NFC tag to write...")
    }

    fun cancelWaitingForTagToWrite() {
        _uiState.update { it.copy(isWaitingForTagToWrite = false) }
    }

    fun clearTag() {
        _uiState.update { it.copy(currentTag = null) }
    }

    fun clearHistory() {
        _uiState.update { it.copy(tagHistory = emptyList()) }
    }

    /**
     * Loads a realistic demo tag for immediate exploration.
     */
    fun loadDemoTag(type: String = "ntag215") {
        val demo = when (type) {
            "ntag215" -> NfcTagData(
                uidHex = "04:8F:A2:3B:5C:8D:90",
                uidDecimal = "1282938491823",
                reverseUidHex = "90:8D:5C:3B:A2:8F:04",
                technologies = listOf("Ndef", "NfcA", "MifareUltralight"),
                tagStandard = "NTAG215 (504 bytes user memory)",
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
                rawHexDump = "00 00 E1 10 3E 00 03 24 D1 01 20 54 02 65 6E ..."
            )
            else -> NfcTagData(
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
                rawHexDump = "A0 A1 A2 A3 A4 A5 FF 07 80 69 B0 B1 B2 B3 B4 B5"
            )
        }

        _uiState.update { current ->
            current.copy(
                currentTag = demo,
                tagHistory = (listOf(demo) + current.tagHistory).take(30)
            )
        }
    }
}
