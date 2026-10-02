package dev.pritam.host.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavHostController
import dev.pritam.host.ftp.ui.FtpScreen
import dev.pritam.host.host.HostAppState
import dev.pritam.host.settings.ui.SettingsScreen
import dev.pritam.host.tool.ftpclient.ui.FtpClientScreen
import dev.pritam.host.tool.llmchat.ui.LlmChatScreen
import dev.pritam.host.tool.llmgateway.ui.LlmGatewayScreen
import dev.pritam.host.tool.logviewer.ui.LogViewerScreen
import dev.pritam.host.tool.nfc.ui.NfcScreen
import dev.pritam.host.tool.sensors.ui.SensorsScreen
import dev.pritam.host.ui.dashboard.DashboardScreen

object HostRoutes {
    const val DASHBOARD = "dashboard"
    const val FTP_SERVER = "ftp_server"
    const val FTP_CLIENT = "ftp_client"
    const val NFC_TOOL = "nfc_tool"
    const val LOG_VIEWER = "log_viewer"
    const val SENSORS = "sensors"
    const val LLM_GATEWAY = "llm_gateway"
    const val LLM_CHAT = "llm_chat"
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
                "llm-gateway" -> HostRoutes.LLM_GATEWAY
                "llm-chat" -> HostRoutes.LLM_CHAT
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
                        "llm-gateway" -> navController.navigate(HostRoutes.LLM_GATEWAY)
                        "llm-chat" -> navController.navigate(HostRoutes.LLM_CHAT)
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

        composable(HostRoutes.LLM_GATEWAY) {
            LlmGatewayScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(HostRoutes.LLM_CHAT) {
            LlmChatScreen(
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