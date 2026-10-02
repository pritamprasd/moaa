package dev.motherofallapps.host.ftp.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.motherofallapps.host.ftp.model.FtpConfig
import dev.motherofallapps.host.ftp.model.FtpServerState
import dev.motherofallapps.host.ftp.model.FtpTelemetry
import dev.motherofallapps.host.ftp.network.NetworkUtils
import dev.motherofallapps.host.ftp.service.FtpForegroundService
import dev.motherofallapps.host.ftp.service.FtpServerController
import dev.motherofallapps.host.ftp.storage.StorageUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FtpUiState(
    val serverState: FtpServerState = FtpServerState.Stopped,
    val telemetry: FtpTelemetry = FtpTelemetry(),
    val config: FtpConfig = FtpConfig(),
    val networkStatus: NetworkUtils.NetworkStatus = NetworkUtils.NetworkStatus(
        isConnectedToWifi = false,
        isHotspotActive = false,
        localIpAddress = null,
        interfaceName = null,
        networkName = null
    ),
    val storageInfo: StorageUtils.StorageInfo = StorageUtils.getStorageInfo(),
    val hasStoragePermission: Boolean = true,
)

class FtpViewModel : ViewModel() {

    private val _config = MutableStateFlow(FtpServerController.currentConfig)
    val config: StateFlow<FtpConfig> = _config.asStateFlow()

    val serverState: StateFlow<FtpServerState> = FtpServerController.serverState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FtpServerState.Stopped)

    val telemetry: StateFlow<FtpTelemetry> = FtpServerController.telemetry
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FtpTelemetry())

    private val _networkStatus = MutableStateFlow(
        NetworkUtils.NetworkStatus(false, false, null, null, null)
    )
    val networkStatus: StateFlow<NetworkUtils.NetworkStatus> = _networkStatus.asStateFlow()

    private val _storageInfo = MutableStateFlow(StorageUtils.getStorageInfo())
    val storageInfo: StateFlow<StorageUtils.StorageInfo> = _storageInfo.asStateFlow()

    private val _hasStoragePermission = MutableStateFlow(true)
    val hasStoragePermission: StateFlow<Boolean> = _hasStoragePermission.asStateFlow()

    fun refreshState(context: Context) {
        val net = NetworkUtils.getNetworkStatus(context)
        _networkStatus.value = net
        _storageInfo.value = StorageUtils.getStorageInfo(_config.value.rootPath)
        _hasStoragePermission.value = StorageUtils.hasAllFilesPermission(context)
    }

    fun toggleServer(context: Context) {
        val currentState = serverState.value
        if (currentState is FtpServerState.Running || currentState is FtpServerState.Starting) {
            FtpForegroundService.stop(context)
        } else {
            refreshState(context)
            FtpForegroundService.start(context, _config.value)
        }
    }

    fun updateConfig(newConfig: FtpConfig) {
        _config.value = newConfig
        FtpServerController.updateConfig(newConfig)
        _storageInfo.value = StorageUtils.getStorageInfo(newConfig.rootPath)
    }

    fun updateRootPath(path: String) {
        val updated = _config.value.copy(rootPath = path)
        updateConfig(updated)
    }

    fun disconnectClient(sessionId: String) {
        FtpServerController.engine.disconnectClient(sessionId)
    }
}
