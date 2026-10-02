package dev.motherofallapps.host.ftp.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import java.io.File
import java.util.Locale

object StorageUtils {

    data class StorageFolderOption(
        val name: String,
        val path: String,
        val description: String,
        val iconType: String,
    )

    data class StorageInfo(
        val rootPath: String,
        val totalBytes: Long,
        val freeBytes: Long,
        val usedBytes: Long,
    ) {
        val freeSpacePercentage: Float
            get() = if (totalBytes > 0) (freeBytes.toFloat() / totalBytes.toFloat()) else 0f

        val usedSpacePercentage: Float
            get() = 1f - freeSpacePercentage
    }

    /**
     * Checks whether the app has full file access permissions (`MANAGE_EXTERNAL_STORAGE` on API 30+).
     */
    fun hasAllFilesPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    /**
     * Creates an intent to request All Files Access from the system settings.
     */
    fun createManageStorageIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }
    }

    /**
     * Inspects total and free storage space for the given folder path.
     */
    fun getStorageInfo(path: String = Environment.getExternalStorageDirectory().absolutePath): StorageInfo {
        return try {
            val file = File(path)
            if (!file.exists()) {
                file.mkdirs()
            }
            val stat = StatFs(file.path)
            val totalBytes = stat.totalBytes
            val freeBytes = stat.availableBytes
            val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)
            StorageInfo(rootPath = path, totalBytes = totalBytes, freeBytes = freeBytes, usedBytes = usedBytes)
        } catch (e: Exception) {
            StorageInfo(
                rootPath = path,
                totalBytes = 0L,
                freeBytes = 0L,
                usedBytes = 0L,
            )
        }
    }

    /**
     * Pre-configured common folder options for quick selection.
     */
    fun getCommonFolderOptions(): List<StorageFolderOption> {
        val root = Environment.getExternalStorageDirectory()
        return listOf(
            StorageFolderOption(
                name = "All Phone Storage",
                path = root.absolutePath,
                description = "Serve all accessible files and folders (/storage/emulated/0)",
                iconType = "storage",
            ),
            StorageFolderOption(
                name = "Downloads",
                path = File(root, "Download").absolutePath,
                description = "Only files in the Downloads directory",
                iconType = "download",
            ),
            StorageFolderOption(
                name = "Documents",
                path = File(root, "Documents").absolutePath,
                description = "Work and text documents directory",
                iconType = "document",
            ),
            StorageFolderOption(
                name = "Camera & Photos (DCIM)",
                path = File(root, "DCIM").absolutePath,
                description = "Camera roll and DCIM photos/videos",
                iconType = "camera",
            ),
            StorageFolderOption(
                name = "Pictures",
                path = File(root, "Pictures").absolutePath,
                description = "Screenshots and image library",
                iconType = "image",
            ),
            StorageFolderOption(
                name = "Music & Audio",
                path = File(root, "Music").absolutePath,
                description = "Audio and music tracks",
                iconType = "music",
            ),
        )
    }

    fun formatBytes(bytes: Long): String {
        val unit = 1024.0
        if (bytes < unit) return "$bytes B"
        val exp = (Math.log(bytes.toDouble()) / Math.log(unit)).toInt()
        val pre = ("KMGTPE")[exp - 1]
        return String.format(Locale.getDefault(), "%.1f %cB", bytes / Math.pow(unit, exp.toDouble()), pre)
    }
}
