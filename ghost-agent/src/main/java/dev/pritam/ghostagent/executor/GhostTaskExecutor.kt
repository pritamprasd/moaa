package dev.pritam.ghostagent.executor

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import dev.pritam.ghostagent.model.ExecutionStatus
import dev.pritam.ghostagent.model.GhostExecutionState
import dev.pritam.ghostagent.model.GhostStep
import dev.pritam.ghostagent.model.GhostTask
import dev.pritam.ghostagent.model.StepType
import dev.pritam.ghostagent.model.TouchEvent
import dev.pritam.ghostagent.service.GhostAccessibilityService
import dev.pritam.ghostagent.service.GhostNotificationHelper
import dev.pritam.ghostagent.service.GhostScreenCaptureService
import dev.pritam.ghostagent.storage.GhostTaskRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * GhostTaskExecutor — the runtime engine that runs a GhostTask step by step.
 *
 * Execution Strategy:
 * - NATURAL_LANGUAGE: Parses the nlPrompt for known intent keywords (open, tap, type, scroll,
 *   back, home) and maps them to AccessibilityService actions.
 *   ⚡ Extension Point: Replace/augment keyword matching with a call to the LLM Gateway
 *   for intelligent reasoning. Attach captureUiTree() + screenshot as context.
 *
 * - RECORDED_MACRO: Replays the ordered list of TouchEvent records using the
 *   AccessibilityService gesture dispatch API.
 *
 * - COMPOSITE: Tries the RECORDED_MACRO first; if it fails within timeout, falls
 *   back to NATURAL_LANGUAGE.
 *
 * On step failure: captures a screenshot (if MediaProjection is running),
 * emits STEP_FAILED status, and posts a rich notification for the user to decide
 * whether to SKIP, RETRY, or ABORT the task.
 */
