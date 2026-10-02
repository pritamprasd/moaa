package dev.motherofallapps.host.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavHostController
import dev.motherofallapps.host.ftp.ui.FtpScreen
import dev.motherofallapps.host.host.HostAppState
import dev.motherofallapps.host.settings.ui.SettingsScreen
import dev.motherofallapps.host.tool.ftpclient.ui.FtpClientScreen
import dev.motherofallapps.host.tool.logviewer.ui.LogViewerScreen
import dev.motherofallapps.host.tool.nfc.ui.NfcScreen
import dev.motherofallapps.host.tool.sensors.ui.SensorsScreen
import dev.motherofallapps.host.ui.dashboard.DashboardScreen

object HostRoutes {
    const val DASHBOARD = "dashboard"
    const val FTP_SERVER = "ftp_server"
    const val FTP_CLIENT = "ftp_client"
    const val NFC_TOOL = "nfc_tool"
    const val LOG_VIEWER = "log_viewer"
    const val SENSORS = "sensors"
    const val SETTINGS = "settings"
}

@Composable
fun HostNavHost(
    hostAppState: HostAppState,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val pendingTargetToolId by hostAppState.pendingTargetToolId.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(pendingTargetToolId) {
        pendingTargetToolId?.let { toolId ->
            val route = when (toolId) {
                "ftp-server" -> HostRoutes.FTP_SERVER
                "ftp-client" -> HostRoutes.FTP_CLIENT
                "nfc-tool" -> HostRoutes.NFC_TOOL
                "log-viewer" -> HostRoutes.LOG_VIEWER
                "sensors" -> HostRoutes.SENSORS
                else -> null
            }
            if (route != null) {
                hostAppState.clearPendingTargetToolId()
                navController.navigate(route) {
                    launchSingleTop = true
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = HostRoutes.DASHBOARD,
        modifier = modifier,
    ) {
        composable(HostRoutes.DASHBOARD) {
            val tools by hostAppState.tools.collectAsStateWithLifecycle()
            DashboardScreen(
                tools = tools,
                onToolClick = { tool ->
                    when (tool.id.value) {
                        "ftp-server" -> navController.navigate(HostRoutes.FTP_SERVER)
                        "ftp-client" -> navController.navigate(HostRoutes.FTP_CLIENT)
                        "nfc-tool" -> navController.navigate(HostRoutes.NFC_TOOL)
                        "log-viewer" -> navController.navigate(HostRoutes.LOG_VIEWER)
                        "sensors" -> navController.navigate(HostRoutes.SENSORS)
                    }
                },
                onOpenSettings = {
                    navController.navigate(HostRoutes.SETTINGS)
                }
            )
        }

        composable(HostRoutes.FTP_SERVER) {
            FtpScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(HostRoutes.FTP_CLIENT) {
            FtpClientScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(HostRoutes.NFC_TOOL) {
            NfcScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(HostRoutes.LOG_VIEWER) {
            LogViewerScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(HostRoutes.SENSORS) {
            SensorsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(HostRoutes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onOpenLogViewer = {
                    navController.navigate(HostRoutes.LOG_VIEWER)
                }
            )
        }
    }
}