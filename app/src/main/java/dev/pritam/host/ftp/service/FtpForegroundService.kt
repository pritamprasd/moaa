package dev.pritam.host.ftp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import dev.pritam.host.MainActivity
import dev.pritam.host.R
import dev.pritam.host.ftp.model.FtpConfig
import dev.pritam.host.ftp.model.FtpServerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Android Foreground Service enabling the FTP Server to run continuously in the background.
 */
class FtpForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireLocks()
        observeServerState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SERVER -> {
                FtpServerController.stopServer()
                stopForegroundService()
                return START_NOT_STICKY
            }
            ACTION_START_SERVER -> {
                val port = intent.getIntExtra(EXTRA_PORT, FtpConfig.DEFAULT_PORT)
                val username = intent.getStringExtra(EXTRA_USERNAME) ?: FtpConfig.DEFAULT_USERNAME
                val password = intent.getStringExtra(EXTRA_PASSWORD) ?: FtpConfig.DEFAULT_PASSWORD
                val rootPath = intent.getStringExtra(EXTRA_ROOT_PATH) ?: FtpConfig.DEFAULT_ROOT_PATH

                val config = FtpConfig(
                    port = port,
                    username = username,
                    password = password,
                    rootPath = rootPath,
                )

                startForeground(NOTIFICATION_ID, buildNotification("Starting FTP Server...", "Initializing network listener"))
                FtpServerController.startServer(config)
            }
        }
        return START_STICKY
    }

    private fun observeServerState() {
        serviceScope.launch {
            FtpServerController.serverState.collectLatest { state ->
                when (state) {
                    is FtpServerState.Running -> {
                        val notification = buildNotification(
                            title = "FTP Server Running",
                            content = "${state.connectionUrl} (User: ${state.username})"
                        )
                        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        manager.notify(NOTIFICATION_ID, notification)
                    }
                    is FtpServerState.Stopped -> {
                        stopForegroundService()
                    }
                    is FtpServerState.Error -> {
                        val notification = buildNotification(
                            title = "FTP Server Error",
                            content = state.message
                        )
                        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        manager.notify(NOTIFICATION_ID, notification)
                    }
                    FtpServerState.Starting -> {
                        // Handled in onStartCommand
                    }
                }
            }
        }
    }

    private fun buildNotification(title: String, content: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, FtpForegroundService::class.java).apply {
            action = ACTION_STOP_SERVER
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        return builder
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .addAction(
                Notification.Action.Builder(
                    null,
                    "Stop Server",
                    stopPendingIntent
                ).build()
            )
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "LAN FTP Server Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live FTP background server status and controls"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun acquireLocks() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MotherOfAllApps:FtpWakeLock")?.apply {
                acquire(10 * 60 * 60 * 1000L) // 10 hours safety max
            }

            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            @Suppress("DEPRECATION")
            wifiLock = wifiManager?.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "MotherOfAllApps:FtpWifiLock")?.apply {
                acquire()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseLocks() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (ignored: Exception) {}
        try {
            if (wifiLock?.isHeld == true) wifiLock?.release()
        } catch (ignored: Exception) {}
    }

    private fun stopForegroundService() {
        releaseLocks()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        releaseLocks()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "ftp_server_channel"
        const val NOTIFICATION_ID = 4242

        const val ACTION_START_SERVER = "dev.pritam.host.ftp.START_SERVER"
        const val ACTION_STOP_SERVER = "dev.pritam.host.ftp.STOP_SERVER"

        const val EXTRA_PORT = "extra_port"
        const val EXTRA_USERNAME = "extra_username"
        const val EXTRA_PASSWORD = "extra_password"
        const val EXTRA_ROOT_PATH = "extra_root_path"

        fun start(context: Context, config: FtpConfig) {
            val intent = Intent(context, FtpForegroundService::class.java).apply {
                action = ACTION_START_SERVER
                putExtra(EXTRA_PORT, config.port)
                putExtra(EXTRA_USERNAME, config.username)
                putExtra(EXTRA_PASSWORD, config.password)
                putExtra(EXTRA_ROOT_PATH, config.rootPath)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FtpForegroundService::class.java).apply {
                action = ACTION_STOP_SERVER
            }
            context.startService(intent)
        }
    }
}
