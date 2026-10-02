package dev.motherofallapps.host.tool.sensors.model

import android.hardware.Sensor
import java.util.Locale
import kotlin.math.sqrt

enum class SensorCategory(val displayName: String, val iconLabel: String) {
    ALL("All", "⚡"),
    MOTION("Motion", "🏃"),
    ENVIRONMENT("Environment", "🌡️"),
    POSITION("Position", "🧭"),
    HEALTH("Health & Step", "❤️"),
    OTHER("System & Other", "⚙️")
}

enum class UpdateInterval(val displayName: String, val delayMs: Long) {
    LIVE_FAST("Live (Fast)", 0L),
    EVERY_1_SEC("1s Interval", 1000L),
    EVERY_2_SEC("2s Interval", 2000L),
    EVERY_5_SEC("5s Interval", 5000L),
    PAUSED("Paused", -1L)
}

data class SensorInfoItem(
    val id: Int,
    val type: Int,
    val name: String,
    val vendor: String,
    val version: Int,
    val stringType: String,
    val category: SensorCategory,
    val maxRange: Float,
    val resolution: Float,
    val powerMa: Float,
    val minDelayUs: Int,
    val fifoMaxEventCount: Int,
    val isWakeUp: Boolean,
    val isDynamic: Boolean,
    val reportingMode: String
)

data class SensorValueReading(
    val sensorType: Int,
    val timestampNanos: Long,
    val accuracy: Int,
    val accuracyLabel: String,
    val rawValues: List<Float>,
    val primaryDisplay: String,
    val formattedAxes: List<Pair<String, String>>,
    val unit: String,
    val formattedTime: String
)

object SensorTelemetryFormatter {

    fun categorizeSensor(type: Int): SensorCategory {
        return when (type) {
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_ACCELEROMETER_UNCALIBRATED,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_GYROSCOPE_UNCALIBRATED,
            Sensor.TYPE_GRAVITY,
            Sensor.TYPE_LINEAR_ACCELERATION,
            Sensor.TYPE_ROTATION_VECTOR,
            Sensor.TYPE_GAME_ROTATION_VECTOR,
            Sensor.TYPE_SIGNIFICANT_MOTION -> SensorCategory.MOTION

            Sensor.TYPE_LIGHT,
            Sensor.TYPE_PRESSURE,
            Sensor.TYPE_AMBIENT_TEMPERATURE,
            Sensor.TYPE_RELATIVE_HUMIDITY,
            Sensor.TYPE_TEMPERATURE -> SensorCategory.ENVIRONMENT

            Sensor.TYPE_MAGNETIC_FIELD,
            Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED,
            Sensor.TYPE_PROXIMITY,
            Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR,
            Sensor.TYPE_ORIENTATION -> SensorCategory.POSITION

            Sensor.TYPE_STEP_COUNTER,
            Sensor.TYPE_STEP_DETECTOR,
            Sensor.TYPE_HEART_RATE,
            Sensor.TYPE_HEART_BEAT -> SensorCategory.HEALTH

            else -> SensorCategory.OTHER
        }
    }

    fun getAccuracyLabel(accuracy: Int): String {
        return when (accuracy) {
            3 -> "High Accuracy"
            2 -> "Medium Accuracy"
            1 -> "Low Accuracy"
            0 -> "Unreliable"
            else -> "Calibrating"
        }
    }

