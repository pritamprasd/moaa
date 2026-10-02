package dev.pritam.host.host

import android.content.Context

class HostApplicationContainer(context: Context? = null) {
    init {
        context?.let { dev.pritam.host.settings.AppSettingsManager.init(it) }
    }
    val state = HostAppState(context)
}