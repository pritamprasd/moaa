package dev.motherofallapps.host

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import android.os.Bundle
import dev.motherofallapps.host.host.HostApplicationContainer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            HostApp(container = remember { HostApplicationContainer() })
        }
    }
}