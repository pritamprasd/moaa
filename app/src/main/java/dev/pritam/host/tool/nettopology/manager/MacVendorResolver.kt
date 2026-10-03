package dev.pritam.host.tool.nettopology.manager

import dev.pritam.host.tool.nettopology.model.NetworkDeviceType
import java.util.Locale

object MacVendorResolver {

    // Common OUI prefixes (uppercase without colons/dashes) mapped to Vendor Names
    private val OUI_DATABASE: Map<String, String> = mapOf(
        // Apple
        "F01898" to "Apple",
        "BCFE76" to "Apple",
        "DC0856" to "Apple",
        "3CE072" to "Apple",
        "0017F2" to "Apple",
        "A4C361" to "Apple",
        "F0DBF8" to "Apple",
        "9801A7" to "Apple",
        "AC87A3" to "Apple",
        "701124" to "Apple",
        "186590" to "Apple",
        "ACDE48" to "Apple",
        "9C35EB" to "Apple",

        // Google / Alphabet / Nest
        "745E1C" to "Google",
        "94EB2C" to "Google",
        "A47733" to "Google",
        "001A11" to "Google",
        "3C5AB4" to "Google Nest",
        "641666" to "Google Nest",
        "D86C63" to "Google",
        "18B430" to "Google Nest",

        // Samsung
        "B0EC71" to "Samsung",
        "508569" to "Samsung",
        "E47CF9" to "Samsung",
        "8C8590" to "Samsung",
        "44F459" to "Samsung",
        "980CA5" to "Samsung",
        "64DB8B" to "Samsung",
        "0808C2" to "Samsung",

        // Espressif (ESP8266 / ESP32 IoT microcontrollers)
        "246F28" to "Espressif (ESP32/ESP8266)",
        "30AEA4" to "Espressif (ESP32)",
        "84F3EB" to "Espressif",
        "A020A6" to "Espressif",
        "DC4F22" to "Espressif",
        "485519" to "Espressif",
        "68C63A" to "Espressif",
        "AC0B8A" to "Espressif",

        // Raspberry Pi
        "B827EB" to "Raspberry Pi",
        "DCA632" to "Raspberry Pi",
        "E45F01" to "Raspberry Pi Foundation",
        "28CDC1" to "Raspberry Pi Foundation",

        // TP-Link
        "50C7BF" to "TP-Link",
        "704F57" to "TP-Link",
        "984827" to "TP-Link",
        "E848B8" to "TP-Link",
        "F4F26D" to "TP-Link",
        "002586" to "TP-Link",
        "C006C3" to "TP-Link",
        "001D0F" to "TP-Link",

        // Netgear
        "00146C" to "Netgear",
        "20E52A" to "Netgear",
        "A040A0" to "Netgear",
        "C0FFD4" to "Netgear",
        "28C68E" to "Netgear",

        // Cisco / Linksys
        "00000C" to "Cisco Systems",
        "000142" to "Cisco Systems",
        "0018BA" to "Cisco Systems",
        "00226B" to "Cisco Linksys",
        "001EE5" to "Cisco Linksys",

        // Ubiquiti Networks / UniFi
        "788A20" to "Ubiquiti UniFi",
        "802AA8" to "Ubiquiti UniFi",
        "DC9FDB" to "Ubiquiti UniFi",
        "FCECDA" to "Ubiquiti UniFi",
        "24A43C" to "Ubiquiti UniFi",

        // Asus
        "0015F2" to "Asus Router/Device",
        "04D4C4" to "Asus Router/Device",
        "10BF48" to "Asus",
        "40167E" to "Asus",

        // Xiaomi / Roborock / Aqara
        "286C07" to "Xiaomi",
        "50642B" to "Xiaomi",
        "640980" to "Xiaomi",
        "7C49EB" to "Xiaomi / Roborock",
        "04CF8C" to "Xiaomi",

        // Amazon (Echo / FireTV / Kindle)
        "40B4CD" to "Amazon (Echo/FireTV)",
        "FC65DE" to "Amazon (Echo/FireTV)",
        "6837E9" to "Amazon (Echo/FireTV)",
        "CC9E00" to "Amazon",
        "747548" to "Amazon",

        // Sony (PlayStation / Bravia TV)
        "00041F" to "Sony",
        "001315" to "Sony",
        "F8461C" to "Sony Interactive (PlayStation)",
        "00248D" to "Sony",

        // LG Electronics (Smart TVs / Appliances)
        "001C62" to "LG Electronics",
        "58A2B5" to "LG Electronics (webOS TV)",
        "A823FE" to "LG Electronics",

        // Philips / Signify (Hue Bridge / Ambilight TV)
        "001788" to "Philips Hue / Signify",
        "ECB5FA" to "Philips Hue",

        // HP / Hewlett-Packard (Printers / Laptops)
        "0018FE" to "HP Printer / Computer",
        "0025B3" to "HP Printer / Computer",
        "3CD92B" to "HP",

        // Intel
        "001B21" to "Intel",
        "001E67" to "Intel",
        "3413E8" to "Intel Wi-Fi",
        "48A472" to "Intel",

        // Microsoft
        "0050F2" to "Microsoft (Xbox/Surface)",
        "7C1E52" to "Microsoft (Xbox)",
        "94F6D6" to "Microsoft",

        // Huawei
        "001882" to "Huawei",
        "404D8E" to "Huawei",
        "707B53" to "Huawei",

        // Synology / QNAP (NAS Storage)
        "001132" to "Synology NAS",
        "00089B" to "QNAP NAS",

        // Tuya / Smart Life IoT
        "D8F15B" to "Tuya Smart IoT",
        "10D561" to "Tuya Smart IoT"
    )

