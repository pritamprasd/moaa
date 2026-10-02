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
import dev.motherofallapps.host.tool.logviewer.ui.LogViewerScreen
import dev.motherofallapps.host.tool.nfc.ui.NfcScreen
import dev.motherofallapps.host.ui.dashboard.DashboardScreen

object HostRoutes {
    const val DASHBOARD = "dashboard"
    const val FTP_SERVER = "ftp_server"
    const val NFC_TOOL = "nfc_tool"
    const val LOG_VIEWER = "log_viewer"
}

@Composable
fun HostNavHost(
    hostAppState: HostAppState,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
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
                        "nfc-tool" -> navController.navigate(HostRoutes.NFC_TOOL)
                        "log-viewer" -> navController.navigate(HostRoutes.LOG_VIEWER)
                    }
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
    }
}