package dev.pritam.host.ftp.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections

object NetworkUtils {

    data class NetworkStatus(
        val isConnectedToWifi: Boolean,
        val isHotspotActive: Boolean,
        val localIpAddress: String?,
        val interfaceName: String?,
        val networkName: String?,
    )

    /**
     * Resolves the primary local LAN IPv4 address (e.g. Wi-Fi or tethering/hotspot).
     */
    fun getLocalIpAddress(): String? {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            // Priority 1: wlan0 (Standard Wi-Fi) or ap0 / rndis0 (Hotspot / USB tether)
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                val addresses = Collections.list(intf.inetAddresses)
                for (addr in addresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress ?: continue
                        // Filter out link-local and 127.0.0.1
                        if (!host.startsWith("127.") && !host.startsWith("169.254.")) {
                            return host
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * Obtains full network status info for UI presentation.
     */
    fun getNetworkStatus(context: Context): NetworkStatus {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

        val activeNetwork = connectivityManager?.activeNetwork
        val caps = connectivityManager?.getNetworkCapabilities(activeNetwork)

        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val localIp = getLocalIpAddress()

        var wifiName: String? = null
        if (isWifi && wifiManager != null) {
            try {
                val wifiInfo = wifiManager.connectionInfo
                val ssid = wifiInfo?.ssid?.replace("\"", "")
                if (!ssid.isNullOrBlank() && ssid != "<unknown ssid>") {
                    wifiName = ssid
                }
            } catch (ignored: Exception) {
            }
        }

        val isHotspot = localIp?.startsWith("192.168.43.") == true || 
                        localIp?.startsWith("192.168.50.") == true || 
                        localIp?.startsWith("172.20.10.") == true

        return NetworkStatus(
            isConnectedToWifi = isWifi,
            isHotspotActive = isHotspot,
            localIpAddress = localIp,
            interfaceName = if (isWifi) "Wi-Fi" else if (isHotspot) "Hotspot" else "LAN",
            networkName = wifiName ?: if (isHotspot) "Local Wi-Fi Hotspot" else if (isWifi) "Connected Wi-Fi" else "Local LAN",
        )
    }
}
