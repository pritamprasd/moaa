package dev.pritam.host.tool.sensors.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.pritam.host.logging.AppLogHub
import dev.pritam.host.logging.LogLevel
import dev.pritam.host.tool.sensors.manager.SensorsManager
import dev.pritam.host.tool.sensors.model.SensorCategory
import dev.pritam.host.tool.sensors.model.SensorInfoItem
import dev.pritam.host.tool.sensors.model.SensorValueReading
import dev.pritam.host.tool.sensors.model.UpdateInterval
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class SensorsStats(
    val totalCount: Int = 0,
    val activeCount: Int = 0,
    val readingsReceived: Int = 0,
    val currentInterval: UpdateInterval = UpdateInterval.EVERY_1_SEC
)

class SensorsViewModel(application: Application) : AndroidViewModel(application) {

    private val sensorsManager = SensorsManager(application.applicationContext)

    val availableSensors: StateFlow<List<SensorInfoItem>> = sensorsManager.availableSensors
    val sensorReadings: StateFlow<Map<Int, SensorValueReading>> = sensorsManager.sensorReadings
    val activeSensorTypes: StateFlow<Set<Int>> = sensorsManager.activeSensorTypes
    val updateInterval: StateFlow<UpdateInterval> = sensorsManager.updateInterval

    private val _selectedCategory = MutableStateFlow(SensorCategory.ALL)
    val selectedCategory: StateFlow<SensorCategory> = _selectedCategory

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _expandedSensorTypes = MutableStateFlow<Set<Int>>(emptySet())
    val expandedSensorTypes: StateFlow<Set<Int>> = _expandedSensorTypes

    val filteredSensors: StateFlow<List<SensorInfoItem>> = combine(
        availableSensors,
        selectedCategory,
        searchQuery
    ) { sensors, category, query ->
        sensors.filter { sensor ->
            val matchesCat = category == SensorCategory.ALL || sensor.category == category
            val matchesQuery = query.isBlank() ||
                    sensor.name.contains(query, ignoreCase = true) ||
                    sensor.vendor.contains(query, ignoreCase = true) ||
                    sensor.category.displayName.contains(query, ignoreCase = true)
            matchesCat && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats: StateFlow<SensorsStats> = combine(
        availableSensors,
        activeSensorTypes,
        sensorReadings,
        updateInterval
    ) { sensors, active, readings, interval ->
        SensorsStats(
            totalCount = sensors.size,
            activeCount = active.size,
            readingsReceived = readings.size,
            currentInterval = interval
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SensorsStats())

    fun setSelectedCategory(category: SensorCategory) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setUpdateInterval(interval: UpdateInterval) {
        sensorsManager.setUpdateInterval(interval)
    }

    fun toggleSensorActive(type: Int) {
        sensorsManager.toggleSensor(type)
    }

    fun toggleExpanded(type: Int) {
        val current = _expandedSensorTypes.value
        _expandedSensorTypes.value = if (current.contains(type)) {
            current - type
        } else {
            current + type
        }
    }

    fun pauseAll() {
        sensorsManager.setUpdateInterval(UpdateInterval.PAUSED)
    }

    fun resumeAll() {
        sensorsManager.resumeAllSensors()
    }

    fun exportTelemetry(context: Context) {
        val snapshot = sensorsManager.exportSnapshot()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Sensors Telemetry", snapshot)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Telemetry snapshot copied to clipboard!", Toast.LENGTH_SHORT).show()

        AppLogHub.log(
            toolId = "sensors",
            toolName = "Sensors",
            level = LogLevel.INFO,
            tag = "Export",
            message = "SENSORS TELEMETRY [EXPORT] Exported snapshot of ${availableSensors.value.size} sensors to clipboard"
        )
    }

    override fun onCleared() {
        super.onCleared()
        sensorsManager.stopAllSensors()
    }
}