    fun formatReading(type: Int, values: FloatArray, accuracy: Int, timeFormatted: String): SensorValueReading {
        val rawList = values.toList()
        val accLabel = getAccuracyLabel(accuracy)

        return when (type) {
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_GRAVITY,
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                val x = values.getOrElse(0) { 0f }
                val y = values.getOrElse(1) { 0f }
                val z = values.getOrElse(2) { 0f }
                val mag = sqrt(x * x + y * y + z * z)
                SensorValueReading(
                    sensorType = type,
                    timestampNanos = System.nanoTime(),
                    accuracy = accuracy,
                    accuracyLabel = accLabel,
                    rawValues = rawList,
                    primaryDisplay = String.format(Locale.US, "%.2f m/s²", mag),
                    formattedAxes = listOf(
                        "X" to String.format(Locale.US, "%+.3f m/s²", x),
                        "Y" to String.format(Locale.US, "%+.3f m/s²", y),
                        "Z" to String.format(Locale.US, "%+.3f m/s²", z),
                        "Total" to String.format(Locale.US, "%.3f m/s²", mag)
                    ),
                    unit = "m/s²",
                    formattedTime = timeFormatted
                )
            }

            Sensor.TYPE_GYROSCOPE -> {
                val x = values.getOrElse(0) { 0f }
                val y = values.getOrElse(1) { 0f }
                val z = values.getOrElse(2) { 0f }
                val mag = sqrt(x * x + y * y + z * z)
                SensorValueReading(
                    sensorType = type,
                    timestampNanos = System.nanoTime(),
                    accuracy = accuracy,
                    accuracyLabel = accLabel,
                    rawValues = rawList,
                    primaryDisplay = String.format(Locale.US, "%.2f rad/s", mag),
                    formattedAxes = listOf(
                        "X (Roll)" to String.format(Locale.US, "%+.3f rad/s", x),
                        "Y (Pitch)" to String.format(Locale.US, "%+.3f rad/s", y),
                        "Z (Yaw)" to String.format(Locale.US, "%+.3f rad/s", z)
                    ),
                    unit = "rad/s",
                    formattedTime = timeFormatted
                )
            }

            Sensor.TYPE_MAGNETIC_FIELD -> {
                val x = values.getOrElse(0) { 0f }
                val y = values.getOrElse(1) { 0f }
                val z = values.getOrElse(2) { 0f }
                val mag = sqrt(x * x + y * y + z * z)
                SensorValueReading(
                    sensorType = type,
                    timestampNanos = System.nanoTime(),
                    accuracy = accuracy,
                    accuracyLabel = accLabel,
                    rawValues = rawList,
                    primaryDisplay = String.format(Locale.US, "%.1f µT", mag),
                    formattedAxes = listOf(
                        "X" to String.format(Locale.US, "%+.2f µT", x),
                        "Y" to String.format(Locale.US, "%+.2f µT", y),
                        "Z" to String.format(Locale.US, "%+.2f µT", z),
                        "Field Strength" to String.format(Locale.US, "%.2f µT", mag)
                    ),
                    unit = "µT",
                    formattedTime = timeFormatted
                )
            }

            Sensor.TYPE_LIGHT -> {
                val lux = values.getOrElse(0) { 0f }
                val description = when {
                    lux < 10 -> "Dark / Moon"
                    lux < 50 -> "Dim Room"
                    lux < 400 -> "Office Light"
                    lux < 1000 -> "Bright Indoors"
                    lux < 10000 -> "Overcast Daylight"
                    else -> "Direct Sunlight"
                }
                SensorValueReading(
                    sensorType = type,
                    timestampNanos = System.nanoTime(),
                    accuracy = accuracy,
                    accuracyLabel = accLabel,
                    rawValues = rawList,
                    primaryDisplay = String.format(Locale.US, "%.1f lx", lux),
                    formattedAxes = listOf(
                        "Illuminance" to String.format(Locale.US, "%.1f lx", lux),
                        "Environment" to description
                    ),
                    unit = "lx",
                    formattedTime = timeFormatted
                )
            }

            Sensor.TYPE_PRESSURE -> {
                val hPa = values.getOrElse(0) { 0f }
                SensorValueReading(
                    sensorType = type,
                    timestampNanos = System.nanoTime(),
                    accuracy = accuracy,
                    accuracyLabel = accLabel,
                    rawValues = rawList,
                    primaryDisplay = String.format(Locale.US, "%.2f hPa", hPa),
                    formattedAxes = listOf(
                        "Pressure" to String.format(Locale.US, "%.2f hPa (mbar)", hPa),
                        "Atmosphere" to String.format(Locale.US, "%.4f atm", hPa / 1013.25f)
                    ),
                    unit = "hPa",
                    formattedTime = timeFormatted
                )
            }

            Sensor.TYPE_PROXIMITY -> {
                val dist = values.getOrElse(0) { 0f }
                val state = if (dist < 3f) "NEAR (Covered)" else "FAR (Clear)"
                SensorValueReading(
                    sensorType = type,
                    timestampNanos = System.nanoTime(),
                    accuracy = accuracy,
                    accuracyLabel = accLabel,
                    rawValues = rawList,
                    primaryDisplay = String.format(Locale.US, "%.1f cm (%s)", dist, state),
                    formattedAxes = listOf(
                        "Distance" to String.format(Locale.US, "%.1f cm", dist),
                        "State" to state
                    ),
                    unit = "cm",
                    formattedTime = timeFormatted
                )
            }

            Sensor.TYPE_AMBIENT_TEMPERATURE,
            Sensor.TYPE_TEMPERATURE -> {
                val tempC = values.getOrElse(0) { 0f }
                val tempF = tempC * 9f / 5f + 32f
                SensorValueReading(
                    sensorType = type,
                    timestampNanos = System.nanoTime(),
                    accuracy = accuracy,
                    accuracyLabel = accLabel,
                    rawValues = rawList,
                    primaryDisplay = String.format(Locale.US, "%.1f °C", tempC),
                    formattedAxes = listOf(
                        "Celsius" to String.format(Locale.US, "%.2f °C", tempC),
                        "Fahrenheit" to String.format(Locale.US, "%.2f °F", tempF)
                    ),
                    unit = "°C",
                    formattedTime = timeFormatted
                )
            }

            Sensor.TYPE_RELATIVE_HUMIDITY -> {
                val humidity = values.getOrElse(0) { 0f }
                SensorValueReading(
                    sensorType = type,
                    timestampNanos = System.nanoTime(),
                    accuracy = accuracy,
                    accuracyLabel = accLabel,
                    rawValues = rawList,
                    primaryDisplay = String.format(Locale.US, "%.1f %%", humidity),
                    formattedAxes = listOf(
                        "Relative Humidity" to String.format(Locale.US, "%.1f %%", humidity)
                    ),
                    unit = "%",
                    formattedTime = timeFormatted
                )
            }

            Sensor.TYPE_STEP_COUNTER -> {
                val steps = values.getOrElse(0) { 0f }.toInt()
                SensorValueReading(
                    sensorType = type,
                    timestampNanos = System.nanoTime(),
                    accuracy = accuracy,
                    accuracyLabel = accLabel,
                    rawValues = rawList,
                    primaryDisplay = "$steps steps",
                    formattedAxes = listOf(
                        "Total Steps (Boot)" to "$steps steps"
                    ),
                    unit = "steps",
                    formattedTime = timeFormatted
                )
            }

            Sensor.TYPE_ROTATION_VECTOR,
            Sensor.TYPE_GAME_ROTATION_VECTOR -> {
                val x = values.getOrElse(0) { 0f }
                val y = values.getOrElse(1) { 0f }
                val z = values.getOrElse(2) { 0f }
                val cos = values.getOrElse(3) { 0f }
                SensorValueReading(
                    sensorType = type,
                    timestampNanos = System.nanoTime(),
                    accuracy = accuracy,
                    accuracyLabel = accLabel,
                    rawValues = rawList,
                    primaryDisplay = String.format(Locale.US, "Quaternion (%.2f, %.2f, %.2f)", x, y, z),
                    formattedAxes = listOf(
                        "X * sin(θ/2)" to String.format(Locale.US, "%+.3f", x),
                        "Y * sin(θ/2)" to String.format(Locale.US, "%+.3f", y),
                        "Z * sin(θ/2)" to String.format(Locale.US, "%+.3f", z),
                        "cos(θ/2)" to String.format(Locale.US, "%+.3f", cos)
                    ),
                    unit = "quat",
                    formattedTime = timeFormatted
                )
            }

            else -> {
                val firstVal = values.getOrElse(0) { 0f }
                val axes = values.mapIndexed { idx, v ->
                    "Value[$idx]" to String.format(Locale.US, "%+.3f", v)
                }
                SensorValueReading(
                    sensorType = type,
                    timestampNanos = System.nanoTime(),
                    accuracy = accuracy,
                    accuracyLabel = accLabel,
                    rawValues = rawList,
                    primaryDisplay = String.format(Locale.US, "%.2f", firstVal),
                    formattedAxes = axes,
                    unit = "units",
                    formattedTime = timeFormatted
                )
            }
        }
    }
}
