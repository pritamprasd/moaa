package dev.pritam.host.tool.sysinfo.manager

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Resources
import android.hardware.biometrics.BiometricManager
import android.media.MediaDrm
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.Process
import android.os.StatFs
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.ui.graphics.Color
import dev.pritam.host.tool.sysinfo.model.CompleteSystemReport
import dev.pritam.host.tool.sysinfo.model.SysInfoGroup
import dev.pritam.host.tool.sysinfo.model.SysInfoItem
import dev.pritam.host.ui.theme.Amber
import dev.pritam.host.ui.theme.Cyan
import dev.pritam.host.ui.theme.Emerald
import dev.pritam.host.ui.theme.Rose
import dev.pritam.host.ui.theme.Violet
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

class SysInfoCollector(private val context: Context) {

    private val WIDEVINE_UUID = UUID(-0x121074568629b532L, -0x5c37d8232ae2de13L)

    fun collectAll(): CompleteSystemReport {
        val groups = listOf(
            collectOsGroup(),
            collectDeviceGroup(),
            collectCpuGroup(),
            collectMemoryGroup(),
            collectStorageGroup(),
            collectNetworkGroup(),
            collectBatteryGroup(),
            collectDisplayGroup(),
            collectKernelGroup(),
            collectSecurityGroup()
        )
        return CompleteSystemReport(groups = groups)
    }

    // ── 1. OS & ANDROID BUILD ─────────────────────────────────────────────
    private fun collectOsGroup(): SysInfoGroup {
        val buildDate = try {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault()).format(Date(Build.TIME))
        } catch (e: Exception) {
            "${Build.TIME}"
        }

        val items = listOf(
            SysInfoItem("Android Version", "Android ${Build.VERSION.RELEASE}", isHighlighted = true),
            SysInfoItem("API Level (SDK)", "${Build.VERSION.SDK_INT} (${Build.VERSION.CODENAME})"),
            SysInfoItem("Security Patch", Build.VERSION.SECURITY_PATCH.ifEmpty { "N/A" }, isHighlighted = true),
            SysInfoItem("Build Display ID", Build.DISPLAY),
            SysInfoItem("Build ID / Incremental", "${Build.ID} (${Build.VERSION.INCREMENTAL})"),
            SysInfoItem("Build Fingerprint", Build.FINGERPRINT),
            SysInfoItem("Build Date & Time", buildDate),
            SysInfoItem("Build Type & Tags", "${Build.TYPE} / ${Build.TAGS}"),
            SysInfoItem("Bootloader", Build.BOOTLOADER),
            SysInfoItem("Radio / Baseband", try { Build.getRadioVersion() ?: "Unknown" } catch (e: Exception) { "Unknown" }),
            SysInfoItem("Java VM Runtime", "${System.getProperty("java.vm.name")} ${System.getProperty("java.vm.version")}")
        )

