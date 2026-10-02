package dev.pritam.host.tool.nfc.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.pritam.host.ftp.ui.components.GlassBackButton
import dev.pritam.host.ftp.ui.components.IsometricCard
import dev.pritam.host.ftp.ui.components.IsometricStatTile
import dev.pritam.host.tool.nfc.manager.NfcManager
import dev.pritam.host.tool.nfc.model.GamingPresetCatalog
import dev.pritam.host.tool.nfc.model.GamingTagPreset
import dev.pritam.host.tool.nfc.model.MifareSectorInfo
import dev.pritam.host.tool.nfc.model.NdefParsedRecord
import dev.pritam.host.tool.nfc.model.NfcAppCatalog
import dev.pritam.host.tool.nfc.model.NfcAppFeatureInfo
import dev.pritam.host.tool.nfc.model.NfcMemoryPage
import dev.pritam.host.tool.nfc.model.NfcTagData
import dev.pritam.host.tool.nfc.model.NfcWritePayload
import dev.pritam.host.tool.nfc.model.NfcWriteType
import dev.pritam.host.tool.nfc.ui.components.IsometricNfcAntenna
import dev.pritam.host.ui.theme.Cyan
import dev.pritam.host.ui.theme.Rose
import dev.pritam.host.ui.theme.SpaceBackground
import dev.pritam.host.ui.theme.SurfaceDeep
import dev.pritam.host.ui.theme.SurfaceElevated
import dev.pritam.host.ui.theme.TextPrimary
import dev.pritam.host.ui.theme.TextSecondary
import dev.pritam.host.ui.theme.Violet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NfcViewModel = viewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refreshHardwareStatus(context)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "NFC TOOLS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    GlassBackButton(onClick = onNavigateBack)
                },
                actions = {
                    TextButton(onClick = { viewModel.refreshHardwareStatus(context) }) {
                        Text("Check NFC", color = TextSecondary, fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Isometric 3D NFC Radar Antenna Canvas
            item {
                IsometricNfcAntenna(
                    isScanningOrWriting = true,
                    isWriteMode = uiState.selectedTab == NfcTab.WRITE || uiState.isWaitingForTagToWrite,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 2. Hardware Status & 3x3 Tools Matrix Grid
            item {
                NfcHeaderAnd3x3Grid(
                    hardwareStatus = uiState.hardwareStatus,
                    selectedTab = uiState.selectedTab,
                    onSelectTab = { viewModel.selectTab(it) }
                )
            }

            // 3. Tab Specific Content (9 Core Features)
            when (uiState.selectedTab) {
                NfcTab.READ -> {
                    item {
                        ReadTagSection(
                            tagData = uiState.currentTag,
                            onLoadDemoNtag = { viewModel.loadDemoTag("ntag215") },
                            onLoadDemoMifare = { viewModel.loadDemoTag("mifare") },
                            onClearTag = { viewModel.clearTag() },
                            onCloneToWriter = { tag -> viewModel.cloneTagToWriter(tag) },
                            onEraseTag = { viewModel.armEraseTag() },
                            onLockTag = { viewModel.armLockTag() },
                            onOpenMemoryMap = { viewModel.selectTab(NfcTab.MEMORY_MAP) },
                            onOpenMifareDecoder = { viewModel.selectTab(NfcTab.SECTOR_ANALYZER) }
                        )
                    }
                }
                NfcTab.WRITE -> {
                    item {
                        WriteStudioSection(
                            payload = uiState.writePayload,
                            onUpdatePayload = { viewModel.updateWritePayload(it) },
                            onArmWrite = { viewModel.startWaitingForTagToWrite() },
                            isBatchMode = uiState.isBatchModeActive,
                            batchCount = uiState.batchCount,
                            onToggleBatch = { viewModel.toggleBatchMode(it) },
                            lastWriteResult = uiState.lastWriteResult,
                            isSuccess = uiState.isWriteSuccess
                        )
                    }
                }
                NfcTab.CLONE_ERASE -> {
                    item {
                        CloneEraseSection(
                            tagData = uiState.currentTag,
                            onCloneToWriter = { tag ->
                                viewModel.cloneTagToWriter(tag)
                                viewModel.selectTab(NfcTab.WRITE)
                            },
                            onEraseTag = { viewModel.armEraseTag() },
                            onLockTag = { viewModel.armLockTag() },
                            onOpenWriter = { viewModel.selectTab(NfcTab.WRITE) }
                        )
                    }
                }
                NfcTab.MEMORY_MAP -> {
                    item {
                        SiliconMemoryMapSection(
                            tagData = uiState.currentTag,
                            onLoadSample = { viewModel.loadDemoTag("ntag215") }
                        )
                    }
                }
                NfcTab.SECTOR_ANALYZER -> {
                    item {
                        MifareSectorAnalyzerSection(
                            tagData = uiState.currentTag,
                            onLoadSample = { viewModel.loadDemoTag("mifare") }
                        )
                    }
                }
                NfcTab.CHIP_DB -> {
                    item {
                        ChipDatabaseSection()
                    }
                }
                NfcTab.GAMING_PRESETS -> {
                    item {
                        GamingAmiiboPresetsSection(
                            onSelectAndWrite = { preset -> viewModel.selectGamingPresetAndWrite(preset) }
                        )
                    }
                }
                NfcTab.POPULAR_APPS -> {
                    item {
                        PopularAppsGuideSection()
                    }
                }
                NfcTab.HISTORY -> {
                    item {
                        TagHistorySection(
                            history = uiState.tagHistory,
                            onClearHistory = { viewModel.clearHistory() },
                            onSelectTag = { tag ->
                                viewModel.selectTab(NfcTab.READ)
                            }
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Modal Dialog when waiting for NFC Tag
    if (uiState.isWaitingForTagToWrite) {
        WriteTagWaitingDialog(
            payload = uiState.writePayload,
            isBatchMode = uiState.isBatchModeActive,
            batchCount = uiState.batchCount,
            onCancel = { viewModel.cancelWaitingForTagToWrite() }
        )
    }
}

@Composable
private fun NfcHeaderAnd3x3Grid(
    hardwareStatus: NfcManager.NfcHardwareStatus,
    selectedTab: NfcTab,
    onSelectTab: (NfcTab) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        val statusColor = when (hardwareStatus) {
            NfcManager.NfcHardwareStatus.AVAILABLE_ENABLED -> Cyan
            NfcManager.NfcHardwareStatus.AVAILABLE_DISABLED -> Color(0xFFFBBF24)
            NfcManager.NfcHardwareStatus.NOT_SUPPORTED -> Rose
        }

        val statusText = when (hardwareStatus) {
            NfcManager.NfcHardwareStatus.AVAILABLE_ENABLED -> "NFC READY · ANTENNA ACTIVE"
            NfcManager.NfcHardwareStatus.AVAILABLE_DISABLED -> "NFC DISABLED IN SETTINGS"
            NfcManager.NfcHardwareStatus.NOT_SUPPORTED -> "NFC HARDWARE NOT PRESENT (DEMO MODE AVAILABLE)"
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(8.dp), shape = CircleShape, color = statusColor) {}
                Spacer(Modifier.width(8.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
            Text(
                text = "3×3 MATRIX",
                style = MaterialTheme.typography.labelSmall,
                color = Cyan.copy(alpha = 0.8f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Spacer(Modifier.height(10.dp))

        // 3x3 Grid of 9 Features
        val tabs = NfcTab.entries
        val chunkedTabs = tabs.chunked(3)

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            chunkedTabs.forEach { rowTabs ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowTabs.forEach { tab ->
                        val isSelected = selectedTab == tab
                        val accentColor = when (tab) {
                            NfcTab.READ -> Cyan
                            NfcTab.WRITE -> Rose
                            NfcTab.CLONE_ERASE -> Color(0xFF38BDF8)
                            NfcTab.MEMORY_MAP -> Color(0xFF818CF8)
                            NfcTab.SECTOR_ANALYZER -> Color(0xFFFBBF24)
                            NfcTab.CHIP_DB -> Color(0xFFF472B6)
                            NfcTab.GAMING_PRESETS -> Color(0xFF34D399)
                            NfcTab.POPULAR_APPS -> Violet
                            NfcTab.HISTORY -> TextPrimary
                        }
                        NfcGridCell(
                            tab = tab,
                            isSelected = isSelected,
                            accentColor = accentColor,
                            modifier = Modifier.weight(1f),
                            onClick = { onSelectTab(tab) }
                        )
                    }
                    if (rowTabs.size < 3) {
                        repeat(3 - rowTabs.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NfcGridCell(
    tab: NfcTab,
    isSelected: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        onClick = onClick,
        modifier = modifier.height(68.dp),
        color = if (isSelected) accentColor.copy(alpha = 0.16f) else Color(0x280F172A),
        shape = shape,
        border = BorderStroke(
            1.dp,
            if (isSelected) accentColor else Color(0x33475569)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = tab.icon,
                fontSize = 17.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = tab.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) accentColor else TextPrimary,
                fontSize = 8.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = tab.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) accentColor.copy(alpha = 0.85f) else TextSecondary.copy(alpha = 0.7f),
                fontSize = 7.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CloneEraseSection(
    tagData: NfcTagData?,
    onCloneToWriter: (NfcTagData) -> Unit,
    onEraseTag: () -> Unit,
    onLockTag: () -> Unit,
    onOpenWriter: () -> Unit,
) {
    IsometricCard(glowColor = Color(0xFF38BDF8)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TAG CLONE & WIPE STUDIO",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                    Text(
                        text = "Contactless Tag Cloning, Memory Formatting & Write-Protection",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }

            // Scanned Tag Info for Cloning
            if (tagData != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0x2038BDF8),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "READY TO CLONE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8)
                            )
                            Text(
                                text = "UID: ${tagData.uidHex}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "${tagData.tagStandard} · ${tagData.technologiesString}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary
                        )
                        if (tagData.records.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Payload: ${tagData.records.first().summary}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { onCloneToWriter(tagData) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("⚡ Copy Payload to Write Studio", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceDeep,
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("💡", fontSize = 20.sp)
                        Column {
                            Text(
                                text = "No Tag Scanned Yet",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Scan any physical tag in 'Scan & Info' tab first to clone its contents here.",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // Factory Wipe / Erase Action Card
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SurfaceDeep,
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🧹", fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Factory Erase / Format NDEF",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Wipes all existing NDEF records and resets chip to empty formatted container.",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                    Button(
                        onClick = onEraseTag,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Arm Factory Wipe / Erase", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Permanent Lock Action Card
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SurfaceDeep,
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔒", fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Permanent Write Protection (Lock)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF59E0B)
                            )
                            Text(
                                text = "WARNING: Blows dynamic lock bytes (OTP). Tag becomes permanent READ-ONLY forever.",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                    Button(
                        onClick = onLockTag,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Arm Permanent Chip Lock (Read Only)", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChipDatabaseSection() {
    IsometricCard(glowColor = Color(0xFFF472B6)) {
        val chipSpecs = listOf(
            Triple("NXP NTAG213 / 215 / 216", "ISO/IEC 14443-A · 13.56 MHz · NFC Forum Type 2", listOf("User Memory: 144 B (213) / 504 B (215) / 888 B (216)", "Amiibo Gaming Standard: NTAG215 (540 B Total)", "Password & Lock: 32-bit password & dynamic lock bytes", "Endurance: 100,000 write cycles · 10-year retention")),
            Triple("NXP MIFARE Classic 1K / 4K", "ISO/IEC 14443-A · 13.56 MHz · Crypto-1", listOf("Memory: 1024 B (1K - 16 sectors) / 4096 B (4K - 40 sectors)", "Security: Proprietary Crypto-1 cipher with Key A / Key B", "Blocks: 4 blocks per sector (16 bytes per block)", "Access Condition: Configurable 3-bit access control")),
            Triple("NXP MIFARE Ultralight C", "ISO/IEC 14443-A · 13.56 MHz · Type 2", listOf("User Memory: 144 Bytes (36 pages × 4 bytes)", "Security: 3DES mutual authentication with 112-bit key", "Use Cases: Event ticketing, public transit single-journey")),
            Triple("NXP MIFARE DESFire EV3", "ISO/IEC 14443-4 · 13.56 MHz · Type 4", listOf("Memory Options: 2 KB / 4 KB / 8 KB multi-application", "Hardware Crypto: AES-128, 3DES, DES hardware engine", "Certifications: Common Criteria EAL5+ security rating")),
            Triple("Sony FeliCa Lite-S (RC-S966)", "JIS X 6319-4 / ISO 18092 · 13.56 MHz · Type 3", listOf("User Memory: 224 Bytes (14 blocks × 16 bytes)", "Speed: 212 kbps / 424 kbps high-speed data transmission", "Use Cases: Suica / Pasmo transit, Japanese payment ecosystem")),
            Triple("NXP ICODE SLIX / SLIX2", "ISO/IEC 15693 Vicinity · 13.56 MHz · Type 5", listOf("Operating Range: Vicinity RFID up to 1.5 meters range", "Memory: 112 Bytes (SLIX) / 316 Bytes (SLIX2)", "Features: EAS protection, Privacy mode, 50-year retention"))
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column {
                Text(
                    text = "SILICON CHIP DATABASE",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF472B6)
                )
                Text(
                    text = "Standard RFID / NFC Integrated Circuit Technical Datasheets",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }

            chipSpecs.forEach { (chipName, standard, details) ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SurfaceDeep,
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = chipName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF472B6)
                            )
                            Text(
                                text = "13.56 MHz",
                                style = MaterialTheme.typography.labelSmall,
                                color = Cyan,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = standard,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        details.forEach { detail ->
                            Row(modifier = Modifier.padding(vertical = 1.dp)) {
                                Text("• ", color = Color(0xFFF472B6), fontSize = 11.sp)
                                Text(
                                    text = detail,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextPrimary.copy(alpha = 0.9f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadTagSection(
    tagData: NfcTagData?,
    onLoadDemoNtag: () -> Unit,
    onLoadDemoMifare: () -> Unit,
    onClearTag: () -> Unit,
    onCloneToWriter: (NfcTagData) -> Unit,
    onEraseTag: () -> Unit,
    onLockTag: () -> Unit,
    onOpenMemoryMap: () -> Unit,
    onOpenMifareDecoder: () -> Unit,
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (tagData == null) {
            IsometricCard(glowColor = Cyan) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "HOLD TAG NEAR PHONE NFC ANTENNA",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Cyan,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Instantly reads NTAG213/215/216, Mifare Classic 1K/4K, DESFire, Ultralight, and ISO 14443 tags",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onLoadDemoNtag,
                            border = BorderStroke(1.dp, Cyan.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Cyan)
                        ) {
                            Text("Sample NTAG215", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = onLoadDemoMifare,
                            border = BorderStroke(1.dp, Violet.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Violet)
                        ) {
                            Text("Sample Mifare", fontSize = 11.sp)
                        }
                    }
                }
            }
        } else {
            // Hardware Specs & Actions
            IsometricCard(glowColor = Cyan) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("TAG SILICON & HARDWARE (UID)", style = MaterialTheme.typography.labelSmall, color = Cyan, fontSize = 11.sp, letterSpacing = 1.sp)
                        TextButton(onClick = onClearTag) {
                            Text("Clear", color = Rose, fontSize = 11.sp)
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { copyToClipboard(context, "Tag UID", tagData.uidHex) },
                        color = SurfaceDeep
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("SERIAL NUMBER (HEX UID)", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                                Text(tagData.uidHex, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Cyan, fontFamily = FontFamily.Monospace)
                            }
                            Text("COPY", style = MaterialTheme.typography.labelSmall, color = Cyan, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        IsometricStatTile(label = "IC Model", value = tagData.tagStandard, accentColor = Violet, modifier = Modifier.weight(1.3f))
                        IsometricStatTile(label = "Manufacturer", value = tagData.icManufacturer, accentColor = Cyan, modifier = Modifier.weight(1f))
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        IsometricStatTile(label = "Memory Size", value = "${tagData.memorySizeBytes} B", accentColor = Cyan, modifier = Modifier.weight(1f))
                        IsometricStatTile(label = "Lock Status", value = if (tagData.isWritable) "WRITABLE" else "READ-ONLY", accentColor = if (tagData.isWritable) Color(0xFF34D399) else Rose, modifier = Modifier.weight(1f))
                    }

                    Spacer(Modifier.height(14.dp))

                    // Action buttons
                    Text("TAG ACTIONS & TOOLBOX", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                    Spacer(Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { onCloneToWriter(tagData) },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, Rose),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Rose),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Text("📋 Clone Tag", fontSize = 10.sp)
                        }

                        OutlinedButton(
                            onClick = onEraseTag,
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, Color(0xFFFBBF24)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFBBF24)),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Text("🗑️ Erase Tag", fontSize = 10.sp)
                        }

                        OutlinedButton(
                            onClick = onLockTag,
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, Color(0xFFEF4444)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Text("🔒 Lock Tag", fontSize = 10.sp)
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = onOpenMemoryMap,
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, Cyan),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Cyan),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Text("🧠 Page Map", fontSize = 10.sp)
                        }

                        OutlinedButton(
                            onClick = onOpenMifareDecoder,
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, Violet),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Violet),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                        ) {
                            Text("🔐 Mifare Decoder", fontSize = 10.sp)
                        }
                    }
                }
            }

            // NDEF Records
            IsometricCard(glowColor = Violet) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("DECODED NDEF RECORDS (${tagData.records.size})", style = MaterialTheme.typography.labelSmall, color = Violet, fontSize = 11.sp, letterSpacing = 1.sp)
                    Spacer(Modifier.height(10.dp))

                    if (tagData.records.isEmpty()) {
                        Text("No NDEF payload formatted on this tag (blank or proprietary sector data)", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 11.sp)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            tagData.records.forEachIndexed { idx, record ->
                                val isUnknown = record is dev.pritam.host.tool.nfc.model.NdefParsedRecord.Unknown
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            BorderStroke(
                                                1.dp,
                                                if (isUnknown) Color(0xFFF59E0B).copy(alpha = 0.5f) else Color(0xFF1E293B)
                                            ),
                                            RoundedCornerShape(8.dp)
                                        ),
                                    color = if (isUnknown) Color(0xFF451A03).copy(alpha = 0.3f) else SurfaceDeep
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Record #${idx + 1} · ${record.recordType}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isUnknown) Color(0xFFF59E0B) else Cyan,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            TextButton(
                                                onClick = { copyToClipboard(context, "Record #${idx + 1} (${record.recordType})", record.summary) },
                                                contentPadding = PaddingValues(0.dp),
                                                modifier = Modifier.height(20.dp)
                                            ) {
                                                Text("Copy", color = if (isUnknown) Color(0xFFF59E0B) else Cyan, fontSize = 10.sp)
                                            }
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(record.summary, style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WriteStudioSection(
    payload: NfcWritePayload,
    onUpdatePayload: (NfcWritePayload) -> Unit,
    onArmWrite: () -> Unit,
    isBatchMode: Boolean,
    batchCount: Int,
    onToggleBatch: (Boolean) -> Unit,
    lastWriteResult: String?,
    isSuccess: Boolean,
) {
    IsometricCard(glowColor = Rose) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("NFC WRITE STUDIO", style = MaterialTheme.typography.labelSmall, color = Rose, fontSize = 11.sp, letterSpacing = 1.sp)
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Rose.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = payload.writeType.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Rose,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Write Type Selector Chips
            val scrollState = rememberScrollState()
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                NfcWriteType.entries.forEach { type ->
                    val isSelected = payload.writeType == type
                    FilterBadge(type.displayName, isSelected = isSelected, color = Rose) {
                        onUpdatePayload(payload.copy(writeType = type))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Form inputs based on type
            when (payload.writeType) {
                NfcWriteType.TEXT -> {
                    OutlinedTextField(
                        value = payload.textContent,
                        onValueChange = { onUpdatePayload(payload.copy(textContent = it)) },
                        label = { Text("Text Message") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                NfcWriteType.URI -> {
                    OutlinedTextField(
                        value = payload.uriString,
                        onValueChange = { onUpdatePayload(payload.copy(uriString = it)) },
                        label = { Text("Web URL / Link") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                NfcWriteType.WIFI -> {
                    OutlinedTextField(
                        value = payload.wifiSsid,
                        onValueChange = { onUpdatePayload(payload.copy(wifiSsid = it)) },
                        label = { Text("Wi-Fi SSID (Network Name)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = payload.wifiPassword,
                        onValueChange = { onUpdatePayload(payload.copy(wifiPassword = it)) },
                        label = { Text("Wi-Fi Password") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                NfcWriteType.CONTACT_VCARD -> {
                    OutlinedTextField(
                        value = payload.contactName,
                        onValueChange = { onUpdatePayload(payload.copy(contactName = it)) },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = payload.contactOrganization,
                        onValueChange = { onUpdatePayload(payload.copy(contactOrganization = it)) },
                        label = { Text("Company / Organization") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = payload.contactPhone,
                        onValueChange = { onUpdatePayload(payload.copy(contactPhone = it)) },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = payload.contactEmail,
                        onValueChange = { onUpdatePayload(payload.copy(contactEmail = it)) },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                NfcWriteType.SOCIAL_PROFILE -> {
                    val platforms = listOf("INSTAGRAM", "LINKEDIN", "X_TWITTER", "YOUTUBE", "TIKTOK", "WHATSAPP", "GITHUB")
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        platforms.forEach { p ->
                            FilterBadge(p, isSelected = payload.socialPlatform == p, color = Violet) {
                                onUpdatePayload(payload.copy(socialPlatform = p))
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = payload.socialHandleOrUrl,
                        onValueChange = { onUpdatePayload(payload.copy(socialHandleOrUrl = it)) },
                        label = { Text("Username / Handle / Phone (e.g. alex_vance)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                NfcWriteType.ONE_TAP_REVIEW -> {
                    OutlinedTextField(
                        value = payload.reviewPlaceOrLink,
                        onValueChange = { onUpdatePayload(payload.copy(reviewPlaceOrLink = it)) },
                        label = { Text("Google Review URL or Trustpilot link") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                NfcWriteType.APP_LAUNCHER -> {
                    OutlinedTextField(
                        value = payload.appPackageName,
                        onValueChange = { onUpdatePayload(payload.copy(appPackageName = it)) },
                        label = { Text("Android Package Name (e.g. com.google.android.youtube)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                NfcWriteType.DEVICE_AUTOMATION -> {
                    val actions = listOf("TOGGLE_WIFI", "TOGGLE_BT", "TOGGLE_FLASHLIGHT", "SILENT_MODE", "HOTSPOT_ON")
                    Text("NFC Tasks Automation Action:", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 10.sp)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        actions.forEach { act ->
                            FilterBadge(act, isSelected = payload.automationTaskAction == act, color = Cyan) {
                                onUpdatePayload(payload.copy(automationTaskAction = act))
                            }
                        }
                    }
                }
                NfcWriteType.NTAG_MIRRORING -> {
                    Text("NXP Dynamic Mirroring Template (Replaces {UID} and {CNT} on tap):", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 10.sp)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = payload.mirroringBaseUrl,
                        onValueChange = { onUpdatePayload(payload.copy(mirroringBaseUrl = it)) },
                        label = { Text("Mirror URL Template") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                NfcWriteType.GAMING_NTAG215 -> {
                    Text("Select Character Preset to write to NTAG215:", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 10.sp)
                    Spacer(Modifier.height(6.dp))
                    GamingPresetCatalog.PRESETS.forEach { preset ->
                        val isSelected = payload.selectedGamingPresetId == preset.id
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onUpdatePayload(payload.copy(selectedGamingPresetId = preset.id)) }
                                .border(BorderStroke(1.dp, if (isSelected) Color(preset.accentColorHex) else Color(0xFF334155)), RoundedCornerShape(8.dp)),
                            color = if (isSelected) Color(preset.accentColorHex).copy(alpha = 0.15f) else SurfaceDeep
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(preset.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("${preset.gameSeries} · ${preset.description}", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 9.sp)
                                }
                                if (isSelected) {
                                    Text("✓", color = Color(preset.accentColorHex), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
                NfcWriteType.RAW_HEX -> {
                    OutlinedTextField(
                        value = payload.rawHexPayload,
                        onValueChange = { onUpdatePayload(payload.copy(rawHexPayload = it)) },
                        label = { Text("Raw Hex Byte Stream (e.g. 00 01 02 A0 B1)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                NfcWriteType.CUSTOM_MIME -> {
                    OutlinedTextField(
                        value = payload.customMimeType,
                        onValueChange = { onUpdatePayload(payload.copy(customMimeType = it)) },
                        label = { Text("MIME Type") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = payload.customMimePayload,
                        onValueChange = { onUpdatePayload(payload.copy(customMimePayload = it)) },
                        label = { Text("Payload Content") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                NfcWriteType.ERASE_FORMAT -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
                        color = Color(0xFFFBBF24).copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "⚠️ Format Mode: Touching a tag will overwrite its contents with an empty NDEF block, returning it to factory blank state.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFBBF24),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Batch write & Read-only toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Batch Writing Mode (NXP TagWriter)", style = MaterialTheme.typography.labelSmall, color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("Keep antenna armed to write consecutive tags", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 9.sp)
                }
                Switch(
                    checked = isBatchMode,
                    onCheckedChange = onToggleBatch,
                    colors = SwitchDefaults.colors(checkedThumbColor = Cyan, checkedTrackColor = Cyan.copy(alpha = 0.3f))
                )
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onArmWrite,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Rose, contentColor = Color(0xFF0F172A))
            ) {
                Text(if (isBatchMode) "ARM BATCH WRITER ($batchCount WRITTEN)" else "ARM ANTENNA & WRITE TO TAG", fontWeight = FontWeight.Bold)
            }

            if (lastWriteResult != null) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSuccess) Color(0xFF34D399).copy(alpha = 0.15f) else Rose.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = lastWriteResult,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSuccess) Color(0xFF34D399) else Rose,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SiliconMemoryMapSection(
    tagData: NfcTagData?,
    onLoadSample: () -> Unit,
) {
    IsometricCard(glowColor = Cyan) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("NTAG MEMORY PAGES (NXP ARCHITECTURE)", style = MaterialTheme.typography.labelSmall, color = Cyan, fontSize = 11.sp, letterSpacing = 1.sp)
                if (tagData == null || tagData.memoryPages.isEmpty()) {
                    TextButton(onClick = onLoadSample) {
                        Text("Load Sample", color = Cyan, fontSize = 10.sp)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "Visualization of 4-byte pages: Header, Capability Container (CC), User Data, and Lock/Config Bytes.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 11.sp
            )

            Spacer(Modifier.height(12.dp))

            val pages = tagData?.memoryPages ?: emptyList()
            if (pages.isEmpty()) {
                Text("Scan an NTAG tag or tap 'Load Sample' to view page memory map", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 11.sp)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    pages.forEach { page ->
                        val badgeColor = when (page.pageType) {
                            NfcMemoryPage.MemoryPageType.HEADER_UID -> Cyan
                            NfcMemoryPage.MemoryPageType.CAPABILITY_CONTAINER -> Violet
                            NfcMemoryPage.MemoryPageType.USER_DATA -> Color(0xFF34D399)
                            NfcMemoryPage.MemoryPageType.DYNAMIC_LOCK -> Color(0xFFFBBF24)
                            NfcMemoryPage.MemoryPageType.CONFIG_AND_PWD -> Rose
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)),
                            color = SurfaceDeep
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1.4f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(3.dp),
                                            color = badgeColor.copy(alpha = 0.2f)
                                        ) {
                                            Text("P${page.pageNumber}", color = badgeColor, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                        }
                                        Spacer(Modifier.width(6.dp))
                                        Text(page.description, style = MaterialTheme.typography.labelSmall, color = TextPrimary, fontSize = 10.sp)
                                    }
                                }

                                Text(
                                    text = page.hexContent,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = badgeColor,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MifareSectorAnalyzerSection(
    tagData: NfcTagData?,
    onLoadSample: () -> Unit,
) {
    IsometricCard(glowColor = Color(0xFFFBBF24)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("MIFARE CLASSIC SECTORS & ACCESS BITS (MCT)", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFBBF24), fontSize = 11.sp, letterSpacing = 1.sp)
                if (tagData == null || tagData.mifareSectors.isEmpty()) {
                    TextButton(onClick = onLoadSample) {
                        Text("Load Sample", color = Color(0xFFFBBF24), fontSize = 10.sp)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "Sector access matrix, trailer blocks (Key A / Key B), and access condition bits.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 11.sp
            )

            Spacer(Modifier.height(12.dp))

            val sectors = tagData?.mifareSectors ?: emptyList()
            if (sectors.isEmpty()) {
                Text("Scan a Mifare Classic tag or tap 'Load Sample' to view sector permissions", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 11.sp)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sectors.forEach { sec ->
                        var isExpanded by remember { mutableStateOf(false) }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isExpanded = !isExpanded }
                                .border(BorderStroke(1.dp, Color(0xFF334155)), RoundedCornerShape(8.dp)),
                            color = SurfaceDeep
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("SECTOR ${sec.sectorIndex} (Blocks ${sec.firstBlock}..${sec.lastBlock})", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    Text(if (isExpanded) "▲" else "▼", color = TextSecondary, fontSize = 10.sp)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text("Access Bits: ${sec.accessBitsHex} · ${sec.accessConditionSummary}", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 9.sp)

                                AnimatedVisibility(visible = isExpanded) {
                                    Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        sec.dataBlocksPreview.forEach { blk ->
                                            Text(blk, style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GamingAmiiboPresetsSection(
    onSelectAndWrite: (GamingTagPreset) -> Unit,
) {
    IsometricCard(glowColor = Color(0xFF34D399)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("GAMING & AMIIBO PRESET LAB (TAGMO)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF34D399), fontSize = 11.sp, letterSpacing = 1.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Preset generator for NTAG215 gaming tags. 1-click write character dumps for Zelda, Mario, Smash Bros, and Metroid.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 11.sp
            )

            Spacer(Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GamingPresetCatalog.PRESETS.forEach { preset ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp)),
                        color = SurfaceDeep
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(preset.name, style = MaterialTheme.typography.titleSmall, color = Color(preset.accentColorHex), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("${preset.gameSeries} · ${preset.amiiboSeries}", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 10.sp)
                                Spacer(Modifier.height(2.dp))
                                Text(preset.description, style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontSize = 10.sp)
                            }

                            Spacer(Modifier.width(8.dp))

                            OutlinedButton(
                                onClick = { onSelectAndWrite(preset) },
                                border = BorderStroke(1.dp, Color(preset.accentColorHex)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(preset.accentColorHex)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Write Tag", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PopularAppsGuideSection() {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("POPULAR NFC APPS REFERENCE & COMPARISON", style = MaterialTheme.typography.labelSmall, color = Violet, fontSize = 11.sp, letterSpacing = 1.sp)

        NfcAppCatalog.popularApps.forEach { appInfo ->
            PopularAppCard(appInfo)
        }
    }
}

@Composable
private fun PopularAppCard(app: NfcAppFeatureInfo) {
    var isExpanded by remember { mutableStateOf(false) }

    IsometricCard(glowColor = Violet) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(app.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("${app.developer} · ${app.category}", style = MaterialTheme.typography.bodySmall, color = Cyan, fontSize = 11.sp)
                }
                Text(if (isExpanded) "Collapse ▲" else "Details ▼", style = MaterialTheme.typography.labelSmall, color = Violet, fontSize = 10.sp)
            }

            Spacer(Modifier.height(6.dp))
            Text(app.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 11.sp)

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    Text("Core Functionalities (Built into MotherOfAllApps):", style = MaterialTheme.typography.labelSmall, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    app.coreFunctionalities.forEach { f ->
                        Text("• $f", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 11.sp)
                    }

                    Spacer(Modifier.height(8.dp))
                    Text("Advanced Hardware Features:", style = MaterialTheme.typography.labelSmall, color = Cyan, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    app.advancedFeatures.forEach { f ->
                        Text("• $f", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 11.sp)
                    }

                    Spacer(Modifier.height(8.dp))
                    Text("Supported Tags: ${app.supportedTags.joinToString(", ")}", style = MaterialTheme.typography.bodySmall, color = Violet, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun TagHistorySection(
    history: List<NfcTagData>,
    onClearHistory: () -> Unit,
    onSelectTag: (NfcTagData) -> Unit,
) {
    IsometricCard(glowColor = TextPrimary) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SCANNED TAG HISTORY (${history.size})", style = MaterialTheme.typography.labelSmall, color = TextPrimary, fontSize = 11.sp, letterSpacing = 1.sp)
                if (history.isNotEmpty()) {
                    TextButton(onClick = onClearHistory) {
                        Text("Clear", color = Rose, fontSize = 11.sp)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (history.isEmpty()) {
                Text("No scanned tags in history yet", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 11.sp)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    history.forEach { item ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelectTag(item) },
                            color = SurfaceDeep
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(item.uidHex, style = MaterialTheme.typography.titleSmall, color = Cyan, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                    Text(item.formattedScannedTime, style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 10.sp)
                                }
                                Text("${item.tagStandard} · ${item.memorySizeBytes} bytes (${item.records.size} NDEF records)", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WriteTagWaitingDialog(
    payload: NfcWritePayload,
    isBatchMode: Boolean,
    batchCount: Int,
    onCancel: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)

    Dialog(onDismissRequest = onCancel) {
        Surface(
            modifier = Modifier.fillMaxWidth().clip(shape).border(BorderStroke(1.dp, Rose), shape),
            color = SurfaceElevated,
            shape = shape
        ) {
            Column(modifier = Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isBatchMode) "BATCH WRITER ACTIVE ($batchCount WRITTEN)" else "READY TO WRITE NFC TAG",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Rose,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Payload: ${payload.writeType.displayName}\nHold NFC tag against the back of your phone to execute",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(18.dp))
                OutlinedButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Text("Done / Cancel")
                }
            }
        }
    }
}

@Composable
private fun FilterBadge(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Surface(
        modifier = Modifier.clip(shape).clickable { onClick() }.border(BorderStroke(1.dp, if (isSelected) color else Color(0xFF334155)), shape),
        color = if (isSelected) color.copy(alpha = 0.2f) else SurfaceDeep,
        shape = shape
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) color else TextSecondary, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    dev.pritam.host.logging.AppLogHub.logClipboardOperation(
        toolId = "nfc-tool",
        toolName = "NFC Tools",
        operationType = "COPY",
        label = label,
        content = text
    )
    Toast.makeText(context, "Copied $label to clipboard", Toast.LENGTH_SHORT).show()
}
