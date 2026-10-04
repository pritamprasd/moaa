package dev.pritam.host.tool.sensors

import android.hardware.Sensor
import dev.pritam.host.tool.sensors.model.SensorCategory
import dev.pritam.host.tool.sensors.model.SensorTelemetryFormatter
import dev.pritam.host.tool.sensors.model.UpdateInterval
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SensorTelemetryTest {

    @Test
    fun testCategorizeSensors() {
        assertEquals(SensorCategory.MOTION, SensorTelemetryFormatter.categorizeSensor(Sensor.TYPE_ACCELEROMETER))
        assertEquals(SensorCategory.MOTION, SensorTelemetryFormatter.categorizeSensor(Sensor.TYPE_GYROSCOPE))
        assertEquals(SensorCategory.ENVIRONMENT, SensorTelemetryFormatter.categorizeSensor(Sensor.TYPE_LIGHT))
        assertEquals(SensorCategory.ENVIRONMENT, SensorTelemetryFormatter.categorizeSensor(Sensor.TYPE_PRESSURE))
        assertEquals(SensorCategory.POSITION, SensorTelemetryFormatter.categorizeSensor(Sensor.TYPE_MAGNETIC_FIELD))
        assertEquals(SensorCategory.POSITION, SensorTelemetryFormatter.categorizeSensor(Sensor.TYPE_PROXIMITY))
        assertEquals(SensorCategory.HEALTH, SensorTelemetryFormatter.categorizeSensor(Sensor.TYPE_STEP_COUNTER))
    }

    @Test
    fun testFormatAccelerometerReading() {
        val values = floatArrayOf(0.0f, 9.80665f, 0.0f)
        val reading = SensorTelemetryFormatter.formatReading(
            type = Sensor.TYPE_ACCELEROMETER,
            values = values,
            accuracy = 3,
            timeFormatted = "12:00:00.000"
        )

        assertEquals("m/s²", reading.unit)
        assertEquals("High Accuracy", reading.accuracyLabel)
        assertTrue(reading.primaryDisplay.contains("9.81 m/s²"))
        assertEquals(4, reading.formattedAxes.size) // X, Y, Z, Total
    }

    @Test
    fun testFormatLightReading() {
        val values = floatArrayOf(450.0f)
        val reading = SensorTelemetryFormatter.formatReading(
            type = Sensor.TYPE_LIGHT,
            values = values,
            accuracy = 3,
            timeFormatted = "12:00:00.000"
        )

        assertEquals("lx", reading.unit)
        assertTrue(reading.primaryDisplay.contains("450.0 lx"))
        assertEquals("Bright Indoors", reading.formattedAxes[1].second)
    }

    @Test
    fun testFormatPressureReading() {
        val values = floatArrayOf(1013.25f)
        val reading = SensorTelemetryFormatter.formatReading(
            type = Sensor.TYPE_PRESSURE,
            values = values,
            accuracy = 3,
            timeFormatted = "12:00:00.000"
        )

        assertEquals("hPa", reading.unit)
        assertTrue(reading.primaryDisplay.contains("1013.25 hPa"))
    }

    @Test
    fun testFormatTemperatureReading() {
        val values = floatArrayOf(25.0f)
        val reading = SensorTelemetryFormatter.formatReading(
            type = Sensor.TYPE_AMBIENT_TEMPERATURE,
            values = values,
            accuracy = 3,
            timeFormatted = "12:00:00.000"
        )

        assertEquals("°C", reading.unit)
        assertTrue(reading.primaryDisplay.contains("25.0 °C"))
        assertTrue(reading.primaryDisplay.contains("77.0 °F"))
        assertTrue(reading.formattedAxes.isEmpty())
    }

    @Test
    fun testUpdateIntervalDurations() {
        assertEquals(1000L, UpdateInterval.EVERY_1_SEC.delayMs)
        assertEquals(2000L, UpdateInterval.EVERY_2_SEC.delayMs)
        assertEquals(5000L, UpdateInterval.EVERY_5_SEC.delayMs)
        assertEquals(0L, UpdateInterval.LIVE_FAST.delayMs)
        assertEquals(-1L, UpdateInterval.PAUSED.delayMs)
    }

    @Test
    fun testSensorExplanation() {
        val accelExp = SensorTelemetryFormatter.getSensorExplanation(Sensor.TYPE_ACCELEROMETER)
        assertTrue(accelExp.userMeaning.contains("acceleration"))
        assertTrue(accelExp.developerGuide.contains("shake detection") || accelExp.developerGuide.contains("X ="))

        val lightExp = SensorTelemetryFormatter.getSensorExplanation(Sensor.TYPE_LIGHT)
        assertTrue(lightExp.userMeaning.contains("lux"))
        assertTrue(lightExp.developerGuide.contains("brightness"))

        val proxExp = SensorTelemetryFormatter.getSensorExplanation(Sensor.TYPE_PROXIMITY)
        assertTrue(proxExp.userMeaning.contains("screen") || proxExp.userMeaning.contains("close"))
        assertTrue(proxExp.developerGuide.contains("calls") || proxExp.developerGuide.contains("near"))
    }

    @Test
    fun testSensorHistoryBufferPruningAndClear() {
        val buffer = dev.pritam.host.tool.sensors.ui.components.SensorHistoryBuffer(maxRetentionMs = 1_000L)
        assertTrue(buffer.points.isEmpty())

        // Record initial points
        buffer.record(listOf(1.0f, 2.0f, 3.0f))
        assertEquals(1, buffer.points.size)
        assertEquals(listOf(1.0f, 2.0f, 3.0f), buffer.points.first().values)

        // Clear buffer
        buffer.clear()
        assertTrue(buffer.points.isEmpty())

        // Insert manually with simulated past timestamp to verify pruning
        buffer.points.add(dev.pritam.host.tool.sensors.ui.components.TelemetryDataPoint(
            timestampMs = System.currentTimeMillis() - 2000L,
            values = listOf(99f)
        ))
        assertEquals(1, buffer.points.size)

        // Recording a new point must prune the point older than 1000ms
        buffer.record(listOf(4.0f, 5.0f))
        assertEquals(1, buffer.points.size)
        assertEquals(listOf(4.0f, 5.0f), buffer.points.first().values)
    }
}

