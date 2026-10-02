package dev.motherofallapps.host.tool.ftpclient.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.motherofallapps.host.ftp.ui.components.IsometricCard
import dev.motherofallapps.host.logging.AppLogHub
import dev.motherofallapps.host.tool.ftpclient.model.FtpClientConnectionStatus
import dev.motherofallapps.host.tool.ftpclient.model.FtpClientState
import dev.motherofallapps.host.tool.ftpclient.model.FtpConnectionProfile
import dev.motherofallapps.host.tool.ftpclient.model.FtpRemoteFile
import dev.motherofallapps.host.tool.ftpclient.model.FtpTransferProgress
import dev.motherofallapps.host.ui.theme.Cyan
import dev.motherofallapps.host.ui.theme.Rose
import dev.motherofallapps.host.ui.theme.SpaceBackground
import dev.motherofallapps.host.ui.theme.SurfaceDeep
import dev.motherofallapps.host.ui.theme.SurfaceElevated
import dev.motherofallapps.host.ui.theme.TextPrimary
import dev.motherofallapps.host.ui.theme.TextSecondary
import dev.motherofallapps.host.ui.theme.Violet

private val Emerald = Color(0xFF34D399)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FtpClientScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FtpClientViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    var showProfileModal by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<FtpConnectionProfile?>(null) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var selectedFileForAction by remember { mutableStateOf<FtpRemoteFile?>(null) }
    var fileToRename by remember { mutableStateOf<FtpRemoteFile?>(null) }
    var fileForDetails by remember { mutableStateOf<FtpRemoteFile?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.uploadFromUri(context, it) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "FTP CLIENT",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("← Back", color = Emerald)
                    }
                },
                actions = {
                    TextButton(onClick = { showProfileModal = true }) {
                        Text("⚡ Profiles", color = Emerald, fontSize = 12.sp)
                    }
                    if (state.isConnected) {
                        TextButton(onClick = { viewModel.disconnect() }) {
                            Text("Disconnect", color = Rose, fontSize = 12.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Error Alert Banner
            if (state.errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Rose.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Rose.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚠️ ${state.errorMessage}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Rose,
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = { viewModel.clearError() },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text("Dismiss", color = Rose, fontSize = 10.sp)
                        }
                    }
                }
            }

            // Live Transfer Banner
            state.activeTransfer?.let { transfer ->
                ActiveTransferCard(transfer = transfer)
                Spacer(Modifier.height(10.dp))
            }

            if (!state.isConnected) {
                // Disconnected State: Quick Mount & Saved Profiles
                DisconnectedMountPanel(
                    state = state,
                    onConnect = { viewModel.connect(it) },
                    onSaveProfile = { viewModel.saveProfile(it) },
                    onDeleteProfile = { viewModel.deleteProfile(it) },
                    onEditProfile = { editingProfile = it }
                )
            } else {
                // Connected State: Remote File Browser
                ConnectedBrowserSection(
                    state = state,
                    onNavigateInto = { viewModel.navigateInto(it) },
                    onNavigateTo = { viewModel.navigateTo(it) },
                    onNavigateUp = { viewModel.navigateUp() },
                    onRefresh = { viewModel.refreshFiles() },
                    onOpenFilePicker = { filePickerLauncher.launch("*/*") },
                    onNewFolder = { showNewFolderDialog = true },
                    onFileClick = { file ->
                        if (file.isDirectory) {
                            viewModel.navigateInto(file)
                        } else {
                            selectedFileForAction = file
                        }
                    },
                    onSearchChange = { viewModel.setSearchQuery(it) },
                    onToggleSort = { viewModel.toggleSort(it) }
                )
            }
        }
    }

    // Dialogs
    if (showProfileModal || editingProfile != null) {
        ProfileManagerDialog(
            initialProfile = editingProfile,
            savedProfiles = state.savedProfiles,
            onDismiss = {
                showProfileModal = false
                editingProfile = null
            },
            onConnect = { profile ->
                showProfileModal = false
                editingProfile = null
                viewModel.connect(profile)
            },
            onSave = { profile ->
                viewModel.saveProfile(profile)
                editingProfile = null
            },
            onDelete = { profileId ->
                viewModel.deleteProfile(profileId)
                if (editingProfile?.id == profileId) editingProfile = null
            }
        )
    }

    if (showNewFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showNewFolderDialog = false },
            onCreate = { folderName ->
                viewModel.createDirectory(folderName)
                showNewFolderDialog = false
            }
        )
    }

    selectedFileForAction?.let { file ->
        FileActionsDialog(
            file = file,
            onDismiss = { selectedFileForAction = null },
            onDownload = {
                viewModel.downloadFile(file)
                selectedFileForAction = null
            },
            onRename = {
                fileToRename = file
                selectedFileForAction = null
            },
            onDelete = {
                viewModel.deleteFile(file)
                selectedFileForAction = null
            },
            onDetails = {
                fileForDetails = file
                selectedFileForAction = null
            },
            onCopyPath = {
                copyToClipboard(context, "Remote Path", file.path)
                selectedFileForAction = null
            }
        )
    }

    fileToRename?.let { file ->
        RenameFileDialog(
            file = file,
            onDismiss = { fileToRename = null },
            onRename = { newName ->
                viewModel.renameFile(file, newName)
                fileToRename = null
            }
        )
    }

    fileForDetails?.let { file ->
        FileDetailsDialog(
            file = file,
            onDismiss = { fileForDetails = null },
            onCopy = { label, text -> copyToClipboard(context, label, text) }
        )
    }
}

