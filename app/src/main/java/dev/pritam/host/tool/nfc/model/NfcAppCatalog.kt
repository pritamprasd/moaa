package dev.pritam.host.tool.nfc.model

data class NfcAppFeatureInfo(
    val id: String,
    val name: String,
    val developer: String,
    val category: String,
    val description: String,
    val supportedTags: List<String>,
    val coreFunctionalities: List<String>,
    val advancedFeatures: List<String>,
    val limitationOrNotes: String,
    val primaryUseCases: List<String>,
)

object NfcAppCatalog {

    val popularApps: List<NfcAppFeatureInfo> = listOf(
        NfcAppFeatureInfo(
            id = "nfc-tools",
            name = "NFC Tools & NFC Tasks",
            developer = "wakdev",
            category = "General Utility & Automation",
            description = "The most widely used Swiss-Army-knife NFC utility for reading, writing, and executing device automation tasks.",
            supportedTags = listOf("NTAG203/213/215/216", "Mifare Ultralight", "Mifare Classic (NDEF)", "ICODE SLIX", "Topaz", "Sony Felica"),
            coreFunctionalities = listOf(
                "Read & parse standard NDEF records (Text, URL, Contact vCard, SMS, Mail)",
                "Write multi-record NDEF payloads with custom TNF/RTD",
                "Lock tags permanently or with standard 32-bit PWD password protection",
                "Erase / Format tags back to factory blank state",
                "Copy / Clone NDEF payloads from one tag to another"
            ),
            advancedFeatures = listOf(
                "Device Automation via NFC Tasks (toggle Wi-Fi, BT, Hotspot, Flashlight, Volume, Ringer)",
                "Conditional task execution based on variables, time, or battery level",
                "Custom hex payloads and raw NDEF byte stream injection",
                "Integration with Tasker and Home Assistant webhooks"
            ),
            limitationOrNotes = "Cannot read/write proprietary encrypted sectors of Mifare Classic without keys.",
            primaryUseCases = listOf("Smart home triggers", "Digital business cards", "Bedside/car device profile automation")
        ),
        NfcAppFeatureInfo(
            id = "nxp-tagwriter",
            name = "NXP TagWriter",
            developer = "NXP Semiconductors",
            category = "Industrial & Chip Diagnostics",
            description = "Official utility by NXP Semiconductors with in-depth hardware diagnostics, IC architecture inspection, and batch programming.",
            supportedTags = listOf("NTAG (all series)", "ICODE SLIX/DNA", "Mifare Classic 1K/4K", "Mifare Ultralight C/EV1", "Mifare DESFire EV1/EV2/EV3", "Hitag"),
            coreFunctionalities = listOf(
                "Chip Model & Manufacturer identification via silicon response",
                "Comprehensive memory block visualization and sector condition flags",
                "NDEF creation with Wi-Fi Protected Setup (WPS) credentials and Bluetooth pairing records",
                "Batch/Consecutive tag writing for multi-tag inventory deployment"
            ),
            advancedFeatures = listOf(
                "NTAG Mirroring (dynamic UID and tap counters appended into URLs)",
                "NTAG Cryptographic Authentication (AES-128 / SUN / DNA verification)",
                "Configurable lock bytes (Static lock, Dynamic lock, PWD/PACK verification)",
                "Export tag dumps in XML and CSV format"
            ),
            limitationOrNotes = "Requires NXP genuine silicon to leverage SUN/Mirroring hardware features.",
            primaryUseCases = listOf("Product anti-counterfeiting", "Batch asset tagging", "Silicon diagnostics")
        ),
        NfcAppFeatureInfo(
            id = "mifare-classic-tool",
            name = "Mifare Classic Tool (MCT)",
            developer = "ikarus (Open Source)",
            category = "Security & Low-Level Sector Tool",
            description = "Open-source low-level utility to analyze, read, write, and clone Mifare Classic 1K/4K RFID chips using sector keys.",
            supportedTags = listOf("Mifare Classic 1K", "Mifare Classic 4K", "Mifare Classic Mini", "Gen1/Gen2 Magic UID Tags"),
            coreFunctionalities = listOf(
                "Read full 16/64 sector dumps using Key A and Key B authentication",
                "Built-in dictionary attack dictionary keys (std.keys, extended.keys)",
                "Write raw hex blocks and modify sector trailer access conditions",
                "Format Value Blocks for access control / debit token simulation"
            ),
            advancedFeatures = listOf(
                "Clone tag dumps to Gen1a / Gen2 'Magic' rewritable Sector 0 UID cards",
                "Sector access condition decoder & permission matrix calculation",
                "BCC (Block Check Character) auto-calculation for Block 0",
                "Hex diff comparator between multiple tag dumps"
            ),
            limitationOrNotes = "Requires phone NFC chip that supports proprietary 106 kbps Crypto-1 framing (supported on most NXP/Broadcom controllers, partially restricted on some Pixel/Samsung chips).",
            primaryUseCases = listOf("RFID access control auditing", "Transit/gym keycard cloning (authorized)", "Security research")
        ),
        NfcAppFeatureInfo(
            id = "tagmo",
            name = "TagMo",
            developer = "HiddenRamblings (Open Source)",
            category = "Gaming & Amiibo Emulation",
            description = "Specialized backup, verification, and writing tool for NTAG215 gaming tags and action figure data.",
            supportedTags = listOf("NTAG215 (504 bytes user memory strictly required)"),
            coreFunctionalities = listOf(
                "Read and parse 540-byte raw NTAG215 bin dumps",
                "Validate cryptographic fixed keys (unfixed-info and locked-secret signatures)",
                "Write gaming tag binaries with locked Sector 0 and dynamic password locks",
                "Organize, preview, and categorize digital amiibo collections with metadata & artwork"
            ),
            advancedFeatures = listOf(
                "Key file verification (`key_retail.bin` parser)",
                "Serial number (UID) randomize option for multi-scan game rewards",
                "Support for Flipper Zero, Proxmark3, and AmiLoop export formats",
                "Support for Elite / Power Tag rewritable hardware devices"
            ),
            limitationOrNotes = "Only works with NTAG215 tags (NTAG213 and NTAG216 cannot be used for Amiibo).",
            primaryUseCases = listOf("Amiibo backup and restoration", "Gaming NFC asset preservation")
        ),
        NfcAppFeatureInfo(
            id = "nfc-tagify",
            name = "NFC Tagify / Business Connect",
            developer = "NFC Tagify Ltd",
            category = "Digital Identity & Contactless Networking",
            description = "Modern networking and contactless marketing tool focused on digital vCards, social media profile landing links, and review requests.",
            supportedTags = listOf("NTAG213", "NTAG215", "NTAG216"),
            coreFunctionalities = listOf(
                "One-tap Digital Business Cards (vCard 3.0 / 4.0)",
                "Google Review / Trustpilot instant tap-to-rate links",
                "Direct Instagram, LinkedIn, YouTube, TikTok, and WhatsApp profile links",
                "Instant Wi-Fi Guest pass generation for hotels, cafes, and offices"
            ),
            advancedFeatures = listOf(
                "Shortened dynamic URL redirection with click telemetry analytics",
                "Contact card lead capture forms",
                "Cryptographic anti-tamper tag lock mechanism"
            ),
            limitationOrNotes = "Primarily focused on high-level URL/vCard payloads rather than low-level sector manipulation.",
            primaryUseCases = listOf("Events & conferences", "Restaurant table ordering & menus", "Social media growth")
        )
    )
}
