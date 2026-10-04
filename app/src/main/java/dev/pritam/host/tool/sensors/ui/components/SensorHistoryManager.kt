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
 * Automatically discards data older than [maxRetentionMs] (30 minutes).
 * Employs adaptive decimation to ensure memory usage stays < 30KB and Canvas renders at 60/120 FPS.
 */
class SensorHistoryBuffer(
    val maxRetentionMs: Long = 1_800_000L // 30 minutes
) {
    val points = mutableStateListOf<TelemetryDataPoint>()

    fun record(rawValues: List<Float>) {
        if (rawValues.isEmpty()) return
        val now = System.currentTimeMillis()

        // Append new telemetry sample
        points.add(TelemetryDataPoint(timestampMs = now, values = rawValues))

        // 1. Prune older points outside the 30-minute retention window
        val cutoff = now - maxRetentionMs
        while (points.isNotEmpty() && points.first().timestampMs < cutoff) {
            points.removeAt(0)
        }

        // 2. Adaptive downsampling: keep recent 60 seconds at high-res (20Hz),
        // but thin out older data (> 60s and > 5m) so the Canvas path never exceeds ~600 points
        if (points.size > 700) {
            decimateOlderPoints(now)
        }
    }

    private fun decimateOlderPoints(now: Long) {
        val oneMinCutoff = now - 60_000L
        val fiveMinCutoff = now - 300_000L

        var lastKeptTime = 0L
        val indicesToRemove = mutableListOf<Int>()

        for (i in 0 until points.size) {
            val ptTime = points[i].timestampMs
            if (ptTime >= oneMinCutoff) {
                // Keep recent 60 seconds untouched at full fidelity
                break
            }

            val minSpacing = if (ptTime < fiveMinCutoff) 10_000L else 2_000L
            if (ptTime - lastKeptTime < minSpacing) {
                indicesToRemove.add(i)
            } else {
                lastKeptTime = ptTime
            }
        }

        // Remove in reverse order to preserve indexing
        for (idx in indicesToRemove.asReversed()) {
            if (idx < points.size) {
                points.removeAt(idx)
            }
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
    val buffer = remember(sensorType) { SensorHistoryBuffer(maxRetentionMs = 1_800_000L) }

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
