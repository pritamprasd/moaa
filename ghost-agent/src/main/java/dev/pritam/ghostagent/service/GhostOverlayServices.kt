package dev.pritam.ghostagent.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import dev.pritam.ghostagent.GhostAgentManager

/**
 * Quick Settings Tile for Ghost Agent.
 *
 * Appears in the notification shade pull-down as a tile. When the tile is tapped:
 * - Shows the Ghost Agent task selector (launches the GhostAgentStudioActivity)
 * - Updates the tile state to reflect whether the Ghost Agent is active
 *
 * Registered in AndroidManifest with:
 *   <service android:name=".service.GhostQuickTileService"
 *            android:permission="android.permission.BIND_QUICK_SETTINGS_TILE"
 *            android:icon="@drawable/ic_ghost_tile"
 *            android:label="Ghost Agent">
 *     <intent-filter>
 *       <action android:name="android.service.quicksettings.action.QS_TILE"/>
 *     </intent-filter>
 *   </service>
 */
class GhostQuickTileService : TileService() {

    override fun onStartListening() {
        updateTile()
    }

    override fun onClick() {
        // Collapse the quick settings panel and open the Ghost Agent UI
        val isRunning = GhostAgentManager.isTaskRunning()
        if (isRunning) {
            // If a task is running, abort it
            GhostAgentManager.abortCurrentTask()
        } else {
            // Launch the Ghost Agent Studio activity
            val intent = Intent().apply {
                setClassName(packageName, "dev.pritam.host.MainActivity")
                putExtra("deep_link_tool", "ghost-agent")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivityAndCollapse(intent)
        }
        updateTile()
    }

    override fun onStopListening() {
        // Tile no longer visible
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val isRunning = GhostAgentManager.isTaskRunning()
        tile.state = if (isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = if (isRunning) "Ghost Active" else "Ghost Agent"
        tile.contentDescription = if (isRunning) "Ghost Agent is running — tap to abort" else "Tap to launch Ghost Agent"
        tile.updateTile()
    }
}

/**
 * Floating overlay bubble service.
 *
 * Shows a draggable translucent "👻" bubble permanently on screen.
 * Tapping it opens the Ghost Agent task selector.
 * Long-pressing it shows a dismiss target.
 *
 * Requires SYSTEM_ALERT_WINDOW permission.
 *
 * Started via GhostAgentManager.showFloatingBubble(context)
 * Stopped via GhostAgentManager.hideFloatingBubble(context)
 */
class GhostFloatingBubbleService : android.app.Service() {

    companion object {
        const val CHANNEL_ID = "ghost_bubble"
        const val NOTIF_ID = 3001
        @Volatile var isRunning = false
    }

    private var windowManager: WindowManager? = null
    private var bubbleView: View? = null
    private var params: WindowManager.LayoutParams? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        startForeground(NOTIF_ID, buildForegroundNotification())
        showBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        removeBubble()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun showBubble() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // Programmatic bubble view — a round glass pill with ghost emoji
        val bubble = android.widget.TextView(this).apply {
            text = "👻"
            textSize = 24f
            gravity = Gravity.CENTER
            setPadding(4, 4, 4, 4)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(0xCC1E293B.toInt())
                setStroke(2, 0x6638BDF8.toInt())
                cornerRadius = 999f
            }
            elevation = 8f
        }

        params = WindowManager.LayoutParams(
            80.dpToPx(), 80.dpToPx(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 20
            y = 400
        }

        // Touch listener for drag + tap
        var initialX = 0; var initialY = 0
        var initialTouchX = 0f; var initialTouchY = 0f
        var isDragging = false

        bubble.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params!!.x; initialY = params!!.y
                    initialTouchX = event.rawX; initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (kotlin.math.abs(dx) > 10 || kotlin.math.abs(dy) > 10) {
                        isDragging = true
                        params!!.x = initialX + dx
                        params!!.y = initialY + dy
                        windowManager?.updateViewLayout(bubble, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // Tap — open Ghost Agent
                        val intent = Intent().apply {
                            setClassName(packageName, "dev.pritam.host.MainActivity")
                            putExtra("deep_link_tool", "ghost-agent")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        }
                        startActivity(intent)
                    }
                    true
                }
                else -> false
            }
        }

        bubbleView = bubble
        windowManager?.addView(bubble, params)
    }

    private fun removeBubble() {
        try {
            bubbleView?.let { windowManager?.removeView(it) }
        } catch (_: Exception) {}
        bubbleView = null
        windowManager = null
    }

    private fun buildForegroundNotification(): Notification {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Ghost Agent Bubble", NotificationManager.IMPORTANCE_MIN)
                    .apply { setShowBadge(false) }
            )
        }
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("👻 Ghost Agent")
            .setContentText("Floating bubble active. Tap to open Ghost Agent.")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}