class GhostTaskExecutor(
    private val context: Context,
    private val repository: GhostTaskRepository,
) {

    private val _state = MutableStateFlow(GhostExecutionState())
    val state: StateFlow<GhostExecutionState> = _state.asStateFlow()

    // User decision signals for step failure (set by notification action receivers)
    @Volatile var pendingUserDecision: UserDecision? = null

    enum class UserDecision { SKIP, RETRY, ABORT }

    // ─── Entry Point ─────────────────────────────────────────────────────────

    /**
     * Runs the given [task] from the start.
     * Must be called from a coroutine (suspend function).
     *
     * Emits GhostExecutionState updates throughout execution.
     */
    suspend fun executeTask(task: GhostTask) {
        try {
            // Optional: launch the target app first
            if (task.targetAppPackage.isNotBlank()) {
                launchApp(task.targetAppPackage)
                delay(2000) // give the app time to open
            }

            for ((index, step) in task.steps.withIndex()) {
                emit(GhostExecutionState(
                    taskId = task.taskId,
                    taskName = task.name,
                    status = ExecutionStatus.RUNNING,
                    currentStepIndex = index,
                    currentStepLabel = step.label,
                    totalSteps = task.steps.size,
                ))

                val success = executeStep(step)

                if (!success) {
                    val screenshotFile = captureFailureScreenshot(task.taskId, step.stepId)
                    val failState = GhostExecutionState(
                        taskId = task.taskId,
                        taskName = task.name,
                        status = ExecutionStatus.STEP_FAILED,
                        currentStepIndex = index,
                        currentStepLabel = step.label,
                        totalSteps = task.steps.size,
                        failedStepIndex = index,
                        failedStepLabel = step.label,
                        screenshotPath = screenshotFile?.absolutePath,
                        errorMessage = "Step \"${step.label}\" could not be completed.",
                    )
                    emit(failState)

                    // Post notification and wait for user decision
                    GhostNotificationHelper.postStepFailureNotification(
                        context, task.name, step.label, screenshotFile, index, task.steps.size
                    )

                    val decision = waitForUserDecision()

                    when (decision) {
                        UserDecision.SKIP -> continue
                        UserDecision.ABORT -> {
                            emit(failState.copy(status = ExecutionStatus.ABORTED))
                            GhostNotificationHelper.cancelStepFailureNotification(context)
                            return
                        }
                        UserDecision.RETRY -> {
                            GhostNotificationHelper.cancelStepFailureNotification(context)
                            val retrySuccess = executeStep(step)
                            if (!retrySuccess) {
                                emit(failState.copy(errorMessage = "Retry also failed for \"${step.label}\". Skipping."))
                                continue
                            }
                        }
                        null -> continue // timeout — treat as skip
                    }
                }

                // Pause between steps to let the target app settle
                delay(step.timeout.coerceAtMost(800L).coerceAtLeast(300L) / 2)
            }

            // Task completed
            repository.incrementRunCount(task.taskId)
            emit(GhostExecutionState(
                taskId = task.taskId,
                taskName = task.name,
                status = ExecutionStatus.COMPLETED,
                currentStepIndex = task.steps.size - 1,
                totalSteps = task.steps.size,
                completedAt = System.currentTimeMillis(),
            ))
            GhostNotificationHelper.postTaskCompleteNotification(context, task.name)

        } catch (e: CancellationException) {
            emit(_state.value.copy(status = ExecutionStatus.ABORTED))
        } catch (e: Exception) {
            emit(_state.value.copy(status = ExecutionStatus.ABORTED, errorMessage = e.message ?: "Unknown error"))
        }
    }

    // ─── Step Execution ───────────────────────────────────────────────────────

    private suspend fun executeStep(step: GhostStep): Boolean = withContext(Dispatchers.Main) {
        val result = withTimeoutOrNull(step.timeout) {
            when (step.type) {
                StepType.NATURAL_LANGUAGE -> handleNlStep(step)
                StepType.RECORDED_MACRO   -> handleMacroStep(step)
                StepType.COMPOSITE        -> {
                    // Try macro first; fall back to NL on failure
                    val macroOk = if (step.events.isNotEmpty()) handleMacroStep(step) else false
                    if (!macroOk) handleNlStep(step) else true
                }
            }
        }
        result ?: false
    }

    /**
     * Natural Language step handler.
     * Parses simple intent keywords from the nlPrompt.
     *
     * Supported intents:
     *   - "open <app_name>"          → launches app by name via intent
     *   - "tap '<text>'"             → clickNodeWithText
     *   - "tap id '<view_id>'"       → clickNodeWithId
     *   - "type '<text>'"            → typeText
     *   - "scroll down/up"           → swipe gesture
     *   - "back"                     → pressBack()
     *   - "home"                     → pressHome()
     *   - "wait <N>s"                → delay
     *
     * ⚡ Extension Point: After matching basic intents, call the LLM Gateway with:
     *   - captureUiTree() as context
     *   - step.nlPrompt as user instruction
     *   - Ask LLM to return a structured JSON action (tap_text, tap_id, type, scroll, etc.)
     *   Then execute that action. This turns NL steps into fully AI-driven automation.
     */
    private suspend fun handleNlStep(step: GhostStep): Boolean {
        val prompt = step.nlPrompt.trim().lowercase()

        return when {
            prompt.startsWith("open ") -> {
                val appName = step.nlPrompt.removePrefix("open ").removePrefix("Open ").trim()
                launchApp(appName)
                delay(2000)
                true
            }
            prompt.startsWith("tap '") || prompt.startsWith("tap \"") -> {
                val text = extractQuoted(step.nlPrompt) ?: return false
                val svc = GhostAccessibilityService.instance ?: return false
                delay(300)
                svc.clickNodeWithText(text)
            }
            prompt.startsWith("tap id '") || prompt.startsWith("tap id \"") -> {
                val id = extractQuoted(step.nlPrompt.substringAfter("id ")) ?: return false
                val svc = GhostAccessibilityService.instance ?: return false
                svc.clickNodeWithId(id)
            }
            prompt.startsWith("type '") || prompt.startsWith("type \"") -> {
                val text = extractQuoted(step.nlPrompt) ?: return false
                val svc = GhostAccessibilityService.instance ?: return false
                delay(200)
                svc.typeText(text)
            }
            prompt == "back" || prompt == "go back" -> {
                GhostAccessibilityService.instance?.pressBack() ?: return false
                true
            }
            prompt == "home" || prompt == "go home" -> {
                GhostAccessibilityService.instance?.pressHome() ?: return false
                true
            }
            prompt.startsWith("scroll down") -> {
                scrollDown()
                true
            }
            prompt.startsWith("scroll up") -> {
                scrollUp()
                true
            }
            prompt.startsWith("wait ") -> {
                val seconds = prompt.removePrefix("wait ").removeSuffix("s").trim().toLongOrNull() ?: 1L
                delay(seconds * 1000L)
                true
            }
            else -> {
                // Unknown NL intent — log and return true (soft pass) to avoid blocking
                // Future: send to LLM for reasoning
                android.util.Log.w("GhostAgent", "Unrecognized NL step: ${step.nlPrompt}")
                true
            }
        }
    }

    /** Macro step handler — replays all recorded TouchEvents in sequence. */
    private suspend fun handleMacroStep(step: GhostStep): Boolean {
        val svc = GhostAccessibilityService.instance ?: return false
        for (event in step.events) {
            delay(event.delayAfterMs.coerceAtLeast(100L))
            replayTouchEvent(svc, event)
        }
        return true
    }

    private suspend fun replayTouchEvent(svc: GhostAccessibilityService, event: TouchEvent) {
        when (event.type) {
            "tap"        -> withContext(Dispatchers.Main) { svc.tap(event.x, event.y) }
            "long_press" -> withContext(Dispatchers.Main) { svc.longPress(event.x, event.y) }
            "swipe"      -> withContext(Dispatchers.Main) { svc.swipe(event.x, event.y, event.x2, event.y2, event.durationMs) }
            "type_text"  -> withContext(Dispatchers.Main) { svc.typeText(event.text) }
            "scroll"     -> {
                if (event.y2 > event.y) scrollDown() else scrollUp()
            }
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun launchApp(packageOrName: String) {
        val pm = context.packageManager
        // Try as package name first
        val launchIntent = pm.getLaunchIntentForPackage(packageOrName)
            ?: pm.queryIntentActivities(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
            ).firstOrNull { ri ->
                ri.loadLabel(pm).toString().lowercase().contains(packageOrName.lowercase())
            }?.let { ri ->
                pm.getLaunchIntentForPackage(ri.activityInfo.packageName)
            }

        launchIntent?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(it)
        }
    }

    private fun scrollDown() {
        val svc = GhostAccessibilityService.instance ?: return
        svc.swipe(540f, 1600f, 540f, 800f, 400L)
    }

    private fun scrollUp() {
        val svc = GhostAccessibilityService.instance ?: return
        svc.swipe(540f, 800f, 540f, 1600f, 400L)
    }

    private fun extractQuoted(text: String): String? {
        val singleQ = Regex("'([^']+)'").find(text)?.groupValues?.getOrNull(1)
        val doubleQ = Regex("\"([^\"]+)\"").find(text)?.groupValues?.getOrNull(1)
        return singleQ ?: doubleQ
    }

    private fun captureFailureScreenshot(taskId: String, stepId: String): java.io.File? {
        val captureSvc = GhostScreenCaptureService.instance ?: return null
        val file = repository.getScreenshotFile(taskId, stepId)
        return captureSvc.captureScreenshot(file)
    }

    private suspend fun waitForUserDecision(): UserDecision? {
        // Wait up to 5 minutes for user input
        val deadline = SystemClock.elapsedRealtime() + 5 * 60 * 1000L
        while (SystemClock.elapsedRealtime() < deadline) {
            val decision = pendingUserDecision
            if (decision != null) {
                pendingUserDecision = null
                return decision
            }
            delay(500)
        }
        return null
    }

    private fun emit(state: GhostExecutionState) {
        _state.value = state
        GhostAccessibilityService.updateExecutionState(state)
    }
}
