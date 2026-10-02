package dev.motherofallapps.host.tool.nfc.model

sealed interface NdefParsedRecord {
    val recordType: String
    val summary: String

    data class Text(
        val text: String,
        val languageCode: String = "en",
        val isUtf8: Boolean = true,
    ) : NdefParsedRecord {
        override val recordType: String = "Text"
        override val summary: String = text
    }

    data class Uri(
        val uriString: String,
    ) : NdefParsedRecord {
        override val recordType: String = "URI / Web Link"
        override val summary: String = uriString
    }

    data class WifiConfig(
        val ssid: String,
        val authType: String = "WPA2-Personal",
        val password: String = "",
        val isHidden: Boolean = false,
    ) : NdefParsedRecord {
        override val recordType: String = "Wi-Fi Network"
        override val summary: String = "SSID: $ssid ($authType)"
    }

    data class Contact(
        val name: String,
        val phone: String = "",
        val email: String = "",
        val organization: String = "",
    ) : NdefParsedRecord {
        override val recordType: String = "vCard Contact"
        override val summary: String = "$name · $phone"
    }

    data class ApplicationLauncher(
        val packageName: String,
    ) : NdefParsedRecord {
        override val recordType: String = "Android App Launcher (AAR)"
        override val summary: String = "Package: $packageName"
    }

    data class CustomMime(
        val mimeType: String,
        val payloadString: String,
        val payloadSizeBytes: Int,
    ) : NdefParsedRecord {
        override val recordType: String = "MIME ($mimeType)"
        override val summary: String = "$mimeType ($payloadSizeBytes bytes)"
    }

    data class Unknown(
        val tnf: Short,
        val typeHex: String,
        val payloadHex: String,
        val sizeBytes: Int,
    ) : NdefParsedRecord {
        override val recordType: String = "Raw Binary Record"
        override val summary: String = "Type: $typeHex ($sizeBytes bytes)"
    }
}