@Composable
private fun DisconnectedMountPanel(
    state: FtpClientState,
    onConnect: (FtpConnectionProfile) -> Unit,
    onSaveProfile: (FtpConnectionProfile) -> Unit,
    onDeleteProfile: (String) -> Unit,
    onEditProfile: (FtpConnectionProfile) -> Unit,
) {
    var host by remember { mutableStateOf("127.0.0.1") }
    var portText by remember { mutableStateOf("2121") }
    var username by remember { mutableStateOf("admin") }
    var password by remember { mutableStateOf("password") }
    var isAnonymous by remember { mutableStateOf(false) }
    var isPassive by remember { mutableStateOf(true) }
    var rememberProfile by remember { mutableStateOf(true) }
    var profileName by remember { mutableStateOf("Local Server") }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp)
    ) {
        // Quick Connect Card
        IsometricCard(glowColor = Emerald) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MOUNT REMOTE FTP SERVER",
                        style = MaterialTheme.typography.labelSmall,
                        color = Emerald,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                    if (state.status == FtpClientConnectionStatus.CONNECTING) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Emerald, strokeWidth = 2.dp)
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Host & Port Row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text("Server Host / IP", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(2f),
                        colors = customFieldColors(Emerald)
                    )

                    OutlinedTextField(
                        value = portText,
                        onValueChange = { portText = it },
                        label = { Text("Port", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = customFieldColors(Emerald)
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Anonymous Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Anonymous Authentication", style = MaterialTheme.typography.labelSmall, color = TextPrimary, fontSize = 11.sp)
                        Text("Login without password as 'anonymous'", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 9.sp)
                    }
                    Switch(
                        checked = isAnonymous,
                        onCheckedChange = { isAnonymous = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Emerald, checkedTrackColor = Emerald.copy(alpha = 0.3f))
                    )
                }

                if (!isAnonymous) {
                    Spacer(Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text("Username", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = customFieldColors(Emerald)
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password", fontSize = 11.sp) },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = customFieldColors(Emerald)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Passive mode & Remember toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Passive Mode (PASV)", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 10.sp)
                    Switch(
                        checked = isPassive,
                        onCheckedChange = { isPassive = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Emerald, checkedTrackColor = Emerald.copy(alpha = 0.3f))
                    )
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Remember Profile & Credentials", style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 10.sp)
                    Switch(
                        checked = rememberProfile,
                        onCheckedChange = { rememberProfile = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Emerald, checkedTrackColor = Emerald.copy(alpha = 0.3f))
                    )
                }

                if (rememberProfile) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = profileName,
                        onValueChange = { profileName = it },
                        label = { Text("Profile Name (e.g. My Phone FTP)", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = customFieldColors(Emerald)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        val port = portText.toIntOrNull() ?: 21
                        val profile = FtpConnectionProfile(
                            name = if (rememberProfile && profileName.isNotBlank()) profileName.trim() else host,
                            host = host.trim(),
                            port = port,
                            username = if (isAnonymous) "anonymous" else username.trim(),
                            password = if (isAnonymous) "" else password,
                            isPassiveMode = isPassive,
                            isAnonymous = isAnonymous
                        )
                        if (rememberProfile) {
                            onSaveProfile(profile)
                        }
                        onConnect(profile)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = Color(0xFF0F172A))
                ) {
                    Text("MOUNT REMOTE FTP SERVER", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Saved Profiles Vault
        IsometricCard(glowColor = Violet) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SAVED SERVERS & CREDENTIALS (${state.savedProfiles.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = Violet,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(Modifier.height(10.dp))

                if (state.savedProfiles.isEmpty()) {
                    Text("No saved server connections yet", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 11.sp)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.savedProfiles.forEach { profile ->
                            SavedProfileItemCard(
                                profile = profile,
                                onQuickConnect = { onConnect(profile) },
                                onEdit = { onEditProfile(profile) },
                                onDelete = { onDeleteProfile(profile.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectedBrowserSection(
    state: FtpClientState,
    onNavigateInto: (FtpRemoteFile) -> Unit,
    onNavigateTo: (String) -> Unit,
    onNavigateUp: () -> Unit,
    onRefresh: () -> Unit,
    onOpenFilePicker: () -> Unit,
    onNewFolder: () -> Unit,
    onFileClick: (FtpRemoteFile) -> Unit,
    onSearchChange: (String) -> Unit,
    onToggleSort: (Boolean) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Connected Server Telemetry Banner
        IsometricCard(glowColor = Emerald, elevationDepth = 2.dp) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = state.connectedProfile?.name ?: "Remote FTP",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Surface(shape = RoundedCornerShape(4.dp), color = Emerald.copy(alpha = 0.2f)) {
                            Text("● ONLINE", color = Emerald, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }
                    Text(
                        text = "${state.connectedProfile?.host}:${state.connectedProfile?.port} (${state.connectedProfile?.username})",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onOpenFilePicker,
                        border = BorderStroke(1.dp, Emerald),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Emerald),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("📤 Upload", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onNewFolder,
                        border = BorderStroke(1.dp, Cyan),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Cyan),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("📁+ Folder", fontSize = 10.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Path Breadcrumbs & Navigation Bar
        PathBreadcrumbBar(
            currentPath = state.currentRemotePath,
            onNavigateUp = onNavigateUp,
            onNavigateTo = onNavigateTo,
            onRefresh = onRefresh,
            isRefreshing = state.isRefreshingFiles
        )

        Spacer(Modifier.height(8.dp))

        // Search & Filter Bar
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Filter remote files...", fontSize = 11.sp) },
            singleLine = true,
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    TextButton(onClick = { onSearchChange("") }) {
                        Text("Clear", fontSize = 10.sp, color = TextSecondary)
                    }
                }
            },
            colors = customFieldColors(Emerald),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        )

        Spacer(Modifier.height(8.dp))

        // Remote Files List
        val filtered = state.filteredFiles
        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDeep)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Directory is empty or no matches found", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Spacer(Modifier.height(6.dp))
                    Text("Tap 'Upload' to send files or 'Folder' to create directories", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 11.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(filtered, key = { it.path }) { file ->
                    RemoteFileListItem(
                        file = file,
                        onClick = { onFileClick(file) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PathBreadcrumbBar(
    currentPath: String,
    onNavigateUp: () -> Unit,
    onNavigateTo: (String) -> Unit,
    onRefresh: () -> Unit,
    isRefreshing: Boolean,
) {
    val scrollState = rememberScrollState()
    val segments = remember(currentPath) {
        val raw = currentPath.trim().split('/').filter { it.isNotBlank() }
        listOf("/") + raw
    }

    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
        color = SurfaceDeep,
        border = BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Up button
            TextButton(
                onClick = onNavigateUp,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                modifier = Modifier.height(24.dp)
            ) {
                Text("⬆️", fontSize = 12.sp)
            }

            // Path segments
            Row(
                modifier = Modifier.weight(1f).horizontalScroll(scrollState),
                verticalAlignment = Alignment.CenterVertically
            ) {
                var accumulated = ""
                segments.forEachIndexed { idx, segment ->
                    accumulated = if (segment == "/") "/" else "$accumulated/$segment".replace("//", "/")
                    val target = accumulated
                    val isLast = idx == segments.size - 1

                    Text(
                        text = segment,
                        color = if (isLast) Emerald else Cyan,
                        fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onNavigateTo(target) }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                    if (!isLast && segment != "/") {
                        Text("/", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }

            // Refresh button
            TextButton(
                onClick = onRefresh,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                modifier = Modifier.height(24.dp)
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(12.dp), color = Emerald, strokeWidth = 1.5.dp)
                } else {
                    Text("🔄", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun RemoteFileListItem(
    file: FtpRemoteFile,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    val itemColor = if (file.isDirectory) Cyan else Emerald

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable { onClick() }
            .border(BorderStroke(1.dp, Color(0xFF1E293B)), shape),
        color = SurfaceElevated.copy(alpha = 0.9f),
        shape = shape
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Icon
                Text(
                    text = if (file.isDirectory) "📁" else getFileEmoji(file.fileExtension),
                    fontSize = 16.sp
                )

                Column {
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (file.isDirectory) FontWeight.Bold else FontWeight.Normal,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = file.formattedSize,
                            style = MaterialTheme.typography.labelSmall,
                            color = itemColor,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        if (file.lastModifiedFormatted.isNotBlank()) {
                            Text(
                                text = file.lastModifiedFormatted,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }

            Text(
                text = if (file.isDirectory) "→" else "⋮",
                color = TextSecondary,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun ActiveTransferCard(transfer: FtpTransferProgress) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)),
        color = Color(0xFF0F172A),
        border = BorderStroke(1.dp, Emerald.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (transfer.isUpload) "📤 UPLOADING" else "📥 DOWNLOADING", style = MaterialTheme.typography.labelSmall, color = Emerald, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                    Text(transfer.fileName, style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
                Text("${transfer.progressPercent}%", style = MaterialTheme.typography.labelSmall, color = Emerald, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { transfer.progressFraction },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = Emerald,
                trackColor = SurfaceDeep
            )

            Spacer(Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "${transfer.bytesTransferred / 1024} KB / ${if (transfer.totalBytes > 0) "${transfer.totalBytes / 1024} KB" else "..."}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = transfer.formattedSpeed,
                    style = MaterialTheme.typography.labelSmall,
                    color = Emerald,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun SavedProfileItemCard(
    profile: FtpConnectionProfile,
    onQuickConnect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
        color = SurfaceDeep,
        border = BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(profile.name, style = MaterialTheme.typography.labelSmall, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text("${profile.host}:${profile.port} (${profile.username})", style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(
                    onClick = onQuickConnect,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Emerald),
                    border = BorderStroke(1.dp, Emerald.copy(alpha = 0.6f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(26.dp)
                ) {
                    Text("Connect", fontSize = 10.sp)
                }

                TextButton(onClick = onEdit, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(26.dp)) {
                    Text("✏️", fontSize = 11.sp)
                }

                TextButton(onClick = onDelete, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(26.dp)) {
                    Text("🗑️", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun ProfileManagerDialog(
    initialProfile: FtpConnectionProfile?,
    savedProfiles: List<FtpConnectionProfile>,
    onDismiss: () -> Unit,
    onConnect: (FtpConnectionProfile) -> Unit,
    onSave: (FtpConnectionProfile) -> Unit,
    onDelete: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initialProfile?.name ?: "") }
    var host by remember { mutableStateOf(initialProfile?.host ?: "") }
    var portText by remember { mutableStateOf(initialProfile?.port?.toString() ?: "21") }
    var username by remember { mutableStateOf(initialProfile?.username ?: "anonymous") }
    var password by remember { mutableStateOf(initialProfile?.password ?: "") }
    var defaultPath by remember { mutableStateOf(initialProfile?.defaultRemotePath ?: "/") }
    var isAnonymous by remember { mutableStateOf(initialProfile?.isAnonymous ?: false) }
    var isPassive by remember { mutableStateOf(initialProfile?.isPassiveMode ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceElevated,
        title = {
            Text(if (initialProfile != null) "Edit Server Profile" else "New Connection Profile", color = Emerald, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Profile Name", fontSize = 10.sp) }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = customFieldColors(Emerald))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("Host", fontSize = 10.sp) }, singleLine = true, modifier = Modifier.weight(2f), colors = customFieldColors(Emerald))
                    OutlinedTextField(value = portText, onValueChange = { portText = it }, label = { Text("Port", fontSize = 10.sp) }, singleLine = true, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = customFieldColors(Emerald))
                }
                Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Anonymous Login", fontSize = 11.sp, color = TextPrimary)
                    Switch(checked = isAnonymous, onCheckedChange = { isAnonymous = it }, colors = SwitchDefaults.colors(checkedThumbColor = Emerald, checkedTrackColor = Emerald.copy(alpha = 0.3f)))
                }
                if (!isAnonymous) {
                    OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username", fontSize = 10.sp) }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = customFieldColors(Emerald))
                    OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password", fontSize = 10.sp) }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth(), colors = customFieldColors(Emerald))
                }
                OutlinedTextField(value = defaultPath, onValueChange = { defaultPath = it }, label = { Text("Default Remote Path", fontSize = 10.sp) }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = customFieldColors(Emerald))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = FtpConnectionProfile(
                        id = initialProfile?.id ?: java.util.UUID.randomUUID().toString(),
                        name = if (name.isNotBlank()) name.trim() else host.trim(),
                        host = host.trim(),
                        port = portText.toIntOrNull() ?: 21,
                        username = if (isAnonymous) "anonymous" else username.trim(),
                        password = if (isAnonymous) "" else password,
                        defaultRemotePath = if (defaultPath.isNotBlank()) defaultPath.trim() else "/",
                        isPassiveMode = isPassive,
                        isAnonymous = isAnonymous
                    )
                    onSave(p)
                    onConnect(p)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = Color(0xFF0F172A))
            ) {
                Text("Save & Connect", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary, fontSize = 11.sp)
            }
        }
    )
}

@Composable
private fun FileActionsDialog(
    file: FtpRemoteFile,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onDetails: () -> Unit,
    onCopyPath: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceElevated,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (file.isDirectory) "📁" else "📄", fontSize = 16.sp)
                Text(file.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (!file.isDirectory) {
                    OutlinedButton(onClick = onDownload, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, Emerald), colors = ButtonDefaults.outlinedButtonColors(contentColor = Emerald)) {
                        Text("⬇️ Download to Phone (Downloads)", fontSize = 11.sp)
                    }
                }
                OutlinedButton(onClick = onRename, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, Cyan), colors = ButtonDefaults.outlinedButtonColors(contentColor = Cyan)) {
                    Text("✏️ Rename", fontSize = 11.sp)
                }
                OutlinedButton(onClick = onCopyPath, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, Violet), colors = ButtonDefaults.outlinedButtonColors(contentColor = Violet)) {
                    Text("📋 Copy Remote Path", fontSize = 11.sp)
                }
                OutlinedButton(onClick = onDetails, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, Color(0xFF64748B)), colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)) {
                    Text("ℹ️ View Details", fontSize = 11.sp)
                }
                OutlinedButton(onClick = onDelete, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, Rose), colors = ButtonDefaults.outlinedButtonColors(contentColor = Rose)) {
                    Text("🗑️ Delete", fontSize = 11.sp)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary, fontSize = 11.sp)
            }
        }
    )
}

@Composable
private fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var folderName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceElevated,
        title = { Text("Create Remote Directory", color = Emerald, fontSize = 13.sp, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = folderName,
                onValueChange = { folderName = it },
                label = { Text("Directory Name", fontSize = 10.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = customFieldColors(Emerald)
            )
        },
        confirmButton = {
            Button(
                onClick = { onCreate(folderName) },
                colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = Color(0xFF0F172A))
            ) {
                Text("Create", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary, fontSize = 11.sp)
            }
        }
    )
}

@Composable
private fun RenameFileDialog(
    file: FtpRemoteFile,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
) {
    var newName by remember { mutableStateOf(file.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceElevated,
        title = { Text("Rename ${if (file.isDirectory) "Directory" else "File"}", color = Cyan, fontSize = 13.sp, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("New Name", fontSize = 10.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = customFieldColors(Cyan)
            )
        },
        confirmButton = {
            Button(
                onClick = { onRename(newName) },
                colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Color(0xFF0F172A))
            ) {
                Text("Rename", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary, fontSize = 11.sp)
            }
        }
    )
}

@Composable
private fun FileDetailsDialog(
    file: FtpRemoteFile,
    onDismiss: () -> Unit,
    onCopy: (String, String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceElevated,
        title = { Text("Remote Object Details", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DetailRow("Name", file.name)
                DetailRow("Type", if (file.isDirectory) "Directory" else "File")
                DetailRow("Size", file.formattedSize)
                DetailRow("Path", file.path)
                if (file.permissions.isNotBlank()) DetailRow("Permissions", file.permissions)
                if (file.lastModifiedFormatted.isNotBlank()) DetailRow("Modified", file.lastModifiedFormatted)
                if (file.rawLine.isNotBlank()) DetailRow("Raw FTP Record", file.rawLine)
            }
        },
        confirmButton = {
            Button(
                onClick = { onCopy("File Info", "${file.name} (${file.path}) - ${file.formattedSize}") },
                colors = ButtonDefaults.buttonColors(containerColor = Emerald, contentColor = Color(0xFF0F172A))
            ) {
                Text("Copy Info", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary, fontSize = 11.sp)
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
        Text(value, style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun customFieldColors(accent: Color) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = accent,
    unfocusedBorderColor = Color(0xFF334155),
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedLabelColor = accent,
    unfocusedLabelColor = TextSecondary
)

private fun getFileEmoji(ext: String): String = when (ext) {
    "mp4", "mkv", "avi", "mov" -> "🎬"
    "mp3", "wav", "flac", "ogg" -> "🎵"
    "jpg", "jpeg", "png", "webp", "gif" -> "🖼️"
    "pdf", "doc", "docx", "txt", "md" -> "📄"
    "zip", "rar", "tar", "gz", "7z" -> "📦"
    "apk", "aab" -> "📱"
    "json", "xml", "csv", "sql" -> "📊"
    else -> "📄"
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    AppLogHub.logClipboardOperation(
        toolId = "ftp-client",
        toolName = "FTP Client",
        operationType = "COPY",
        label = label,
        content = text
    )
    Toast.makeText(context, "Copied $label to clipboard", Toast.LENGTH_SHORT).show()
}
