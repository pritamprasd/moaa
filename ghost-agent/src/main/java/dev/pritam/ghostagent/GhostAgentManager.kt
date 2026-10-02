package dev.pritam.ghostagent

import android.content.Context
import android.content.Intent
import dev.pritam.ghostagent.executor.GhostTaskExecutor
import dev.pritam.ghostagent.model.GhostExecutionState
import dev.pritam.ghostagent.model.ExecutionStatus
import dev.pritam.ghostagent.service.GhostAccessibilityService
import dev.pritam.ghostagent.service.GhostFloatingBubbleService
import dev.pritam.ghostagent.service.GhostScreenCaptureService
import dev.pritam.ghostagent.storage.GhostTaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * GhostAgentManager — the singleton facade for the Ghost Agent system.
 *
 * Responsibilities:
 * - Manage the lifecycle of GhostTaskExecutor (start/abort tasks)
 * - Manage the floating bubble overlay service
 * - Manage the MediaProjection screen capture service
 * - Provide a single StateFlow<GhostExecutionState> for the UI
 * - Check if the AccessibilityService is connected
 *
 * Usage from the host app:
 *   GhostAgentManager.init(context)
 *   GhostAgentManager.executeTask(taskId)
 */
object GhostAgentManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var executionJob: Job? = null

    @Volatile var executor: GhostTaskExecutor? = null
        internal set

    // ─── Initialization ───────────────────────────────────────────────────────

    fun init(context: Context) {
        // Pre-initialize repository singleton
        GhostTaskRepository.getInstance(context)
    }

    // ─── Task Execution ───────────────────────────────────────────────────────

    val executionState: StateFlow<GhostExecutionState>
        get() = GhostAccessibilityService.executionState

    fun isTaskRunning(): Boolean =
        executionState.value.status == ExecutionStatus.RUNNING ||
        executionState.value.status == ExecutionStatus.WAITING_USER

    /**
     * Start executing the task with [taskId].
     * If another task is running, it will be aborted first.
     */
    fun executeTask(context: Context, taskId: String) {
        abortCurrentTask()
        val repo = GhostTaskRepository.getInstance(context)
        val task = repo.getTask(taskId) ?: return

        val exec = GhostTaskExecutor(context.applicationContext, repo)
        executor = exec

        executionJob = scope.launch {
            exec.executeTask(task)
        }
    }

    /** Abort the currently running task (if any). */
    fun abortCurrentTask() {
        executor?.pendingUserDecision = GhostTaskExecutor.UserDecision.ABORT
        executionJob?.cancel()
        executionJob = null
        executor = null
    }

    // ─── Accessibility Service ────────────────────────────────────────────────

    fun isAccessibilityServiceConnected(): Boolean =
        GhostAccessibilityService.instance != null

    // ─── Floating Bubble ─────────────────────────────────────────────────────

    fun showFloatingBubble(context: Context) {
        if (!GhostFloatingBubbleService.isRunning) {
            context.startForegroundService(
                Intent(context, GhostFloatingBubbleService::class.java)
            )
        }
    }

    fun hideFloatingBubble(context: Context) {
        context.stopService(Intent(context, GhostFloatingBubbleService::class.java))
    }

    fun isBubbleVisible(): Boolean = GhostFloatingBubbleService.isRunning

    // ─── Screen Capture ───────────────────────────────────────────────────────

    fun startScreenCapture(context: Context, resultCode: Int, resultData: Intent) {
        val intent = Intent(context, GhostScreenCaptureService::class.java).apply {
            putExtra(GhostScreenCaptureService.EXTRA_RESULT_CODE, resultCode)
            putExtra(GhostScreenCaptureService.EXTRA_RESULT_DATA, resultData)
        }
        context.startForegroundService(intent)
    }

    fun stopScreenCapture(context: Context) {
        context.stopService(Intent(context, GhostScreenCaptureService::class.java))
    }

    val isScreenCaptureActive: StateFlow<Boolean>
        get() = GhostScreenCaptureService.isCapturing
}