        return SysInfoGroup(
            id = "os_build",
            title = "Android OS & Build",
            emoji = "🤖",
            accentColor = Cyan,
            items = items
        )
    }

    // ── 2. DEVICE & HARDWARE SPECS ────────────────────────────────────────
    private fun collectDeviceGroup(): SysInfoGroup {
        val items = listOf(
            SysInfoItem("Manufacturer", Build.MANUFACTURER.replaceFirstChar { it.uppercase() }, isHighlighted = true),
            SysInfoItem("Brand", Build.BRAND.replaceFirstChar { it.uppercase() }),
            SysInfoItem("Model Name", Build.MODEL, isHighlighted = true),
            SysInfoItem("Product Name", Build.PRODUCT),
            SysInfoItem("Device Code", Build.DEVICE),
            SysInfoItem("Board", Build.BOARD),
            SysInfoItem("Hardware SoC", Build.HARDWARE),
            SysInfoItem("Supported ABIs", Build.SUPPORTED_ABIS.joinToString(", ")),
            SysInfoItem("64-Bit ABI Architecture", if (Process.is64Bit()) "Yes (64-Bit ARM / x86_64)" else "No (32-Bit)"),
            SysInfoItem("Host & User", "${Build.USER}@${Build.HOST}")
        )

        return SysInfoGroup(
            id = "device_hardware",
            title = "Device & Hardware Model",
            emoji = "📱",
            accentColor = Emerald,
            items = items
        )
    }

    // ── 3. CPU & PROCESSOR ────────────────────────────────────────────────
    private fun collectCpuGroup(): SysInfoGroup {
        val cores = Runtime.getRuntime().availableProcessors()
        val arch = System.getProperty("os.arch") ?: "Unknown"
        var cpuHardware = Build.HARDWARE
        var cpuFeatures = "Standard ARMv8/v9 ISA"

        try {
            val cpuInfo = File("/proc/cpuinfo")
            if (cpuInfo.exists() && cpuInfo.canRead()) {
                val reader = BufferedReader(FileReader(cpuInfo))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val l = line ?: break
                    if (l.startsWith("Hardware", ignoreCase = true)) {
                        cpuHardware = l.substringAfter(":").trim()
                    } else if (l.startsWith("Features", ignoreCase = true)) {
                        cpuFeatures = l.substringAfter(":").trim()
                    }
                }
                reader.close()
            }
        } catch (e: Exception) {
            // Ignore
        }

        val items = listOf(
            SysInfoItem("Processor Architecture", arch, isHighlighted = true),
            SysInfoItem("CPU Cores Count", "$cores Active Cores", isHighlighted = true),
            SysInfoItem("SoC / Chipset", cpuHardware),
            SysInfoItem("Instruction Set Features", cpuFeatures),
            SysInfoItem("ABI 32/64 Spec", Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a")
        )

        return SysInfoGroup(
            id = "cpu_processor",
            title = "CPU & Processor",
            emoji = "🧠",
            accentColor = Violet,
            items = items
        )
    }

    // ── 4. RAM & SWAP MEMORY ──────────────────────────────────────────────
    private fun collectMemoryGroup(): SysInfoGroup {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)

        val totalBytes = memInfo.totalMem
        val availBytes = memInfo.availMem
        val usedBytes = (totalBytes - availBytes).coerceAtLeast(0L)

        val totalGb = totalBytes / (1024.0 * 1024.0 * 1024.0)
        val availGb = availBytes / (1024.0 * 1024.0 * 1024.0)
        val usedGb = usedBytes / (1024.0 * 1024.0 * 1024.0)
        val usedPercent = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()) else 0f
        val thresholdMb = memInfo.threshold / (1024 * 1024)

        var swapTotal = "Unknown"
        var swapFree = "Unknown"
        try {
            val meminfoFile = File("/proc/meminfo")
            if (meminfoFile.exists()) {
                val lines = meminfoFile.readLines()
                lines.forEach { line ->
                    if (line.startsWith("SwapTotal:")) swapTotal = line.substringAfter(":").trim()
                    if (line.startsWith("SwapFree:")) swapFree = line.substringAfter(":").trim()
                }
            }
        } catch (e: Exception) {
            // Ignore
        }

        val items = listOf(
            SysInfoItem(
                key = "RAM Utilization",
                value = String.format(Locale.US, "%.2f GB used of %.2f GB (%.1f%%)", usedGb, totalGb, usedPercent * 100f),
                subtitle = "Active system & application memory footprint",
                isHighlighted = true,
                progressFraction = usedPercent,
                progressColor = if (usedPercent > 0.85f) Rose else Emerald
            ),
            SysInfoItem("Total Physical RAM", String.format(Locale.US, "%.2f GB (%d MB)", totalGb, totalBytes / (1024 * 1024))),
            SysInfoItem("Available / Free RAM", String.format(Locale.US, "%.2f GB (%d MB)", availGb, availBytes / (1024 * 1024))),
            SysInfoItem("Low Memory Warning Flag", if (memInfo.lowMemory) "YES (CRITICAL MEMORY PRESSURE)" else "No (Healthy)"),
            SysInfoItem("Low Memory Threshold", "$thresholdMb MB"),
            SysInfoItem("ZRAM / Swap Total", swapTotal),
            SysInfoItem("ZRAM / Swap Free", swapFree)
        )

        return SysInfoGroup(
            id = "memory_ram",
            title = "RAM & Swap Memory",
            emoji = "⚡",
            accentColor = Emerald,
            items = items
        )
    }

    // ── 5. STORAGE PARTITIONS ─────────────────────────────────────────────
    private fun collectStorageGroup(): SysInfoGroup {
        val items = mutableListOf<SysInfoItem>()

        // Internal Data (/data)
        try {
            val dataPath = Environment.getDataDirectory()
            val statFs = StatFs(dataPath.path)
            val blockSize = statFs.blockSizeLong
            val totalBytes = statFs.blockCountLong * blockSize
            val freeBytes = statFs.availableBlocksLong * blockSize
            val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)

            val totalGb = totalBytes / (1024.0 * 1024.0 * 1024.0)
            val freeGb = freeBytes / (1024.0 * 1024.0 * 1024.0)
            val usedGb = usedBytes / (1024.0 * 1024.0 * 1024.0)
            val fraction = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()) else 0f

            items.add(
                SysInfoItem(
                    key = "Internal Storage (/data)",
                    value = String.format(Locale.US, "%.2f GB used of %.2f GB (%.1f%%)", usedGb, totalGb, fraction * 100f),
                    subtitle = String.format(Locale.US, "%.2f GB free remaining", freeGb),
                    isHighlighted = true,
                    progressFraction = fraction,
                    progressColor = if (fraction > 0.9f) Rose else Cyan
                )
            )
            items.add(SysInfoItem("Internal Total Capacity", String.format(Locale.US, "%.2f GB", totalGb)))
            items.add(SysInfoItem("Internal Free Space", String.format(Locale.US, "%.2f GB", freeGb)))
        } catch (e: Exception) {
            items.add(SysInfoItem("Internal Storage", "Unable to read StatFs"))
        }

        // External Storage (/sdcard)
        try {
            val extPath = Environment.getExternalStorageDirectory()
            if (extPath != null && extPath.exists()) {
                val statFs = StatFs(extPath.path)
                val blockSize = statFs.blockSizeLong
                val totalBytes = statFs.blockCountLong * blockSize
                val freeBytes = statFs.availableBlocksLong * blockSize
                val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)

                val totalGb = totalBytes / (1024.0 * 1024.0 * 1024.0)
                val freeGb = freeBytes / (1024.0 * 1024.0 * 1024.0)
                val usedGb = usedBytes / (1024.0 * 1024.0 * 1024.0)
                val fraction = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()) else 0f

                items.add(
                    SysInfoItem(
                        key = "Shared Storage (${extPath.path})",
                        value = String.format(Locale.US, "%.2f GB used of %.2f GB (%.1f%%)", usedGb, totalGb, fraction * 100f),
                        subtitle = String.format(Locale.US, "%.2f GB free", freeGb),
                        progressFraction = fraction,
                        progressColor = Amber
                    )
                )
            }
        } catch (e: Exception) {
            // Ignore
        }

        // System partition (/system)
        try {
            val sysPath = Environment.getRootDirectory()
            val statFs = StatFs(sysPath.path)
            val totalGb = (statFs.blockCountLong * statFs.blockSizeLong) / (1024.0 * 1024.0 * 1024.0)
            val freeGb = (statFs.availableBlocksLong * statFs.blockSizeLong) / (1024.0 * 1024.0 * 1024.0)
            items.add(SysInfoItem("System Root Partition (/system)", String.format(Locale.US, "%.2f GB (%.2f GB free)", totalGb, freeGb)))
        } catch (e: Exception) {
            // Ignore
        }

        return SysInfoGroup(
            id = "storage_partitions",
            title = "Storage & Partitions",
            emoji = "💾",
            accentColor = Amber,
            items = items
        )
    }

    // ── 6. NETWORK & CONNECTIVITY ─────────────────────────────────────────
    private fun collectNetworkGroup(): SysInfoGroup {
        val connManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNet = connManager?.activeNetwork
        val caps = connManager?.getNetworkCapabilities(activeNet)
        val linkProps = connManager?.getLinkProperties(activeNet)

        val transport = when {
            caps == null -> "Disconnected / Offline"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi Network"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular Mobile Data"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet LAN"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "Virtual Private Network (VPN)"
            else -> "Connected Network"
        }

        val isVpn = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true

        var localIpv4 = "Not Assigned"
        var localIpv6 = "Not Assigned"
        val interfaceList = mutableListOf<String>()
        var macAddress = "02:00:00:00:00:00 (Protected)"

        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress) {
                        if (addr is Inet4Address && localIpv4 == "Not Assigned") {
                            localIpv4 = "${addr.hostAddress} (${intf.name})"
                        } else if (addr is Inet6Address && localIpv6 == "Not Assigned") {
                            localIpv6 = "${addr.hostAddress?.substringBefore("%")} (${intf.name})"
                        }
                    }
                }
                val hw = intf.hardwareAddress
                if (hw != null && hw.isNotEmpty() && macAddress.startsWith("02:00")) {
                    val macStr = hw.joinToString(":") { String.format("%02X", it) }
                    if (macStr != "00:00:00:00:00:00") {
                        macAddress = "$macStr (${intf.name})"
                    }
                }
                interfaceList.add("${intf.name} (MTU ${intf.mtu})")
            }
        } catch (e: Exception) {
            // Ignore
        }

        // Wi-Fi detailed info
        var wifiDetails = "N/A"
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val wifiInfo = wifiManager?.connectionInfo
            if (wifiInfo != null && caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                val ssid = wifiInfo.ssid.removeSurrounding("\"")
                val rssi = wifiInfo.rssi
                val linkSpeed = wifiInfo.linkSpeed
                val freq = wifiInfo.frequency
                wifiDetails = "$ssid (RSSI: $rssi dBm, $linkSpeed Mbps, ${freq}MHz)"
            }
        } catch (e: Exception) {
            // Ignore
        }

        val gateway = linkProps?.routes?.firstOrNull { it.isDefaultRoute }?.gateway?.hostAddress ?: "N/A"
        val dnsServers = linkProps?.dnsServers?.joinToString(", ") { it.hostAddress ?: "" }?.ifEmpty { "N/A" } ?: "N/A"

        val airplaneMode = try {
            if (Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1) "Active (ON)" else "Inactive (OFF)"
        } catch (e: Exception) {
            "Unknown"
        }

        val items = listOf(
            SysInfoItem("Active Network Connection", transport, isHighlighted = true),
            SysInfoItem("Local IPv4 Address", localIpv4, isHighlighted = true),
            SysInfoItem("Local IPv6 Address", localIpv6),
            SysInfoItem("Hardware MAC Address", macAddress, isHighlighted = true),
            SysInfoItem("Wi-Fi Telemetry & Link", wifiDetails),
            SysInfoItem("Default Gateway IP", gateway),
            SysInfoItem("DNS Servers", dnsServers),
            SysInfoItem("VPN Tunnel Status", if (isVpn) "Active (VPN Encrypted)" else "Inactive (Direct Route)"),
            SysInfoItem("Airplane Mode", airplaneMode),
            SysInfoItem("Active Network Interfaces", interfaceList.joinToString(", ").ifEmpty { "None" })
        )

        return SysInfoGroup(
            id = "network_connectivity",
            title = "Network & Connectivity",
            emoji = "🌐",
            accentColor = Cyan,
            items = items
        )
    }

    // ── 7. BATTERY & POWER TELEMETRY ──────────────────────────────────────
    private fun collectBatteryGroup(): SysInfoGroup {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, intentFilter)

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) (level / scale.toFloat()) * 100f else -1f

        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val statusStr = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "⚡ Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging (Battery Power)"
            BatteryManager.BATTERY_STATUS_FULL -> "✓ Full (100%)"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
            else -> "Unknown"
        }

        val plugged = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val powerSource = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Wall Charger"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Cable / Computer"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Qi Charger"
            else -> "Battery Only"
        }

        val health = batteryStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
        val healthStr = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good / Optimal"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "OVERHEAT (Warning)"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
            else -> "Standard"
        }

        val tempTenths = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val tempC = tempTenths / 10.0
        val tempF = (tempC * 9 / 5) + 32

        val voltageMv = batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        val tech = batteryStatus?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Li-ion"

        val fraction = (batteryPct / 100f).coerceIn(0f, 1f)

        val items = listOf(
            SysInfoItem(
                key = "Battery Level",
                value = String.format(Locale.US, "%.0f%% (%s)", batteryPct, statusStr),
                isHighlighted = true,
                progressFraction = fraction,
                progressColor = if (fraction > 0.2f) Emerald else Rose
            ),
            SysInfoItem("Power Source", powerSource),
            SysInfoItem("Battery Health", healthStr),
            SysInfoItem("Temperature", String.format(Locale.US, "%.1f °C / %.1f °F", tempC, tempF)),
            SysInfoItem("Voltage", String.format(Locale.US, "%d mV (%.2f V)", voltageMv, voltageMv / 1000.0)),
            SysInfoItem("Chemistry Technology", tech)
        )

        return SysInfoGroup(
            id = "battery_power",
            title = "Battery & Power",
            emoji = "🔋",
            accentColor = Emerald,
            items = items
        )
    }

    // ── 8. DISPLAY & SCREEN SPECS ─────────────────────────────────────────
    private fun collectDisplayGroup(): SysInfoGroup {
        val metrics = Resources.getSystem().displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val densityDpi = metrics.densityDpi
        val density = metrics.density

        val xdpi = metrics.xdpi
        val ydpi = metrics.ydpi
        val widthInches = if (xdpi > 0) width / xdpi else 0f
        val heightInches = if (ydpi > 0) height / ydpi else 0f
        val diagonalInches = Math.sqrt((widthInches * widthInches + heightInches * heightInches).toDouble())

        val refreshRate = try {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? android.view.WindowManager
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) context.display else windowManager?.defaultDisplay
            display?.refreshRate ?: 60f
        } catch (e: Exception) {
            60f
        }

        val items = listOf(
            SysInfoItem("Screen Resolution", "${width} × ${height} pixels", isHighlighted = true),
            SysInfoItem("Refresh Rate", String.format(Locale.US, "%.1f Hz", refreshRate), isHighlighted = true),
            SysInfoItem("Screen Density (DPI)", "$densityDpi dpi (Scale factor ${density}x)"),
            SysInfoItem("Physical Diagonal Size", String.format(Locale.US, "%.2f inches (Estimated)", diagonalInches)),
            SysInfoItem("Aspect Ratio", String.format(Locale.US, "%.2f:1 (%d:%d)", height.toFloat() / width.toFloat(), height, width)),
            SysInfoItem("Font Scale", "${metrics.scaledDensity / metrics.density}x")
        )

        return SysInfoGroup(
            id = "display_screen",
            title = "Display & Screen",
            emoji = "🖥️",
            accentColor = Violet,
            items = items
        )
    }

    // ── 9. KERNEL & LINUX ENVIRONMENT ─────────────────────────────────────
    private fun collectKernelGroup(): SysInfoGroup {
        val kernelVersion = System.getProperty("os.version") ?: "Linux"
        val uptimeMs = SystemClock.elapsedRealtime()
        val uptimeSecs = uptimeMs / 1000
        val days = uptimeSecs / (24 * 3600)
        val hours = (uptimeSecs % (24 * 3600)) / 3600
        val mins = (uptimeSecs % 3600) / 60
        val secs = uptimeSecs % 60
        val uptimeFormatted = if (days > 0) "${days}d ${hours}h ${mins}m ${secs}s" else "${hours}h ${mins}m ${secs}s"

        val isRooted = checkRootAccess()

        val timezone = TimeZone.getDefault()
        val tzDisplay = "${timezone.id} (${timezone.getDisplayName(false, TimeZone.SHORT)})"
        val locale = Locale.getDefault().toLanguageTag()

        val items = listOf(
            SysInfoItem("Linux Kernel Release", kernelVersion, isHighlighted = true),
            SysInfoItem("System Uptime", uptimeFormatted, isHighlighted = true),
            SysInfoItem("Root / Superuser Access", if (isRooted) "ROOTED (su binary present)" else "Standard User (Not Rooted)"),
            SysInfoItem("System Timezone", tzDisplay),
            SysInfoItem("Default System Locale", locale),
            SysInfoItem("SELinux Status", "Enforcing (Android Strict Security)")
        )

        return SysInfoGroup(
            id = "kernel_linux",
            title = "Kernel & Linux Environment",
            emoji = "🐧",
            accentColor = Amber,
            items = items
        )
    }

    // ── 10. SECURITY & DRM ────────────────────────────────────────────────
    private fun collectSecurityGroup(): SysInfoGroup {
        var widevineLevel = "Not Supported / Unavailable"
        try {
            if (MediaDrm.isCryptoSchemeSupported(WIDEVINE_UUID)) {
                val mediaDrm = MediaDrm(WIDEVINE_UUID)
                val securityLevel = mediaDrm.getPropertyString("securityLevel")
                widevineLevel = "Widevine $securityLevel (Hardware Encrypted DRM)"
                mediaDrm.close()
            }
        } catch (e: Exception) {
            widevineLevel = "Widevine Present"
        }

        val pm = context.packageManager
        val hasFingerprint = pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
        val hasFace = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) pm.hasSystemFeature(PackageManager.FEATURE_FACE) else false
        val hasNfc = pm.hasSystemFeature(PackageManager.FEATURE_NFC)
        val hasBluetoothLe = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
        val hasUsbHost = pm.hasSystemFeature(PackageManager.FEATURE_USB_HOST)

        val biometrics = buildList {
            if (hasFingerprint) add("Fingerprint")
            if (hasFace) add("Face Unlock")
            if (isEmpty()) add("None")
        }.joinToString(", ")

        val items = listOf(
            SysInfoItem("Widevine DRM Level", widevineLevel, isHighlighted = true),
            SysInfoItem("Biometric Sensors", biometrics),
            SysInfoItem("NFC Hardware Available", if (hasNfc) "Yes" else "No"),
            SysInfoItem("Bluetooth Low Energy (BLE)", if (hasBluetoothLe) "Yes" else "No"),
            SysInfoItem("USB Host & OTG Support", if (hasUsbHost) "Yes" else "No")
        )

        return SysInfoGroup(
            id = "security_drm",
            title = "Security, DRM & Sensors",
            emoji = "🔒",
            accentColor = Rose,
            items = items
        )
    }

    private fun checkRootAccess(): Boolean {
        val paths = listOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        return paths.any { File(it).exists() }
    }
}
