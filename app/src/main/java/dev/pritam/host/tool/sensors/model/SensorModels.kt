package dev.pritam.host.tool.sensors.model

import android.hardware.Sensor
import java.util.Locale
import kotlin.math.sqrt

enum class SensorCategory(val displayName: String, val iconLabel: String) {
    ALL("All", "⚡"),
    MOTION("Motion", "🏃"),
    ENVIRONMENT("ENV", "🌡️"),
    POSITION("Position", "🧭"),
    HEALTH("Health", "❤️"),
    OTHER("System", "⚙️")
}

enum class UpdateInterval(val displayName: String, val delayMs: Long) {
    LIVE_FAST("Live (Fast)", 0L),
    EVERY_1_SEC("1 sec", 1000L),
    EVERY_2_SEC("2 sec", 2000L),
    EVERY_5_SEC("5 sec", 5000L),
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

data class SensorExplanation(
    val userMeaning: String,
    val developerGuide: String
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

    fun getSensorExplanation(type: Int): SensorExplanation {
        return when (type) {
            Sensor.TYPE_ACCELEROMETER -> SensorExplanation(
                userMeaning = "Measures total acceleration forces in m/s² acting on the phone across 3D space (X, Y, Z), including Earth's gravity (~9.81 m/s² downward at rest).",
                developerGuide = "X = horizontal tilt, Y = vertical tilt, Z = screen facing up (+9.81) or down (-9.81). Total magnitude = √(x²+y²+z²). Used for shake detection, screen orientation, and movement gestures."
            )
            Sensor.TYPE_LINEAR_ACCELERATION -> SensorExplanation(
                userMeaning = "Measures pure physical acceleration forces in m/s² excluding gravity (reads 0.00 m/s² when stationary).",
                developerGuide = "Isolates device motion from gravity using sensor fusion. Ideal for gesture recognition, step detection, vehicle speed tracking, and physics calculations without gravity subtraction."
            )
            Sensor.TYPE_GRAVITY -> SensorExplanation(
                userMeaning = "Isolates the direction and magnitude of Earth's gravity vector in m/s² (~9.81 m/s² total).",
                developerGuide = "Extracted via filtering of accelerometer data. Used for spirit levels, measuring incline/tilt relative to Earth, and orientation tracking."
            )
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_GYROSCOPE_UNCALIBRATED -> SensorExplanation(
                userMeaning = "Measures the rate of rotation around the phone's 3 physical axes in radians/sec (rad/s).",
                developerGuide = "X = Pitch (nodding), Y = Roll (tilting side-to-side), Z = Yaw (compass spin). Critical for 3D camera tracking, mobile gaming, VR/AR, and Optical Image Stabilization (OIS)."
            )
            Sensor.TYPE_MAGNETIC_FIELD,
            Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED -> SensorExplanation(
                userMeaning = "Measures ambient geomagnetic field strength in microteslas (µT). Earth's natural magnetic field is typically between 30 µT and 60 µT.",
                developerGuide = "Provides geomagnetic flux vectors. Combined with accelerometer in SensorManager.getRotationMatrix() to compute true Compass Azimuth/Heading, or for metal/magnet detection."
            )
            Sensor.TYPE_ROTATION_VECTOR,
            Sensor.TYPE_GAME_ROTATION_VECTOR,
            Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> SensorExplanation(
                userMeaning = "Calculates the exact 3D orientation of the phone in space as a unit quaternion, completely free from gimbal lock.",
                developerGuide = "Fusion of accelerometer, gyroscope, and magnetometer. Direct input for 3D graphics (OpenGL/Vulkan), ARCore, VR head tracking, and SensorManager.getRotationMatrixFromVector()."
            )
            Sensor.TYPE_LIGHT -> SensorExplanation(
                userMeaning = "Measures ambient room illumination in lux (lx). Varies from <10 lx in dark rooms to >10,000 lx in bright sunlight.",
                developerGuide = "Single scalar value. Used to trigger automatic screen brightness, dark mode themes, and detecting indoor vs. outdoor environments."
            )
            Sensor.TYPE_PROXIMITY -> SensorExplanation(
                userMeaning = "Detects whether an object (such as your ear or hand) is close to the top of the phone screen.",
                developerGuide = "Most hardware sensors report binary states: near (0 cm) vs. far (e.g. 5 cm). Used during calls to turn off the display and touch digitizer to prevent accidental cheek inputs."
            )
            Sensor.TYPE_PRESSURE -> SensorExplanation(
                userMeaning = "Measures ambient atmospheric pressure in hectopascals (hPa / mbar). Sea-level standard is ~1013.25 hPa.",
                developerGuide = "Single scalar value (~1.2 hPa drop per 10m elevation). Used for hypsometric altitude calculation, indoor floor-level GPS navigation, and barometric weather trend tracking."
            )
            Sensor.TYPE_AMBIENT_TEMPERATURE,
            Sensor.TYPE_TEMPERATURE -> SensorExplanation(
                userMeaning = "Measures surrounding ambient air temperature in Celsius (°C) and Fahrenheit (°F).",
                developerGuide = "Environmental temperature reading. Note: phone internal processor/battery heat can cause thermal drift under heavy CPU/GPU load."
            )
            Sensor.TYPE_RELATIVE_HUMIDITY -> SensorExplanation(
                userMeaning = "Measures ambient relative air humidity percentage (0% to 100%).",
                developerGuide = "Environmental moisture metric. Combined with ambient temperature to compute dew point and heat comfort index."
            )
            Sensor.TYPE_STEP_COUNTER,
            Sensor.TYPE_STEP_DETECTOR -> SensorExplanation(
                userMeaning = "Tracks footsteps taken by the user since the phone was last booted up.",
                developerGuide = "Dedicated hardware low-power pedometer ASIC. Fires events without waking up the main CPU, providing high-efficiency fitness step counting."
            )
            Sensor.TYPE_HEART_RATE,
            Sensor.TYPE_HEART_BEAT -> SensorExplanation(
                userMeaning = "Measures instantaneous pulse rate in beats per minute (BPM) via optical photoplethysmography (PPG).",
                developerGuide = "Returns heart rate in BPM with sensor accuracy status. Used in fitness trackers and biometric health diagnostics."
            )
            Sensor.TYPE_SIGNIFICANT_MOTION -> SensorExplanation(
                userMeaning = "Detects when the user has picked up the device or started walking/driving.",
                developerGuide = "One-shot trigger sensor. Automatically disables itself after firing; used to wake up high-accuracy GPS or initiate activity recognition."
            )
            else -> SensorExplanation(
                userMeaning = "Streams real-time physical telemetry directly from the device's hardware sensor bus.",
                developerGuide = "Values array maps directly to Android SensorEvent.values. See hardware HAL driver documentation for unit conventions."
            )
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
                    primaryDisplay = String.format(Locale.US, "%.1f °C (%.1f °F)", tempC, tempF),
                    formattedAxes = emptyList(),
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
