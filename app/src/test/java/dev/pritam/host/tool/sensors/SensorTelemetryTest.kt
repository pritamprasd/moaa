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
    fun testUpdateIntervalDurations() {
        assertEquals(1000L, UpdateInterval.EVERY_1_SEC.delayMs)
        assertEquals(2000L, UpdateInterval.EVERY_2_SEC.delayMs)
        assertEquals(5000L, UpdateInterval.EVERY_5_SEC.delayMs)
        assertEquals(0L, UpdateInterval.LIVE_FAST.delayMs)
        assertEquals(-1L, UpdateInterval.PAUSED.delayMs)
    }
}
