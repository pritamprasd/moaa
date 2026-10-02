package dev.pritam.host.shortcut

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.widget.Toast
import dev.pritam.host.MainActivity
import dev.pritam.host.config.ToolDefinition
import dev.pritam.host.logging.AppLogHub
import dev.pritam.host.logging.LogLevel

object ShortcutUtils {

    const val EXTRA_TARGET_TOOL_ID = "target_tool_id"

    fun isPinShortcutSupported(context: Context): Boolean {
        val shortcutManager = context.getSystemService(ShortcutManager::class.java)
        return shortcutManager != null && shortcutManager.isRequestPinShortcutSupported
    }

    fun pinToolToHomeScreen(context: Context, tool: ToolDefinition): Boolean {
        val shortcutManager = context.getSystemService(ShortcutManager::class.java)
        if (shortcutManager == null || !shortcutManager.isRequestPinShortcutSupported) {
            Toast.makeText(context, "Homescreen shortcuts are not supported on this launcher", Toast.LENGTH_SHORT).show()
            return false
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(EXTRA_TARGET_TOOL_ID, tool.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val iconBitmap = generateShortcutIcon(tool.name, tool.accentColorHex)
        val icon = Icon.createWithBitmap(iconBitmap)

        val shortcutInfo = ShortcutInfo.Builder(context, "tool_shortcut_${tool.id}")
            .setShortLabel(tool.name)
            .setLongLabel("${tool.name} - MotherOfAllApps")
            .setIcon(icon)
            .setIntent(launchIntent)
            .build()

        val success = shortcutManager.requestPinShortcut(shortcutInfo, null)
        if (success) {
            AppLogHub.log(
                toolId = tool.id,
                toolName = tool.name,
                level = LogLevel.INFO,
                tag = "Shortcut",
                message = "HOMESCREEN SHORTCUT [PIN] Added '${tool.name}' direct launcher shortcut to homescreen"
            )
            Toast.makeText(context, "Added '${tool.name}' to Home Screen!", Toast.LENGTH_SHORT).show()
        }
        return success
    }

    private fun generateShortcutIcon(toolName: String, accentColorHex: Long): Bitmap {
        val size = 192
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Dark cyberpunk background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF0B0F19.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(0f, 0f, size.toFloat(), size.toFloat(), 48f, 48f, bgPaint)

        // Outer Glowing Border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorHex.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 10f
        }
        canvas.drawRoundRect(8f, 8f, size - 8f, size - 8f, 40f, 40f, borderPaint)

        // Inner glowing badge
        val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = (accentColorHex and 0x00FFFFFF or 0x33000000).toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(24f, 24f, size - 24f, size - 24f, 28f, 28f, badgeBgPaint)

        // Tool Monogram Text
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColorHex.toInt()
            textSize = 58f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val monogram = when {
            toolName.contains("Server", ignoreCase = true) -> "FTP"
            toolName.contains("Client", ignoreCase = true) -> "CLI"
            toolName.contains("NFC", ignoreCase = true) -> "NFC"
            toolName.contains("Logs", ignoreCase = true) -> "LOG"
            else -> toolName.take(3).uppercase()
        }

        val yPos = (canvas.height / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(monogram, size / 2f, yPos, textPaint)

        return bitmap
    }
}
