package dev.motherofallapps.host

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.motherofallapps.host.host.HostApplicationContainer
import dev.motherofallapps.host.ui.navigation.HostNavHost
import dev.motherofallapps.host.ui.theme.AppTheme

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