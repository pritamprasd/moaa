package dev.pritam.host.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavHostController
import dev.pritam.dynamictools.ui.runner.DynamicToolRunnerScreen
import dev.pritam.dynamictools.ui.studio.DynamicToolsStudioScreen
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

import dev.pritam.host.tool.manual.ui.SystemManualScreen
import dev.pritam.ghostagent.ui.GhostAgentStudioScreen
import dev.pritam.host.tool.nettopology.ui.NetTopologyScreen
import dev.pritam.host.tool.sysinfo.ui.SysInfoScreen
import dev.pritam.host.tool.terminal.ui.TerminalScreen

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
    const val DYNAMIC_TOOLS_STUDIO = "dynamic_tools_studio"
    const val DYNAMIC_TOOL_RUNNER = "dynamic_tool_runner/{toolId}"
    const val SYSTEM_MANUAL = "system_manual"
    const val GHOST_AGENT = "ghost_agent"
    const val TERMINAL = "terminal"
    const val SYS_INFO = "sys_info"
    const val NET_TOPOLOGY = "net_topology"
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
            val route = when {
                toolId == "ftp-server" -> HostRoutes.FTP_SERVER
                toolId == "ftp-client" -> HostRoutes.FTP_CLIENT
                toolId == "nfc-tool" -> HostRoutes.NFC_TOOL
                toolId == "log-viewer" -> HostRoutes.LOG_VIEWER
                toolId == "sensors" -> HostRoutes.SENSORS
                toolId == "llm-gateway" -> HostRoutes.LLM_GATEWAY
                toolId == "llm-chat" -> HostRoutes.LLM_CHAT
                toolId == "dynamic-tools-studio" -> HostRoutes.DYNAMIC_TOOLS_STUDIO
                toolId == "system-manual" -> HostRoutes.SYSTEM_MANUAL
                toolId == "ghost-agent" -> HostRoutes.GHOST_AGENT
                toolId == "terminal" -> HostRoutes.TERMINAL
                toolId == "system-info" -> HostRoutes.SYS_INFO
                toolId == "net-topology" -> HostRoutes.NET_TOPOLOGY
                toolId.startsWith("dynamic_") -> "dynamic_tool_runner/${toolId.removePrefix("dynamic_")}"
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
                    when {
                        tool.id.value == "ftp-server" -> navController.navigate(HostRoutes.FTP_SERVER)
                        tool.id.value == "ftp-client" -> navController.navigate(HostRoutes.FTP_CLIENT)
                        tool.id.value == "nfc-tool" -> navController.navigate(HostRoutes.NFC_TOOL)
                        tool.id.value == "log-viewer" -> navController.navigate(HostRoutes.LOG_VIEWER)
                        tool.id.value == "sensors" -> navController.navigate(HostRoutes.SENSORS)
                        tool.id.value == "llm-gateway" -> navController.navigate(HostRoutes.LLM_GATEWAY)
                        tool.id.value == "llm-chat" -> navController.navigate(HostRoutes.LLM_CHAT)
                        tool.id.value == "dynamic-tools-studio" -> navController.navigate(HostRoutes.DYNAMIC_TOOLS_STUDIO)
                        tool.id.value == "system-manual" -> navController.navigate(HostRoutes.SYSTEM_MANUAL)
                        tool.id.value == "ghost-agent" -> navController.navigate(HostRoutes.GHOST_AGENT)
                        tool.id.value == "terminal" -> navController.navigate(HostRoutes.TERMINAL)
                        tool.id.value == "system-info" -> navController.navigate(HostRoutes.SYS_INFO)
                        tool.id.value == "net-topology" -> navController.navigate(HostRoutes.NET_TOPOLOGY)
                        tool.id.value.startsWith("dynamic_") -> {
                            val customId = tool.id.value.removePrefix("dynamic_")
                            navController.navigate("dynamic_tool_runner/$customId")
                        }
                    }
                },
                onOpenSettings = {
                    navController.navigate(HostRoutes.SETTINGS)
                },
                onReorderTools = { fromIndex, toIndex ->
                    hostAppState.reorderTools(fromIndex, toIndex)
                },
                onResetOrder = {
                    hostAppState.resetToolOrder()
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
                },
                onOpenSystemManual = {
                    navController.navigate(HostRoutes.SYSTEM_MANUAL)
                }
            )
        }

        composable(HostRoutes.SYSTEM_MANUAL) {
            SystemManualScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToTool = { routeId ->
                    when (routeId) {
                        "ftp-server" -> navController.navigate(HostRoutes.FTP_SERVER)
                        "ftp-client" -> navController.navigate(HostRoutes.FTP_CLIENT)
                        "nfc-tool" -> navController.navigate(HostRoutes.NFC_TOOL)
                        "log-viewer" -> navController.navigate(HostRoutes.LOG_VIEWER)
                        "sensors" -> navController.navigate(HostRoutes.SENSORS)
                        "llm-gateway" -> navController.navigate(HostRoutes.LLM_GATEWAY)
                        "llm-chat" -> navController.navigate(HostRoutes.LLM_CHAT)
                        "dynamic-tools-studio" -> navController.navigate(HostRoutes.DYNAMIC_TOOLS_STUDIO)
                        "ghost-agent" -> navController.navigate(HostRoutes.GHOST_AGENT)
                        "terminal" -> navController.navigate(HostRoutes.TERMINAL)
                        "system-info" -> navController.navigate(HostRoutes.SYS_INFO)
                        "net-topology" -> navController.navigate(HostRoutes.NET_TOPOLOGY)
                        "settings" -> navController.navigate(HostRoutes.SETTINGS)
                        else -> {
                            if (routeId.startsWith("dynamic_")) {
                                navController.navigate("dynamic_tool_runner/${routeId.removePrefix("dynamic_")}")
                            }
                        }
                    }
                }
            )
        }

        composable(HostRoutes.GHOST_AGENT) {
            GhostAgentStudioScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(HostRoutes.TERMINAL) {
            TerminalScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(HostRoutes.SYS_INFO) {
            SysInfoScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(HostRoutes.NET_TOPOLOGY) {
            NetTopologyScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(HostRoutes.DYNAMIC_TOOLS_STUDIO) {
            DynamicToolsStudioScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLaunchTool = { toolId ->
                    navController.navigate("dynamic_tool_runner/$toolId")
                }
            )
        }

        composable(HostRoutes.DYNAMIC_TOOL_RUNNER) { backStackEntry ->
            val toolId = backStackEntry.arguments?.getString("toolId") ?: ""
            DynamicToolRunnerScreen(
                toolId = toolId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}