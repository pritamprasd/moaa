package dev.motherofallapps.host.tool.nfc.model

/**
 * Memory page representation for NXP NTAG / Ultralight series.
 */
data class NfcMemoryPage(
    val pageNumber: Int,
    val hexContent: String,
    val asciiContent: String,
    val pageType: MemoryPageType,
    val description: String,
) {
    enum class MemoryPageType {
        HEADER_UID,
        CAPABILITY_CONTAINER,
        USER_DATA,
        DYNAMIC_LOCK,
        CONFIG_AND_PWD,
    }
}

/**
 * Sector layout representation for Mifare Classic 1K/4K RFID chips.
 */
data class MifareSectorInfo(
    val sectorIndex: Int,
    val firstBlock: Int,
    val lastBlock: Int,
    val blockCount: Int,
    val keyATransportDefault: Boolean,
    val keyBTransportDefault: Boolean,
    val accessBitsHex: String,
    val accessConditionSummary: String,
    val dataBlocksPreview: List<String>,
)

/**
 * Gaming / Amiibo preset model for NTAG215 write payloads.
 */
data class GamingTagPreset(
    val id: String,
    val name: String,
    val gameSeries: String,
    val amiiboSeries: String,
    val amiiboHexId: String,
    val description: String,
    val accentColorHex: Long,
)

object GamingPresetCatalog {
    val PRESETS = listOf(
        GamingTagPreset(
            id = "zelda-link-totk",
            name = "Link (Tears of the Kingdom)",
            gameSeries = "The Legend of Zelda",
            amiiboSeries = "Zelda Series",
            amiiboHexId = "0100000000380002",
            description = "Unlocks Champion's Tunic Paraglider Fabric, Knight's Broadsword, and Soldier's Bow in TotK.",
            accentColorHex = 0xFF38BDF8
        ),
        GamingTagPreset(
            id = "zelda-zelda-botw",
            name = "Zelda (Breath of the Wild)",
            gameSeries = "The Legend of Zelda",
            amiiboSeries = "Zelda Series",
            amiiboHexId = "01010000002D0002",
            description = "Unlocks Radiant Shield, Star Fragment materials, and Royal Bow in BotW and TotK.",
            accentColorHex = 0xFFF472B6
        ),
        GamingTagPreset(
            id = "mario-odyssey-wedding",
            name = "Mario (Wedding Outfit)",
            gameSeries = "Super Mario",
            amiiboSeries = "Super Mario Odyssey",
            amiiboHexId = "0000000003830002",
            description = "Grants temporary invulnerability and wedding tuxedo costume in Super Mario Odyssey.",
            accentColorHex = 0xFFF87171
        ),
        GamingTagPreset(
            id = "smash-sephiroth",
            name = "Sephiroth (Smash Ultimate)",
            gameSeries = "Super Smash Bros",
            amiiboSeries = "Super Smash Bros Series",
            amiiboHexId = "0014000003A40002",
            description = "Trainable FP AI fighter with Octaslash and One-Winged Angel mechanics in SSBU.",
            accentColorHex = 0xFFA78BFA
        ),
        GamingTagPreset(
            id = "metroid-samus-dread",
            name = "Samus (Metroid Dread)",
            gameSeries = "Metroid",
            amiiboSeries = "Metroid Series",
            amiiboHexId = "0200000003A00002",
            description = "Grants an extra permanent Energy Tank and 200 daily health replenish in Metroid Dread.",
            accentColorHex = 0xFF34D399
        ),
        GamingTagPreset(
            id = "animal-crossing-isabelle",
            name = "Isabelle (Summer Outfit)",
            gameSeries = "Animal Crossing",
            amiiboSeries = "Animal Crossing Series",
            amiiboHexId = "0180000000010002",
            description = "Invites Isabelle for a coffee chat at The Roost cafe in Animal Crossing: New Horizons.",
            accentColorHex = 0xFFFBBF24
        )
    )
}
