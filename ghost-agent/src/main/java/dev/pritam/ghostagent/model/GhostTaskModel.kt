package dev.pritam.ghostagent.model

import org.json.JSONArray
import org.json.JSONObject

// ─────────────────────────────────────────────────────────────────────────────
// TASK DSL MODEL
// A GhostTask is a named automation workflow composed of sequential steps.
// Steps can be:
//   - NaturalLanguage: high-level instruction interpreted by the executor
//   - RecordedMacro:   precise replay of recorded touch/input events
//   - Composite:       a NL goal with a recorded fallback
// ─────────────────────────────────────────────────────────────────────────────

enum class StepType { NATURAL_LANGUAGE, RECORDED_MACRO, COMPOSITE }

/**
 * A single recorded touch/input event for macro playback.
 * Coordinates are in screen pixels (raw values from MotionEvent).
 */
data class TouchEvent(
    val type: String,       // "tap" | "swipe" | "long_press" | "type_text" | "scroll"
    val x: Float = 0f,
    val y: Float = 0f,
    val x2: Float = 0f,     // swipe end x
    val y2: Float = 0f,     // swipe end y
    val text: String = "",  // for type_text events
    val durationMs: Long = 100L,
    val delayAfterMs: Long = 500L,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("type", type); put("x", x); put("y", y)
        put("x2", x2); put("y2", y2); put("text", text)
        put("duration_ms", durationMs); put("delay_after_ms", delayAfterMs)
    }

    companion object {
        fun fromJson(obj: JSONObject) = TouchEvent(
            type = obj.optString("type", "tap"),
            x = obj.optDouble("x", 0.0).toFloat(),
            y = obj.optDouble("y", 0.0).toFloat(),
            x2 = obj.optDouble("x2", 0.0).toFloat(),
            y2 = obj.optDouble("y2", 0.0).toFloat(),
            text = obj.optString("text", ""),
            durationMs = obj.optLong("duration_ms", 100L),
            delayAfterMs = obj.optLong("delay_after_ms", 500L),
        )
    }
}

/**
 * A single step in a GhostTask.
 *
 * @param stepId    Unique ID within the task.
 * @param type      StepType determines execution strategy.
 * @param label     Short human-readable label (shown in the step list UI).
 * @param nlPrompt  Natural-language instruction (used for NL and Composite steps).
 * @param events    Recorded touch events (used for Macro and Composite fallback).
 * @param timeout   Max time (ms) to wait for the step to complete before failing.
 */
data class GhostStep(
    val stepId: String,
    val type: StepType,
    val label: String,
    val nlPrompt: String = "",
    val events: List<TouchEvent> = emptyList(),
    val timeout: Long = 15_000L,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("step_id", stepId)
        put("type", type.name)
        put("label", label)
        put("nl_prompt", nlPrompt)
        put("timeout_ms", timeout)
        val eventsArr = JSONArray()
        events.forEach { eventsArr.put(it.toJson()) }
        put("events", eventsArr)
    }

    companion object {
        fun fromJson(obj: JSONObject): GhostStep {
            val eventsArr = obj.optJSONArray("events") ?: JSONArray()
            val events = (0 until eventsArr.length()).map { TouchEvent.fromJson(eventsArr.getJSONObject(it)) }
            return GhostStep(
                stepId = obj.optString("step_id", "step_${System.currentTimeMillis()}"),
                type = runCatching { StepType.valueOf(obj.optString("type", "NATURAL_LANGUAGE")) }.getOrDefault(StepType.NATURAL_LANGUAGE),
                label = obj.optString("label", "Step"),
                nlPrompt = obj.optString("nl_prompt", ""),
                timeout = obj.optLong("timeout_ms", 15_000L),
                events = events,
            )
        }
    }
}

/** Runtime status of a task or individual step. */
enum class ExecutionStatus {
    IDLE, RUNNING, PAUSED, WAITING_USER, STEP_FAILED, COMPLETED, ABORTED
}

/**
 * A complete automation workflow.
 *
 * @param taskId        UUID for the task.
 * @param name          Human-readable name shown in the UI.
 * @param description   Short description of what the task does.
 * @param targetAppPackage  If set, Ghost Agent will launch this app before starting.
 * @param steps         Ordered list of steps.
 * @param createdAt     Creation timestamp (epoch ms).
 * @param updatedAt     Last edit timestamp (epoch ms).
 * @param runCount      How many times the task has been executed.
 */
data class GhostTask(
    val taskId: String,
    val name: String,
    val description: String = "",
    val targetAppPackage: String = "",
    val steps: List<GhostStep> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val runCount: Int = 0,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("task_id", taskId)
        put("name", name)
        put("description", description)
        put("target_app_package", targetAppPackage)
        put("created_at", createdAt)
        put("updated_at", updatedAt)
        put("run_count", runCount)
        val stepsArr = JSONArray()
        steps.forEach { stepsArr.put(it.toJson()) }
        put("steps", stepsArr)
    }

    companion object {
        fun fromJson(obj: JSONObject): GhostTask {
            val stepsArr = obj.optJSONArray("steps") ?: JSONArray()
            val steps = (0 until stepsArr.length()).map { GhostStep.fromJson(stepsArr.getJSONObject(it)) }
            return GhostTask(
                taskId = obj.optString("task_id", "task_${System.currentTimeMillis()}"),
                name = obj.optString("name", "Unnamed Task"),
                description = obj.optString("description", ""),
                targetAppPackage = obj.optString("target_app_package", ""),
                createdAt = obj.optLong("created_at", System.currentTimeMillis()),
                updatedAt = obj.optLong("updated_at", System.currentTimeMillis()),
                runCount = obj.optInt("run_count", 0),
                steps = steps,
            )
        }
    }
}

/** Snapshot of a running task's execution state, emitted to UI via StateFlow. */
data class GhostExecutionState(
    val taskId: String = "",
    val taskName: String = "",
    val status: ExecutionStatus = ExecutionStatus.IDLE,
    val currentStepIndex: Int = -1,
    val currentStepLabel: String = "",
    val totalSteps: Int = 0,
    val failedStepIndex: Int = -1,
    val failedStepLabel: String = "",
    val screenshotPath: String? = null,  // path to screenshot for the stuck/failed step
    val errorMessage: String = "",
    val completedAt: Long? = null,
)
