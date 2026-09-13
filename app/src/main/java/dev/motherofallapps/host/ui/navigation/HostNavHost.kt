package dev.motherofallapps.host.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavHostController
import dev.motherofallapps.host.host.HostAppState
import dev.motherofallapps.host.ui.dashboard.DashboardScreen

object HostRoutes {
    const val DASHBOARD = "dashboard"
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
            DashboardScreen(tools = tools)
        }
    }
}