package dev.pritam.host.tool.sensors.ui.components

import android.hardware.Sensor
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.pritam.host.tool.sensors.model.SensorValueReading

/**
 * Hub component that renders the specialized 3D instrument / gauge for a sensor,
 * paired with a real-time 30s/1m historical streaming chart.
 *
 * Performance guarantee:
 * Telemetry history recording is lazy and only active when [isExpanded] is true.
 * Sliding window buffers strictly trim points older than 60 seconds to avoid memory spikes.
 */
@Composable
fun SensorVisualizerHub(
    sensorType: Int,
    reading: SensorValueReading?,
    isExpanded: Boolean,
    modifier: Modifier = Modifier
) {
    // Lazy recording: only records when card is expanded to maintain 60/120 FPS
    val historyBuffer = rememberSensorHistoryBuffer(
        sensorType = sensorType,
        rawValues = reading?.rawValues,
        isActive = isExpanded
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Specialized 3D Visualizer / Instrument Gauge
        when (sensorType) {
            Sensor.TYPE_ROTATION_VECTOR,
            Sensor.TYPE_GAME_ROTATION_VECTOR,
            Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> {
                SensorOrientationCube3D(rawValues = reading?.rawValues)
            }

            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_GRAVITY,
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                SensorVectorSphere3D(
                    rawValues = reading?.rawValues,
                    unit = reading?.unit ?: "m/s²"
                )
            }

            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_GYROSCOPE_UNCALIBRATED -> {
                SensorGimbalRings(rawValues = reading?.rawValues)
            }

            Sensor.TYPE_MAGNETIC_FIELD,
            Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED -> {
                SensorCompassRose(rawValues = reading?.rawValues)
            }

            Sensor.TYPE_LIGHT -> {
                SensorLuxGauge(rawValues = reading?.rawValues)
            }

            Sensor.TYPE_PRESSURE -> {
                SensorAltimeterDial(rawValues = reading?.rawValues)
            }

            Sensor.TYPE_PROXIMITY -> {
                SensorSonarRadar(rawValues = reading?.rawValues)
            }

            Sensor.TYPE_AMBIENT_TEMPERATURE,
            Sensor.TYPE_RELATIVE_HUMIDITY -> {
                SensorThermalGauge(
                    rawValues = reading?.rawValues,
                    unit = reading?.unit ?: ""
                )
            }

            Sensor.TYPE_STEP_COUNTER,
            Sensor.TYPE_STEP_DETECTOR -> {
                SensorStepCadence(rawValues = reading?.rawValues)
            }

            else -> {
                // For other sensor types, the streaming telemetry chart provides the primary visual
            }
        }

        // 2. 30-Second / 1-Minute Historical Telemetry Chart
        SensorStreamingChart(
            historyBuffer = historyBuffer,
            unit = reading?.unit ?: ""
        )
    }
}
