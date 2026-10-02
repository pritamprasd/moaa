package dev.pritam.host.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class LogRetentionPolicy(
    val displayName: String,
    val durationMs: Long,
    val description: String,
) {
    ONE_HOUR("1 Hour", 60 * 60 * 1000L, "Keep logs from the past 60 minutes"),
    ONE_DAY("1 Day", 24 * 60 * 60 * 1000L, "Keep logs from the past 24 hours"),
    ONE_WEEK("1 Week", 7 * 24 * 60 * 60 * 1000L, "Keep logs from the past 7 days"),
    ONE_MONTH("1 Month", 30L * 24 * 60 * 60 * 1000L, "Keep logs from the past 30 days"),
    ONE_YEAR("1 Year", 365L * 24 * 60 * 60 * 1000L, "Keep logs from the past 365 days"),
    KEEP_ALL("Keep All Logs", Long.MAX_VALUE, "Never automatically delete logs until buffer cap"),
}

/**
 * Global App Settings manager holding user configurations such as gallery layout and log retention.
 */
object AppSettingsManager {

    private val _galleryColumnCount = MutableStateFlow(2) // 1, 2, 3, or 4 columns
    val galleryColumnCount: StateFlow<Int> = _galleryColumnCount.asStateFlow()

    private val _isVibrationFeedbackEnabled = MutableStateFlow(true)
    val isVibrationFeedbackEnabled: StateFlow<Boolean> = _isVibrationFeedbackEnabled.asStateFlow()

    private val _logRetentionPolicy = MutableStateFlow(LogRetentionPolicy.ONE_DAY)
    val logRetentionPolicy: StateFlow<LogRetentionPolicy> = _logRetentionPolicy.asStateFlow()

    private val _dashboardPaddingDp = MutableStateFlow(18) // 8 to 32 dp
    val dashboardPaddingDp: StateFlow<Int> = _dashboardPaddingDp.asStateFlow()

    fun setGalleryColumnCount(count: Int) {
        if (count in 1..4) {
            _galleryColumnCount.value = count
        }
    }

    fun setDashboardPadding(paddingDp: Int) {
        _dashboardPaddingDp.value = paddingDp.coerceIn(8, 32)
    }

    fun setVibrationFeedback(enabled: Boolean) {
        _isVibrationFeedbackEnabled.value = enabled
    }

    fun setLogRetentionPolicy(policy: LogRetentionPolicy) {
        _logRetentionPolicy.value = policy
    }

    fun resetToDefaults() {
        _galleryColumnCount.value = 2
        _dashboardPaddingDp.value = 18
        _isVibrationFeedbackEnabled.value = true
        _logRetentionPolicy.value = LogRetentionPolicy.ONE_DAY
    }
}
