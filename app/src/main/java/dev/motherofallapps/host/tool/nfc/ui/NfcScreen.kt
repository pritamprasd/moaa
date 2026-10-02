package dev.motherofallapps.host.tool.nfc.ui

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.motherofallapps.host.ftp.storage.StorageUtils
import dev.motherofallapps.host.ftp.ui.components.IsometricCard
import dev.motherofallapps.host.ftp.ui.components.IsometricStatTile
import dev.motherofallapps.host.tool.nfc.manager.NfcManager
import dev.motherofallapps.host.tool.nfc.model.NdefParsedRecord
import dev.motherofallapps.host.tool.nfc.model.NfcAppCatalog
import dev.motherofallapps.host.tool.nfc.model.NfcAppFeatureInfo
import dev.motherofallapps.host.tool.nfc.model.NfcTagData
import dev.motherofallapps.host.tool.nfc.model.NfcWritePayload
import dev.motherofallapps.host.tool.nfc.model.NfcWriteType
import dev.motherofallapps.host.tool.nfc.ui.components.IsometricNfcAntenna
import dev.motherofallapps.host.ui.theme.Cyan
import dev.motherofallapps.host.ui.theme.Rose
import dev.motherofallapps.host.ui.theme.SpaceBackground
import dev.motherofallapps.host.ui.theme.SurfaceDeep
import dev.motherofallapps.host.ui.theme.SurfaceElevated
import dev.motherofallapps.host.ui.theme.TextPrimary
import dev.motherofallapps.host.ui.theme.TextSecondary
import dev.motherofallapps.host.ui.theme.Violet

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
        containerColor = SpaceBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "NFC READ & WRITE TOOL",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("← Back", color = Cyan)
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.refreshHardwareStatus(context) }) {
                        Text("Check NFC", color = TextSecondary, fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SpaceBackground)
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
                    isWriteMode = uiState.selectedTab == NfcTab.WRITE,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 2. Hardware Status & Tab Selector
            item {
                NfcHeaderAndTabs(
                    hardwareStatus = uiState.hardwareStatus,
                    selectedTab = uiState.selectedTab,
                    onSelectTab = { viewModel.selectTab(it) }
                )
            }

            // 3. Tab Content
            when (uiState.selectedTab) {
                NfcTab.READ -> {
                    item {
                        ReadTagSection(
                            tagData = uiState.currentTag,
                            onLoadDemoNtag = { viewModel.loadDemoTag("ntag215") },
                            onLoadDemoMifare = { viewModel.loadDemoTag("mifare") },
                            onClearTag = { viewModel.clearTag() }
                        )
                    }
                }
                NfcTab.WRITE -> {
                    item {
                        WriteTagSection(
                            payload = uiState.writePayload,
                            onUpdatePayload = { viewModel.updateWritePayload(it) },
                            onArmWrite = { viewModel.startWaitingForTagToWrite() },
                            lastWriteResult = uiState.lastWriteResult,
                            isSuccess = uiState.isWriteSuccess
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
                            onClearHistory = { viewModel.clearHistory() }
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Modal when waiting for NFC Tag to write
    if (uiState.isWaitingForTagToWrite) {
        WriteTagWaitingDialog(
            payload = uiState.writePayload,
            onCancel = { viewModel.cancelWaitingForTagToWrite() }
        )
    }
}

@Composable
private fun NfcHeaderAndTabs(
    hardwareStatus: NfcManager.NfcHardwareStatus,
    selectedTab: NfcTab,
    onSelectTab: (NfcTab) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Status indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val statusColor = when (hardwareStatus) {
                NfcManager.NfcHardwareStatus.AVAILABLE_ENABLED -> Cyan
                NfcManager.NfcHardwareStatus.AVAILABLE_DISABLED -> Color(0xFFFBBF24)
                NfcManager.NfcHardwareStatus.NOT_SUPPORTED -> Rose
            }

            val statusText = when (hardwareStatus) {
                NfcManager.NfcHardwareStatus.AVAILABLE_ENABLED -> "NFC READY · TAP TAG TO SCAN"
                NfcManager.NfcHardwareStatus.AVAILABLE_DISABLED -> "NFC DISABLED IN SYSTEM SETTINGS"
                NfcManager.NfcHardwareStatus.NOT_SUPPORTED -> "NFC HARDWARE NOT DETECTED (DEMO MODE AVAILABLE)"
            }

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
        }

        Spacer(Modifier.height(12.dp))

        // Tab Selector Row
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TabBadgeButton("📡 READ / SCAN", isSelected = selectedTab == NfcTab.READ, color = Cyan) {
                onSelectTab(NfcTab.READ)
            }
            TabBadgeButton("✍️ WRITE TAG", isSelected = selectedTab == NfcTab.WRITE, color = Rose) {
                onSelectTab(NfcTab.WRITE)
            }
            TabBadgeButton("🌟 POPULAR APPS GUIDE", isSelected = selectedTab == NfcTab.POPULAR_APPS, color = Violet) {
                onSelectTab(NfcTab.POPULAR_APPS)
            }
            TabBadgeButton("📜 TAG HISTORY", isSelected = selectedTab == NfcTab.HISTORY, color = TextPrimary) {
                onSelectTab(NfcTab.HISTORY)
            }
        }
    }
}

@Composable
private fun TabBadgeButton(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Surface(
        modifier = Modifier
            .clip(shape)
            .clickable { onClick() }
            .border(BorderStroke(1.dp, if (isSelected) color else Color(0xFF334155)), shape),
        color = if (isSelected) color.copy(alpha = 0.2f) else SurfaceDeep,
        shape = shape
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) color else TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun ReadTagSection(
    tagData: NfcTagData?,
    onLoadDemoNtag: () -> Unit,
    onLoadDemoMifare: () -> Unit,
    onClearTag: () -> Unit,
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
                        text = "Supports NTAG213/215/216, Mifare Classic, DESFire, Ultralight, and ISO 14443 tags",
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
                            Text("Load Sample NTAG215", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = onLoadDemoMifare,
                            border = BorderStroke(1.dp, Violet.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Violet)
                        ) {
                            Text("Load Sample Mifare", fontSize = 11.sp)
                        }
                    }
                }
            }
        } else {
            // UID and Chip Specs Card
            IsometricCard(glowColor = Cyan) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("TAG HARDWARE & SERIAL (UID)", style = MaterialTheme.typography.labelSmall, color = Cyan, fontSize = 11.sp, letterSpacing = 1.sp)
                        TextButton(onClick = onClearTag) {
                            Text("Clear", color = Rose, fontSize = 11.sp)
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // UID Box
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
                                Text("HEX UID", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
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
                        IsometricStatTile(label = "User Memory", value = "${tagData.memorySizeBytes} B", accentColor = Cyan, modifier = Modifier.weight(1f))
                        IsometricStatTile(label = "Lock Status", value = if (tagData.isWritable) "WRITABLE" else "READ-ONLY", accentColor = if (tagData.isWritable) Color(0xFF34D399) else Rose, modifier = Modifier.weight(1f))
                    }

                    Spacer(Modifier.height(10.dp))

                    Text("Supported Technologies: ${tagData.technologiesString}", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 11.sp)
                }
            }

            // NDEF Records Card
            IsometricCard(glowColor = Violet) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("DECODED NDEF RECORDS (${tagData.records.size})", style = MaterialTheme.typography.labelSmall, color = Violet, fontSize = 11.sp, letterSpacing = 1.sp)
                    Spacer(Modifier.height(10.dp))

                    if (tagData.records.isEmpty()) {
                        Text("No NDEF payload formatted on this tag (blank or proprietary sector data)", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 11.sp)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            tagData.records.forEachIndexed { idx, record ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp)),
                                    color = SurfaceDeep
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Record #${idx + 1} · ${record.recordType}", style = MaterialTheme.typography.labelSmall, color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
private fun WriteTagSection(
    payload: NfcWritePayload,
    onUpdatePayload: (NfcWritePayload) -> Unit,
    onArmWrite: () -> Unit,
    lastWriteResult: String?,
    isSuccess: Boolean,
) {
    IsometricCard(glowColor = Rose) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("CREATE NDEF PAYLOAD TO WRITE", style = MaterialTheme.typography.labelSmall, color = Rose, fontSize = 11.sp, letterSpacing = 1.sp)
            Spacer(Modifier.height(12.dp))

            // Write Type Selector Chips
            val scrollState = rememberScrollState()
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                NfcWriteType.entries.forEach { type ->
                    val isSelected = payload.writeType == type
                    FilterBadge(type.name.replace("_", " "), isSelected = isSelected, color = Rose) {
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
                NfcWriteType.APP_LAUNCHER -> {
                    OutlinedTextField(
                        value = payload.appPackageName,
                        onValueChange = { onUpdatePayload(payload.copy(appPackageName = it)) },
                        label = { Text("Android Package Name (AAR)") },
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
                        label = { Text("Payload String / JSON") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Button(
                onClick = onArmWrite,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Rose, contentColor = Color(0xFF0F172A))
            ) {
                Text("ARM ANTENNA & WRITE TO TAG", fontWeight = FontWeight.Bold)
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
private fun PopularAppsGuideSection() {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("POPULAR NFC APPS & FUNCTIONALITY GUIDE", style = MaterialTheme.typography.labelSmall, color = Violet, fontSize = 11.sp, letterSpacing = 1.sp)

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
                    Text("Core Functionalities:", style = MaterialTheme.typography.labelSmall, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
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
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
                            color = SurfaceDeep
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(item.uidHex, style = MaterialTheme.typography.titleSmall, color = Cyan, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                    Text(item.formattedScannedTime, style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 10.sp)
                                }
                                Text("${item.tagStandard} · ${item.memorySizeBytes} bytes", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 11.sp)
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
                Text("READY TO WRITE NFC TAG", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Rose, letterSpacing = 1.sp)
                Spacer(Modifier.height(10.dp))
                Text("Hold an NFC tag against the back of your phone to write payload", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 11.sp)
                Spacer(Modifier.height(18.dp))
                OutlinedButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Text("Cancel")
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
    Toast.makeText(context, "Copied $label to clipboard", Toast.LENGTH_SHORT).show()
}
