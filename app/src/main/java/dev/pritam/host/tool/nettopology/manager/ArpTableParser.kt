package dev.pritam.host.tool.nettopology.manager

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.Locale

object ArpTableParser {

    /**
     * Reads `/proc/net/arp` and returns a map of IP Address -> MAC Address (uppercase).
     * Format of /proc/net/arp:
     * IP address       HW type     Flags       HW address            Mask     Device
     * 192.168.1.1      0x1         0x2         c0:06:c3:xx:xx:xx     *        wlan0
     */
    fun readArpTable(): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val arpPath = File("/proc/net/arp")
        if (!arpPath.exists() || !arpPath.canRead()) {
            return result
        }

        try {
            BufferedReader(FileReader(arpPath)).use { reader ->
                var line: String? = reader.readLine() // Skip header line
                while (reader.readLine().also { line = it } != null) {
                    val tokens = line?.trim()?.split("\\s+".toRegex()) ?: continue
                    if (tokens.size >= 4) {
                        val ip = tokens[0]
                        val flags = tokens[2]
                        val mac = tokens[3].uppercase(Locale.US)

                        // 0x0 flags or 00:00:00:00:00:00 means incomplete/invalid entry
                        if (flags != "0x0" && mac != "00:00:00:00:00:00" && mac.length == 17) {
                            result[ip] = mac
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Fallback gracefully if SELinux restricts procfs access on newer Android versions
        }

        return result
    }
}
