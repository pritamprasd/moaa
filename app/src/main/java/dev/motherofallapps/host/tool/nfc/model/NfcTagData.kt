package dev.motherofallapps.host.tool.nfc.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Detailed representation of a scanned NFC tag.
 */
data class NfcTagData(
    val uidHex: String,
    val uidDecimal: String,
    val reverseUidHex: String,
    val technologies: List<String>,
    val tagStandard: String,
    val icManufacturer: String,
    val memorySizeBytes: Int,
    val maxNdefSizeBytes: Int,
    val isNdefSupported: Boolean,
    val isWritable: Boolean,
    val canMakeReadOnly: Boolean,
    val records: List<NdefParsedRecord>,
    val scannedAtEpochMs: Long = System.currentTimeMillis(),
    val rawHexDump: String = "",
) {
    val formattedScannedTime: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(scannedAtEpochMs))

    val technologiesString: String
        get() = technologies.joinToString(", ")
}
