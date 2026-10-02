package dev.pritam.host.host

import android.content.Context

class HostApplicationContainer(context: Context? = null) {
    val state = HostAppState(context)
}