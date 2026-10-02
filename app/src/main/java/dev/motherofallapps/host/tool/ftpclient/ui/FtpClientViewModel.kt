package dev.motherofallapps.host.tool.ftpclient.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.motherofallapps.host.logging.AppLogHub
import dev.motherofallapps.host.logging.LogLevel
import dev.motherofallapps.host.tool.ftpclient.engine.FtpClient
import dev.motherofallapps.host.tool.ftpclient.engine.FtpListParser
import dev.motherofallapps.host.tool.ftpclient.model.FtpClientConnectionStatus
import dev.motherofallapps.host.tool.ftpclient.model.FtpClientState
import dev.motherofallapps.host.tool.ftpclient.model.FtpConnectionProfile
import dev.motherofallapps.host.tool.ftpclient.model.FtpRemoteFile
import dev.motherofallapps.host.tool.ftpclient.model.FtpTransferProgress
import dev.motherofallapps.host.tool.ftpclient.storage.FtpProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class FtpClientViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FtpProfileRepository(application)
    private val client = FtpClient()

    private val _state = MutableStateFlow(
        FtpClientState(savedProfiles = repository.loadProfiles())
    )
    val state: StateFlow<FtpClientState> = _state.asStateFlow()

    fun connect(profile: FtpConnectionProfile) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    status = FtpClientConnectionStatus.CONNECTING,
                    connectedProfile = profile,
                    errorMessage = null
                )
            }

            val result = client.connect(profile)
            if (result.isSuccess) {
                val updatedProfiles = repository.updateLastConnected(profile.id)
                _state.update {
                    it.copy(
                        status = FtpClientConnectionStatus.CONNECTED,
                        connectedProfile = profile,
                        currentRemotePath = client.currentWorkingDirectory,
                        serverWelcomeMessage = client.serverGreeting,
                        savedProfiles = updatedProfiles,
                        errorMessage = null
                    )
                }
                refreshFiles()
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to connect"
                _state.update {
                    it.copy(
                        status = FtpClientConnectionStatus.ERROR,
                        errorMessage = err
                    )
                }
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            _state.update { it.copy(status = FtpClientConnectionStatus.DISCONNECTING) }
            client.disconnect()
            _state.update {
                it.copy(
                    status = FtpClientConnectionStatus.DISCONNECTED,
                    connectedProfile = null,
                    remoteFiles = emptyList(),
                    currentRemotePath = "/",
                    activeTransfer = null,
                    errorMessage = null
                )
            }
        }
    }

    fun refreshFiles() {
        viewModelScope.launch {
            if (!_state.value.isConnected) return@launch
            _state.update { it.copy(isRefreshingFiles = true, errorMessage = null) }

            val result = client.listFiles(_state.value.currentRemotePath)
            if (result.isSuccess) {
                _state.update {
                    it.copy(
                        remoteFiles = result.getOrNull() ?: emptyList(),
                        isRefreshingFiles = false,
                        currentRemotePath = client.currentWorkingDirectory
                    )
                }
            } else {
                _state.update {
                    it.copy(
                        isRefreshingFiles = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to list files"
                    )
                }
            }
        }
    }

    fun navigateInto(file: FtpRemoteFile) {
        if (!file.isDirectory) return
        viewModelScope.launch {
            val result = client.changeDirectory(file.path)
            if (result.isSuccess) {
                _state.update { it.copy(currentRemotePath = client.currentWorkingDirectory) }
                refreshFiles()
            } else {
                _state.update { it.copy(errorMessage = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun navigateTo(path: String) {
        viewModelScope.launch {
            val result = client.changeDirectory(path)
            if (result.isSuccess) {
                _state.update { it.copy(currentRemotePath = client.currentWorkingDirectory) }
                refreshFiles()
            } else {
                _state.update { it.copy(errorMessage = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun navigateUp() {
        viewModelScope.launch {
            val result = client.navigateUp()
            if (result.isSuccess) {
                _state.update { it.copy(currentRemotePath = client.currentWorkingDirectory) }
                refreshFiles()
            } else {
                _state.update { it.copy(errorMessage = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun downloadFile(remoteFile: FtpRemoteFile) {
        if (remoteFile.isDirectory) return
        viewModelScope.launch {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                ?: getApplication<Application>().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: getApplication<Application>().filesDir

            val destination = File(downloadsDir, remoteFile.name)

            val initialProgress = FtpTransferProgress(
                fileName = remoteFile.name,
                remotePath = remoteFile.path,
                isUpload = false,
                bytesTransferred = 0L,
                totalBytes = remoteFile.sizeBytes
            )
            _state.update { it.copy(activeTransfer = initialProgress) }

            val result = client.downloadFile(
                remotePath = remoteFile.path,
                localDestinationFile = destination,
                onProgress = { downloaded, total ->
                    _state.update {
                        it.copy(
                            activeTransfer = it.activeTransfer?.copy(
                                bytesTransferred = downloaded,
                                totalBytes = if (total > 0L) total else remoteFile.sizeBytes
                            )
                        )
                    }
                }
            )

            val finalProgress = if (result.isSuccess) {
                initialProgress.copy(
                    bytesTransferred = remoteFile.sizeBytes,
                    isComplete = true
                )
            } else {
                initialProgress.copy(
                    isComplete = true,
                    errorMsg = result.exceptionOrNull()?.message ?: "Download failed"
                )
            }

            _state.update {
                it.copy(
                    activeTransfer = null,
                    transferHistory = listOf(finalProgress) + it.transferHistory.take(20)
                )
            }
        }
    }

    fun uploadFromUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val fileName = queryFileName(context, uri) ?: "upload_${System.currentTimeMillis()}.bin"
            val tempFile = File(context.cacheDir, fileName)

            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }

                val initialProgress = FtpTransferProgress(
                    fileName = fileName,
                    remotePath = FtpListParser.buildFullPath(_state.value.currentRemotePath, fileName),
                    isUpload = true,
                    bytesTransferred = 0L,
                    totalBytes = tempFile.length()
                )
                _state.update { it.copy(activeTransfer = initialProgress) }

                val result = client.uploadFile(
                    localFile = tempFile,
                    remoteFileName = fileName,
                    onProgress = { uploaded, total ->
                        _state.update {
                            it.copy(
                                activeTransfer = it.activeTransfer?.copy(
                                    bytesTransferred = uploaded,
                                    totalBytes = total
                                )
                            )
                        }
                    }
                )

                val finalProgress = if (result.isSuccess) {
                    initialProgress.copy(
                        bytesTransferred = tempFile.length(),
                        isComplete = true
                    )
                } else {
                    initialProgress.copy(
                        isComplete = true,
                        errorMsg = result.exceptionOrNull()?.message ?: "Upload failed"
                    )
                }

                _state.update {
                    it.copy(
                        activeTransfer = null,
                        transferHistory = listOf(finalProgress) + it.transferHistory.take(20)
                    )
                }

                tempFile.delete()
                refreshFiles()
            } catch (e: Exception) {
                AppLogHub.log(
                    toolId = "ftp-client",
                    toolName = "FTP Client",
                    level = LogLevel.ERROR,
                    tag = "ClientTransfer",
                    message = "FILE OPERATION [UPLOAD] Failed reading file URI: ${e.message}",
                    throwable = e
                )
                _state.update { it.copy(errorMessage = "Failed to prepare file: ${e.message}", activeTransfer = null) }
            }
        }
    }

    fun deleteFile(remoteFile: FtpRemoteFile) {
        viewModelScope.launch {
            val result = if (remoteFile.isDirectory) {
                client.removeDirectory(remoteFile.path)
            } else {
                client.deleteFile(remoteFile.path)
            }

            if (result.isSuccess) {
                refreshFiles()
            } else {
                _state.update { it.copy(errorMessage = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun createDirectory(dirName: String) {
        if (dirName.isBlank()) return
        viewModelScope.launch {
            val result = client.createDirectory(dirName.trim())
            if (result.isSuccess) {
                refreshFiles()
            } else {
                _state.update { it.copy(errorMessage = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun renameFile(remoteFile: FtpRemoteFile, newName: String) {
        if (newName.isBlank() || newName == remoteFile.name) return
        viewModelScope.launch {
            val targetPath = FtpListParser.buildFullPath(_state.value.currentRemotePath, newName.trim())
            val result = client.rename(remoteFile.path, targetPath)
            if (result.isSuccess) {
                refreshFiles()
            } else {
                _state.update { it.copy(errorMessage = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun saveProfile(profile: FtpConnectionProfile) {
        val updated = repository.saveProfile(profile)
        _state.update { it.copy(savedProfiles = updated) }
    }

    fun deleteProfile(profileId: String) {
        val updated = repository.deleteProfile(profileId)
        _state.update { it.copy(savedProfiles = updated) }
    }

    fun setSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }

    fun toggleSort(byName: Boolean) {
        _state.update {
            if (it.isSortByName == byName) {
                it.copy(isSortAscending = !it.isSortAscending)
            } else {
                it.copy(isSortByName = byName, isSortAscending = true)
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    private fun queryFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIdx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIdx >= 0) {
                        name = it.getString(nameIdx)
                    }
                }
            }
        }
        if (name == null) {
            name = uri.path?.substringAfterLast('/')
        }
        return name
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            client.disconnect()
        }
    }
}
