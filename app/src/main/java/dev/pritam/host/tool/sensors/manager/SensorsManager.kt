package dev.pritam.host.tool.sensors.manager

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dev.pritam.host.logging.AppLogHub
import dev.pritam.host.logging.LogLevel
import dev.pritam.host.tool.sensors.model.SensorInfoItem
import dev.pritam.host.tool.sensors.model.SensorTelemetryFormatter
import dev.pritam.host.tool.sensors.model.SensorValueReading
import dev.pritam.host.tool.sensors.model.UpdateInterval
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class SensorsManager(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    private val _availableSensors = MutableStateFlow<List<SensorInfoItem>>(emptyList())
    val availableSensors: StateFlow<List<SensorInfoItem>> = _availableSensors.asStateFlow()

    private val _sensorReadings = MutableStateFlow<Map<Int, SensorValueReading>>(emptyMap())
    val sensorReadings: StateFlow<Map<Int, SensorValueReading>> = _sensorReadings.asStateFlow()

    private val _activeSensorTypes = MutableStateFlow<Set<Int>>(emptySet())
    val activeSensorTypes: StateFlow<Set<Int>> = _activeSensorTypes.asStateFlow()

    private val _updateInterval = MutableStateFlow(UpdateInterval.EVERY_1_SEC)
    val updateInterval: StateFlow<UpdateInterval> = _updateInterval.asStateFlow()

    // High-performance thread-safe internal reading cache for 0-allocation sensor event dispatching
    private val latestReadings = ConcurrentHashMap<Int, SensorValueReading>()
    @Volatile private var hasNewReadings = false
    private var batcherJob: Job? = null

    // Last emission timestamp per sensorType to enforce throttling
    private val lastEmissionTimes = ConcurrentHashMap<Int, Long>()
    private val sensorListeners = ConcurrentHashMap<Int, SensorEventListener>()

    init {
        activeInstance = this
        discoverAvailableSensors()
    }

    fun discoverAvailableSensors() {
        val sm = sensorManager ?: return
        val rawList = sm.getSensorList(Sensor.TYPE_ALL)

        val items = rawList.mapIndexed { index, s ->
            val reportingMode = when (s.reportingMode) {
                Sensor.REPORTING_MODE_CONTINUOUS -> "Continuous"
                Sensor.REPORTING_MODE_ON_CHANGE -> "On Change"
                Sensor.REPORTING_MODE_ONE_SHOT -> "One Shot"
                Sensor.REPORTING_MODE_SPECIAL_TRIGGER -> "Special Trigger"
                else -> "Standard"
            }

            SensorInfoItem(
                id = index,
                type = s.type,
                name = s.name.ifBlank { "Sensor #${s.type}" },
                vendor = s.vendor.ifBlank { "Generic / Android HAL" },
                version = s.version,
                stringType = s.stringType ?: "android.sensor.type_${s.type}",
                category = SensorTelemetryFormatter.categorizeSensor(s.type),
                maxRange = s.maximumRange,
                resolution = s.resolution,
                powerMa = s.power,
                minDelayUs = s.minDelay,
                fifoMaxEventCount = s.fifoMaxEventCount,
                isWakeUp = s.isWakeUpSensor,
                isDynamic = s.isDynamicSensor,
                reportingMode = reportingMode
            )
        }.distinctBy { it.type } // Keep unique sensor types

        _availableSensors.value = items

        AppLogHub.log(
            toolId = "sensors",
            toolName = "Sensors Live",
            level = LogLevel.INFO,
            tag = "Discovery",
            message = "SENSORS HARDWARE [DISCOVERY] Found ${items.size} hardware sensors on device."
        )

        // Automatically start streaming primary sensors on device
        val primarySensors = items.filter { it.type in CORE_DEFAULT_SENSOR_TYPES }.map { it.type }.toSet()
        val typesToStart = if (primarySensors.isNotEmpty()) primarySensors else items.take(12).map { it.type }.toSet()
        startStreamingSensors(typesToStart)
    }

    private fun startBatcher() {
        if (batcherJob?.isActive == true) return
        batcherJob = scope.launch {
            while (isActive) {
                delay(33L) // 30 FPS UI state update throttle eliminates thousands of redundant recompositions
                if (hasNewReadings) {
                    hasNewReadings = false
                    _sensorReadings.value = HashMap(latestReadings)
                }
            }
        }
    }

    fun setUpdateInterval(interval: UpdateInterval) {
        try {
            _updateInterval.value = interval
            AppLogHub.log(
                toolId = "sensors",
                toolName = "Sensors Live",
                level = LogLevel.INFO,
                tag = "Sampling",
                message = "SENSORS RATE [INTERVAL CHANGE] Set sampling interval to ${interval.displayName}"
            )

            if (interval == UpdateInterval.PAUSED) {
                stopAllSensors()
            } else {
                // Re-register active sensors with appropriate sampling rate
                val currentActive = _activeSensorTypes.value
                if (currentActive.isNotEmpty()) {
                    stopAllSensors()
                    startStreamingSensors(currentActive)
                }
            }
        } catch (t: Throwable) {
            AppLogHub.log(
                toolId = "sensors",
                toolName = "Sensors Live",
                level = LogLevel.ERROR,
                tag = "RateChange",
                message = "Failed to switch sensor update interval to ${interval.displayName}: ${t.message ?: t.javaClass.simpleName}",
                throwable = t
            )
        }
    }

    fun startStreamingSensors(sensorTypes: Set<Int>) {
        val sm = sensorManager ?: return
        val interval = _updateInterval.value
        if (interval == UpdateInterval.PAUSED) return

        startBatcher()

        // Use SENSOR_DELAY_GAME (~20ms / 50Hz) for live mode to prevent native sensor hardware flooding
        val samplingRateUs = when (interval) {
            UpdateInterval.LIVE_FAST -> SensorManager.SENSOR_DELAY_GAME
            UpdateInterval.EVERY_1_SEC -> SensorManager.SENSOR_DELAY_NORMAL
            UpdateInterval.EVERY_2_SEC -> SensorManager.SENSOR_DELAY_UI
            UpdateInterval.EVERY_5_SEC -> SensorManager.SENSOR_DELAY_NORMAL
            UpdateInterval.PAUSED -> return
        }

        sensorTypes.forEach { type ->
            if (!sensorListeners.containsKey(type)) {
                val sensor = sm.getDefaultSensor(type)
                if (sensor != null) {
                    val listener = object : SensorEventListener {
                        override fun onSensorChanged(event: SensorEvent?) {
                            if (event == null) return
                            try {
                                val now = System.currentTimeMillis()
                                // Throttle live mode to ~50 FPS (20ms) max to ensure 60/120 FPS UI smoothness without frame drops
                                val minInterval = if (interval == UpdateInterval.LIVE_FAST) 20L else interval.delayMs

                                val lastTime = lastEmissionTimes[type] ?: 0L
                                if (minInterval <= 0L || now - lastTime >= minInterval) {
                                    lastEmissionTimes[type] = now
                                    val timeFormatted = timeFormat.format(Date(now))
                                    // Clone values array immediately to avoid thread-safety issues with Android's internal buffer reuse
                                    val safeValues = event.values.clone()
                                    val reading = SensorTelemetryFormatter.formatReading(
                                        type = type,
                                        values = safeValues,
                                        accuracy = event.accuracy,
                                        timeFormatted = timeFormatted
                                    )

                                    // Store in zero-allocation lock-free map; UI batcher will emit at smooth 30 FPS
                                    latestReadings[type] = reading
                                    hasNewReadings = true
                                }
                            } catch (t: Throwable) {
                                AppLogHub.log(
                                    toolId = "sensors",
                                    toolName = "Sensors Live",
                                    level = LogLevel.ERROR,
                                    tag = "SensorEvent",
                                    message = "CRASH / ERROR in sensor stream for type $type: ${t.message ?: t.javaClass.simpleName}",
                                    throwable = t
                                )
                            }
                        }

                        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                    }

                    try {
                        sm.registerListener(listener, sensor, samplingRateUs)
                        sensorListeners[type] = listener
                    } catch (t: Throwable) {
                        AppLogHub.log(
                            toolId = "sensors",
                            toolName = "Sensors Live",
                            level = LogLevel.ERROR,
                            tag = "RegisterListener",
                            message = "Failed to register sensor listener for type $type: ${t.message ?: t.javaClass.simpleName}",
                            throwable = t
                        )
                    }
                }
            }
        }

        _activeSensorTypes.value = sensorListeners.keys.toSet()
    }

    fun toggleSensor(type: Int) {
        val sm = sensorManager ?: return
        if (sensorListeners.containsKey(type)) {
            val listener = sensorListeners.remove(type)
            if (listener != null) {
                val sensor = sm.getDefaultSensor(type)
                sm.unregisterListener(listener, sensor)
            }
            lastEmissionTimes.remove(type)
        } else {
            startStreamingSensors(setOf(type))
        }
        _activeSensorTypes.value = sensorListeners.keys.toSet()
    }

    fun stopAllSensors() {
        batcherJob?.cancel()
        batcherJob = null
        val sm = sensorManager ?: return
        sensorListeners.forEach { (type, listener) ->
            val sensor = sm.getDefaultSensor(type)
            sm.unregisterListener(listener, sensor)
        }
        sensorListeners.clear()
        lastEmissionTimes.clear()
        _activeSensorTypes.value = emptySet()
    }

    fun resumeAllSensors() {
        val allTypes = _availableSensors.value.map { it.type }.toSet()
        if (_updateInterval.value == UpdateInterval.PAUSED) {
            _updateInterval.value = UpdateInterval.EVERY_1_SEC
        }
        startStreamingSensors(allTypes)
    }

    fun exportSnapshot(): String {
        val readings = _sensorReadings.value
        val sensors = _availableSensors.value
        val sb = StringBuilder()
        sb.appendLine("=== MOTHER OF ALL APPS - SENSOR TELEMETRY SNAPSHOT ===")
        sb.appendLine("Generated at: ${timeFormat.format(Date())}")
        sb.appendLine("Sampling Interval: ${_updateInterval.value.displayName}")
        sb.appendLine("Total Sensors Available: ${sensors.size}")
        sb.appendLine("Active Streaming Sensors: ${_activeSensorTypes.value.size}")
        sb.appendLine("-------------------------------------------------------")

        sensors.forEach { sensor ->
            val reading = readings[sensor.type]
            sb.appendLine("\n[${sensor.name.uppercase()}] (${sensor.category.displayName})")
            sb.appendLine("  Vendor: ${sensor.vendor} | Version: ${sensor.version} | Power: ${sensor.powerMa} mA")
            sb.appendLine("  Max Range: ${sensor.maxRange} | Resolution: ${sensor.resolution}")
            if (reading != null) {
                sb.appendLine("  Last Reading: ${reading.primaryDisplay} [Accuracy: ${reading.accuracyLabel}]")
                reading.formattedAxes.forEach { (axis, value) ->
                    sb.appendLine("    $axis: $value")
                }
                sb.appendLine("  Timestamp: ${reading.formattedTime}")
            } else {
                sb.appendLine("  Status: Waiting for event / Inactive")
            }
        }
        return sb.toString()
    }

    companion object {
        @Volatile var activeInstance: SensorsManager? = null

        val CORE_DEFAULT_SENSOR_TYPES = setOf(
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_MAGNETIC_FIELD,
            Sensor.TYPE_LIGHT,
            Sensor.TYPE_PRESSURE,
            Sensor.TYPE_PROXIMITY,
            Sensor.TYPE_GRAVITY,
            Sensor.TYPE_LINEAR_ACCELERATION,
            Sensor.TYPE_ROTATION_VECTOR,
            Sensor.TYPE_STEP_COUNTER,
            Sensor.TYPE_STEP_DETECTOR,
            Sensor.TYPE_AMBIENT_TEMPERATURE,
            Sensor.TYPE_RELATIVE_HUMIDITY
        )

        fun pauseActiveStreaming(): Int {
            val count = activeInstance?.sensorListeners?.size ?: 0
            activeInstance?.stopAllSensors()
            return count
        }
    }
}
