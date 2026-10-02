package dev.pritam.ghostagent.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.io.File

/**
 * Helper for all Ghost Agent notifications:
 * 1. Step failure notification (with screenshot + action buttons)
 * 2. Task complete notification
 */
object GhostNotificationHelper {

    const val CHANNEL_ID = "ghost_agent_alerts"
    const val NOTIF_FAILURE_ID = 3010
    const val NOTIF_COMPLETE_ID = 3011

    const val ACTION_SKIP   = "ghost.action.SKIP_STEP"
    const val ACTION_RETRY  = "ghost.action.RETRY_STEP"
    const val ACTION_ABORT  = "ghost.action.ABORT_TASK"

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Ghost Agent Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for Ghost Agent task automation events."
            setShowBadge(true)
        }
        nm.createNotificationChannel(channel)
    }

    fun postStepFailureNotification(
        context: Context,
        taskName: String,
        stepLabel: String,
        screenshot: File?,
        stepIndex: Int,
        totalSteps: Int,
    ) {
        ensureChannel(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        fun pi(action: String): PendingIntent = PendingIntent.getBroadcast(
            context, action.hashCode(),
            Intent(action).setPackage(context.packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val skipAction = Notification.Action.Builder(
            android.graphics.drawable.Icon.createWithResource(context, android.R.drawable.ic_media_next),
            "Skip", pi(ACTION_SKIP)
        ).build()

        val retryAction = Notification.Action.Builder(
            android.graphics.drawable.Icon.createWithResource(context, android.R.drawable.ic_media_rew),
            "Retry", pi(ACTION_RETRY)
        ).build()

        val abortAction = Notification.Action.Builder(
            android.graphics.drawable.Icon.createWithResource(context, android.R.drawable.ic_menu_close_clear_cancel),
            "Abort", pi(ACTION_ABORT)
        ).build()

        val builder = Notification.Builder(context, CHANNEL_ID)
            .setContentTitle("⚠️ Ghost Agent: Step Failed")
            .setContentText("\"$stepLabel\" (Step ${stepIndex + 1}/$totalSteps) in \"$taskName\"")
            .setStyle(Notification.BigTextStyle()
                .bigText("Task \"$taskName\" is stuck at step ${stepIndex + 1}/$totalSteps:\n\"$stepLabel\"\n\nChoose an action to continue."))
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setOngoing(true)
            .addAction(skipAction)
            .addAction(retryAction)
            .addAction(abortAction)
            .setAutoCancel(false)

        // Attach screenshot if available
        if (screenshot != null && screenshot.exists()) {
            try {
                val bmp = android.graphics.BitmapFactory.decodeFile(screenshot.absolutePath)
                if (bmp != null) {
                    builder.setStyle(
                        Notification.BigPictureStyle()
                            .bigPicture(bmp)
                            .bigLargeIcon(null as android.graphics.Bitmap?)
                            .setSummaryText("\"$stepLabel\" failed — choose an action:")
                    )
                }
            } catch (_: Exception) { /* fallback to text style */ }
        }

        nm.notify(NOTIF_FAILURE_ID, builder.build())
    }

    fun cancelStepFailureNotification(context: Context) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(NOTIF_FAILURE_ID)
    }

    fun postTaskCompleteNotification(context: Context, taskName: String) {
        ensureChannel(context)
        val notif = Notification.Builder(context, CHANNEL_ID)
            .setContentTitle("✅ Ghost Agent: Task Complete")
            .setContentText("\"$taskName\" finished successfully.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .build()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIF_COMPLETE_ID, notif)
    }
}

/**
 * BroadcastReceiver that handles user decision actions from step-failure notifications.
 * Forwards decisions to the GhostAgentManager singleton executor.
 */
class GhostNotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val executor = dev.pritam.ghostagent.GhostAgentManager.executor ?: return
        executor.pendingUserDecision = when (intent.action) {
            GhostNotificationHelper.ACTION_SKIP  -> dev.pritam.ghostagent.executor.GhostTaskExecutor.UserDecision.SKIP
            GhostNotificationHelper.ACTION_RETRY -> dev.pritam.ghostagent.executor.GhostTaskExecutor.UserDecision.RETRY
            GhostNotificationHelper.ACTION_ABORT -> dev.pritam.ghostagent.executor.GhostTaskExecutor.UserDecision.ABORT
            else -> null
        }
    }
}
