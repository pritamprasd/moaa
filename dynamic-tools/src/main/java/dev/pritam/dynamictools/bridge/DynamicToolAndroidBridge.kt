package dev.pritam.dynamictools.bridge

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.webkit.JavascriptInterface
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * JavaScript Interface bridge injected into WebView as `window.AndroidBridge`.
 * Provides safe native Android capabilities for dynamic tools.
 */
class DynamicToolAndroidBridge(
    private val context: Context,
    private val toolName: String,
    private val onLogReceived: ((String) -> Unit)? = null
) {
    private val mainScope = CoroutineScope(Dispatchers.Main)

    @JavascriptInterface
    fun log(message: String) {
        onLogReceived?.invoke("[$toolName] $message")
    }

    @JavascriptInterface
    fun copyToClipboard(text: String) {
        mainScope.launch {
            try {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(toolName, text)
                clipboard.setPrimaryClip(clip)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @JavascriptInterface
    fun showToast(message: String) {
        mainScope.launch {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    @JavascriptInterface
    fun vibrate(durationMs: Long) {
        try {
            val clampedMs = durationMs.coerceIn(10L, 2000L)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.vibrate(
                    CombinedVibration.createParallel(
                        VibrationEffect.createOneShot(clampedMs, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(clampedMs)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JavascriptInterface
    fun getDeviceInfo(): String {
        return try {
            JSONObject().apply {
                put("model", Build.MODEL)
                put("manufacturer", Build.MANUFACTURER)
                put("android_version", Build.VERSION.RELEASE)
                put("sdk_int", Build.VERSION.SDK_INT)
                put("timestamp", System.currentTimeMillis())
            }.toString()
        } catch (e: Exception) {
            "{}"
        }
    }
}
