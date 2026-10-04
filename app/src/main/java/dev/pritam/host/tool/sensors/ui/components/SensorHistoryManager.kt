package dev.pritam.host.tool.sensors.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Represents a single snapshot point in time for multi-channel sensor telemetry.
 */
data class TelemetryDataPoint(
    val timestampMs: Long,
    val values: List<Float>
)

/**
 * Lightweight, zero-leakage sliding window history buffer for an active sensor.
 * Automatically discards data older than [maxRetentionMs] (defaults to 60 seconds).
 * Ensures memory overhead never exceeds a few kilobytes per sensor.
 */
class SensorHistoryBuffer(
    val maxRetentionMs: Long = 60_000L
) {
    val points = mutableStateListOf<TelemetryDataPoint>()

    fun record(rawValues: List<Float>) {
        if (rawValues.isEmpty()) return
        val now = System.currentTimeMillis()

        // Append new data point
        points.add(TelemetryDataPoint(timestampMs = now, values = rawValues))

        // Prune older points outside the retention window
        val cutoff = now - maxRetentionMs
        while (points.isNotEmpty() && points.first().timestampMs < cutoff) {
            points.removeAt(0)
        }
    }

    fun clear() {
        points.clear()
    }
}

/**
 * Remembers a [SensorHistoryBuffer] that only records when [isActive] is true.
 * Cleans up and clears buffer when disposed to prevent memory leaks and keep app fast.
 */
@Composable
fun rememberSensorHistoryBuffer(
    sensorType: Int,
    rawValues: List<Float>?,
    isActive: Boolean
): SensorHistoryBuffer {
    val buffer = remember(sensorType) { SensorHistoryBuffer(maxRetentionMs = 60_000L) }

    // Throttle recording to ~15-20Hz max for history chart to keep CPU & memory usage near zero
    var lastRecordedMs by remember(sensorType) { mutableLongStateOf(0L) }

    if (isActive && rawValues != null && rawValues.isNotEmpty()) {
        val now = System.currentTimeMillis()
        if (now - lastRecordedMs >= 50L) { // ~20 updates/sec max
            buffer.record(rawValues)
            lastRecordedMs = now
        }
    }

    DisposableEffect(sensorType) {
        onDispose {
            buffer.clear()
        }
    }

    return buffer
}