    fun resolveVendor(mac: String?): String? {
        if (mac.isNullOrBlank()) return null
        val clean = mac.replace(":", "").replace("-", "").replace(".", "").uppercase(Locale.US)
        if (clean.length < 6) return null
        val prefix = clean.substring(0, 6)
        return OUI_DATABASE[prefix]
    }

    fun inferDeviceType(
        isGateway: Boolean,
        isLocalDevice: Boolean,
        hostname: String?,
        vendor: String?,
        openPorts: List<Int> = emptyList()
    ): NetworkDeviceType {
        if (isLocalDevice) return NetworkDeviceType.LOCAL_HOST
        if (isGateway) return NetworkDeviceType.GATEWAY

        val host = hostname?.lowercase(Locale.US) ?: ""
        val ven = vendor?.lowercase(Locale.US) ?: ""

        // Check ports
        if (openPorts.contains(9100) || openPorts.contains(631) || host.contains("printer") || ven.contains("hp printer") || host.contains("canon") || host.contains("epson")) {
            return NetworkDeviceType.PRINTER
        }
        if (openPorts.contains(554) || host.contains("cam") || host.contains("ipcam") || host.contains("rtsp") || host.contains("hikvision") || host.contains("dahua")) {
            return NetworkDeviceType.IOT_SMART
        }
        if (openPorts.contains(1883) || openPorts.contains(8883) || ven.contains("espressif") || ven.contains("tuya") || ven.contains("hue") || host.contains("esp_") || host.contains("tasmota") || host.contains("wled") || host.contains("shelly")) {
            return NetworkDeviceType.IOT_SMART
        }
        if (openPorts.contains(3306) || openPorts.contains(5432) || openPorts.contains(11434) || ven.contains("synology") || ven.contains("qnap") || host.contains("nas") || host.contains("server") || host.contains("proxmox") || host.contains("truenas")) {
            return NetworkDeviceType.SERVER_NAS
        }
        if (ven.contains("tp-link") || ven.contains("netgear") || ven.contains("cisco") || ven.contains("unifi") || ven.contains("asus router") || host.contains("router") || host.contains("repeater") || host.contains("ap-") || host.contains("mesh")) {
            return NetworkDeviceType.SUB_ROUTER
        }
        if (ven.contains("playstation") || ven.contains("xbox") || ven.contains("webos") || ven.contains("bravia") || ven.contains("amazon (echo/firetv)") || host.contains("tv") || host.contains("chromecast") || host.contains("firestick") || host.contains("appletv") || host.contains("roku")) {
            return NetworkDeviceType.MEDIA_TV
        }
        if (host.contains("iphone") || host.contains("android") || host.contains("galaxy") || host.contains("pixel") || host.contains("redmi") || host.contains("oneplus") || host.contains("ipad")) {
            return NetworkDeviceType.MOBILE
        }
        if (ven.contains("apple") || ven.contains("samsung") || ven.contains("google") || ven.contains("xiaomi")) {
            return NetworkDeviceType.MOBILE
        }
        if (ven.contains("intel") || ven.contains("microsoft") || host.contains("macbook") || host.contains("desktop") || host.contains("laptop") || host.contains("pc") || host.contains("thinkpad") || host.contains("ubuntu") || host.contains("linux")) {
            return NetworkDeviceType.COMPUTER
        }
        if (ven.contains("raspberry pi")) {
            return NetworkDeviceType.SERVER_NAS
        }

        return NetworkDeviceType.UNKNOWN
    }
}
