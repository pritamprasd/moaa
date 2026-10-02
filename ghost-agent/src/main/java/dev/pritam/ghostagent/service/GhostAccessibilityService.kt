package dev.pritam.ghostagent.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import dev.pritam.ghostagent.model.GhostExecutionState
import dev.pritam.ghostagent.model.ExecutionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Core AccessibilityService for the Ghost Agent.
 *
 * This service:
 * 1. Reads the full UI hierarchy of the foreground app via AccessibilityNodeInfo
 * 2. Dispatches gestures (taps, swipes, long press) via dispatchGesture()
 * 3. Types text via performAction(ACTION_SET_TEXT)
 * 4. Exposes the current UI tree as a string for LLM reasoning (future Gemini Vision hook)
 *
 * Permissions required:
 *   <service android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
 *
 * The service is declared in the app's AndroidManifest and configured via
 * res/xml/ghost_agent_accessibility_config.xml.
 *
 * ⚡ Extension Point: To add Gemini Vision reasoning in the future, inject the
 *    current screenshot bytes + UI tree string into GhostTaskExecutor.handleNlStep().
 */
class GhostAccessibilityService : AccessibilityService() {

    companion object {
        // Singleton reference so GhostTaskExecutor can reach service methods
        @Volatile var instance: GhostAccessibilityService? = null
            private set

        private val _executionState = MutableStateFlow(GhostExecutionState())
        val executionState: StateFlow<GhostExecutionState> = _executionState.asStateFlow()

        fun updateExecutionState(state: GhostExecutionState) {
            _executionState.value = state
        }

        // ─── UI Tree Snapshot ────────────────────────────────────────────────
        /** Returns a compact text representation of the on-screen UI hierarchy.
         *  Used as context for LLM-based step reasoning (NL mode). */
        fun captureUiTree(): String {
            val svc = instance ?: return "Service not connected"
            val root = svc.rootInActiveWindow ?: return "No active window"
            return buildString {
                appendNode(root, 0, this)
                root.recycle()
            }.take(8000) // LLM context limit
        }

        private fun appendNode(node: AccessibilityNodeInfo?, depth: Int, sb: StringBuilder) {
            if (node == null) return
            val indent = "  ".repeat(depth)
            val cls = node.className?.toString()?.substringAfterLast('.') ?: "?"
            val text = node.text?.toString()?.take(60) ?: ""
            val contentDesc = node.contentDescription?.toString()?.take(60) ?: ""
            val viewId = node.viewIdResourceName?.substringAfterLast('/') ?: ""
            val clickable = if (node.isClickable) "★" else ""
            val editable = if (node.isEditable) "✏" else ""
            val bounds = android.graphics.Rect().also { node.getBoundsInScreen(it) }
            sb.appendLine("$indent[$cls] $clickable$editable text=\"$text\" desc=\"$contentDesc\" id=\"$viewId\" bounds=[${bounds.left},${bounds.top},${bounds.right},${bounds.bottom}]")
            for (i in 0 until node.childCount) {
                appendNode(node.getChild(i), depth + 1, sb)
            }
        }
    }

    override fun onServiceConnected() {
        instance = this
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Events are handled on-demand via captureUiTree() — no continuous processing needed
    }

    override fun onInterrupt() {
        // Required override
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Gesture Dispatch API (called by GhostTaskExecutor)
    // ─────────────────────────────────────────────────────────────────────────

    /** Dispatch a precise tap at screen coordinates (x, y). */
    fun tap(x: Float, y: Float, callback: AccessibilityService.GestureResultCallback? = null) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, 100L)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, callback, null)
    }

    /** Dispatch a swipe from (x1,y1) to (x2,y2) over [durationMs]. */
    fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Long = 300L,
               callback: AccessibilityService.GestureResultCallback? = null) {
        val path = Path().apply { moveTo(x1, y1); lineTo(x2, y2) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, callback, null)
    }

    /** Dispatch a long press at screen coordinates (x, y). */
    fun longPress(x: Float, y: Float, callback: AccessibilityService.GestureResultCallback? = null) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, 1200L)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, callback, null)
    }

    /** Find a node by text content and click it. Returns true if node was found and clicked. */
    fun clickNodeWithText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByText(text)
        val node = nodes.firstOrNull { it.isClickable } ?: nodes.firstOrNull()
        return if (node != null) {
            val bounds = android.graphics.Rect()
            node.getBoundsInScreen(bounds)
            node.recycle()
            root.recycle()
            tap(bounds.centerX().toFloat(), bounds.centerY().toFloat())
            true
        } else {
            root.recycle()
            false
        }
    }

    /** Find a node by view-id suffix and click it. */
    fun clickNodeWithId(viewId: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByViewId(viewId)
        val node = nodes.firstOrNull()
        return if (node != null) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            node.recycle()
            root.recycle()
            true
        } else {
            root.recycle()
            false
        }
    }

    /** Type text into the currently focused editable field. */
    fun typeText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        return if (focused != null && focused.isEditable) {
            val args = android.os.Bundle().apply {
                putString(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            val result = focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            focused.recycle()
            root.recycle()
            result
        } else {
            root.recycle()
            false
        }
    }

    /** Press the global back button. */
    fun pressBack() = performGlobalAction(GLOBAL_ACTION_BACK)

    /** Press the home button. */
    fun pressHome() = performGlobalAction(GLOBAL_ACTION_HOME)

    /** Open the recents screen. */
    fun pressRecents() = performGlobalAction(GLOBAL_ACTION_RECENTS)

    /** Take a screenshot via AccessibilityService (Android 9+). */
    @Suppress("DEPRECATION")
    fun takeScreenshot(callback: TakeScreenshotCallback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            takeScreenshot(android.view.Display.DEFAULT_DISPLAY, mainExecutor, callback)
        }
    }
}
