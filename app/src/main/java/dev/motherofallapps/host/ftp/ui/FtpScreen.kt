package dev.motherofallapps.host.ftp.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.motherofallapps.host.ftp.model.FtpServerState
import dev.motherofallapps.host.ftp.storage.StorageUtils
import dev.motherofallapps.host.ftp.ui.components.ConnectionDetailsCard
import dev.motherofallapps.host.ftp.ui.components.FolderPickerDialog
import dev.motherofallapps.host.ftp.ui.components.FtpConfigDialog
import dev.motherofallapps.host.ftp.ui.components.IsometricCard
import dev.motherofallapps.host.ftp.ui.components.IsometricControlHeader
import dev.motherofallapps.host.ftp.ui.components.IsometricServerNode
import dev.motherofallapps.host.ftp.ui.components.IsometricTelemetryDials
import dev.motherofallapps.host.ftp.ui.components.LiveActivityConsole
import dev.motherofallapps.host.ftp.ui.components.StorageInfoCard
import dev.motherofallapps.host.ui.theme.Cyan
import dev.motherofallapps.host.ui.theme.SpaceBackground
import dev.motherofallapps.host.ui.theme.SurfaceElevated
import dev.motherofallapps.host.ui.theme.TextPrimary
import dev.motherofallapps.host.ui.theme.TextSecondary
import dev.motherofallapps.host.ui.theme.Violet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FtpScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FtpViewModel = viewModel(),
) {
    val context = LocalContext.current

    val serverState by viewModel.serverState.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()
    val networkStatus by viewModel.networkStatus.collectAsStateWithLifecycle()
    val storageInfo by viewModel.storageInfo.collectAsStateWithLifecycle()
    val hasStoragePermission by viewModel.hasStoragePermission.collectAsStateWithLifecycle()

    var showConfigDialog by remember { mutableStateOf(false) }
    var showFolderDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshState(context)
    }

    val isRunning = serverState is FtpServerState.Running

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = SpaceBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "LAN FTP SERVER",
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
                    TextButton(onClick = { viewModel.refreshState(context) }) {
                        Text("Refresh", color = TextSecondary, fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SpaceBackground
                )
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
            // 1. Isometric 3D Server Node Graphic
            item {
                IsometricServerNode(
                    isRunning = isRunning,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 2. Isometric Control Header & Power Switch
            item {
                IsometricControlHeader(
                    serverState = serverState,
                    networkStatus = networkStatus,
                    onToggleServer = {
                        if (!hasStoragePermission) {
                            try {
                                context.startActivity(StorageUtils.createManageStorageIntent(context))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Please grant storage permission in settings", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            viewModel.toggleServer(context)
                        }
                    }
                )
            }

            // 3. Live Telemetry Dials
            item {
                IsometricTelemetryDials(
                    telemetry = telemetry,
                    isRunning = isRunning
                )
            }

            // 4. Connected LAN Devices Section
            item {
                dev.motherofallapps.host.ftp.ui.components.ConnectedDevicesCard(
                    clients = telemetry.connectedClients,
                    isRunning = isRunning,
                    onDisconnectClient = { sessionId ->
                        viewModel.disconnectClient(sessionId)
                        Toast.makeText(context, "Disconnected client $sessionId", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // 5. Connection Details & Credentials Card
            item {
                ConnectionDetailsCard(
                    serverState = serverState,
                    config = config,
                    localIp = networkStatus.localIpAddress,
                    onCopyUrl = { copyToClipboard(context, "FTP URL", it) },
                    onCopyCredentials = { copyToClipboard(context, "Username", it) },
                    onEditSettings = { showConfigDialog = true }
                )
            }

            // 6. Storage Directory & Free Space Info Card
            item {
                StorageInfoCard(
                    currentPath = config.rootPath,
                    storageInfo = storageInfo,
                    hasStoragePermission = hasStoragePermission,
                    isRunning = isRunning,
                    onChangeFolder = { showFolderDialog = true },
                    onRequestPermission = {
                        try {
                            context.startActivity(StorageUtils.createManageStorageIntent(context))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Open Settings -> All files access", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }

            // 7. Live Activity Console
            item {
                LiveActivityConsole(logs = telemetry.recentLogs)
            }

            // 8. Quick Connect Guide Card
            item {
                QuickConnectGuideCard()
            }

            item {
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // Config Dialog
    if (showConfigDialog) {
        FtpConfigDialog(
            initialConfig = config,
            onDismiss = { showConfigDialog = false },
            onSave = { newConfig ->
                viewModel.updateConfig(newConfig)
                showConfigDialog = false
                Toast.makeText(context, "Settings updated", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Folder Picker Dialog
    if (showFolderDialog) {
        FolderPickerDialog(
            currentPath = config.rootPath,
            onDismiss = { showFolderDialog = false },
            onSelectFolder = { selectedPath ->
                viewModel.updateRootPath(selectedPath)
                showFolderDialog = false
                Toast.makeText(context, "Root folder updated", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun QuickConnectGuideCard() {
    IsometricCard(glowColor = Color(0xFF475569)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "HOW TO CONNECT FROM LAN DEVICES",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                letterSpacing = 1.sp,
                fontSize = 11.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "• Windows: Press Win+E -> Address bar -> type ftp://<ip>:<port> -> Enter\n" +
                        "• macOS: Finder -> Cmd+K -> type ftp://<ip>:<port> -> Connect\n" +
                        "• Android / iOS / Linux: Use FileZilla, AndFTP, or Solid Explorer\n" +
                        "• Keep serving: You can minimize or hide this app, background service keeps running seamlessly.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 18.sp
            )
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Copied $label to clipboard", Toast.LENGTH_SHORT).show()
}
