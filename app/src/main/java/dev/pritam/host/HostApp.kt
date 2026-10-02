package dev.pritam.host

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.pritam.host.host.HostApplicationContainer
import dev.pritam.host.ui.navigation.HostNavHost
import dev.pritam.host.ui.theme.AppTheme

@Composable
fun HostApp(
    container: HostApplicationContainer,
    modifier: Modifier = Modifier,
) {
    AppTheme {
        HostNavHost(
            hostAppState = container.state,
            modifier = modifier,
        )
    }
}