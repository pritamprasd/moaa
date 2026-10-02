package dev.pritam.host.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
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
 * Global App Settings manager holding user configurations such as gallery layout,
 * dashboard padding, dialog opacity, and log retention.
 * Persists all values to SharedPreferences so settings survive app restarts and redeployments.
 */
object AppSettingsManager {

    private const val PREFS_NAME = "app_settings_prefs"
    private const val KEY_GALLERY_COLUMNS = "gallery_column_count"
    private const val KEY_DASHBOARD_PADDING = "dashboard_padding_dp"
    private const val KEY_DIALOG_OPACITY = "dialog_opacity_percent"
    private const val KEY_VIBRATION = "vibration_feedback"
    private const val KEY_LOG_RETENTION = "log_retention_policy"

    private var prefs: SharedPreferences? = null

    private val _galleryColumnCount = MutableStateFlow(2) // 1, 2, 3, or 4 columns
    val galleryColumnCount: StateFlow<Int> = _galleryColumnCount.asStateFlow()

    private val _dashboardPaddingDp = MutableStateFlow(18) // 8 to 32 dp
    val dashboardPaddingDp: StateFlow<Int> = _dashboardPaddingDp.asStateFlow()

    private val _dialogOpacityPercent = MutableStateFlow(94) // 50 to 100% opacity, default 94%
    val dialogOpacityPercent: StateFlow<Int> = _dialogOpacityPercent.asStateFlow()

    private val _isVibrationFeedbackEnabled = MutableStateFlow(true)
    val isVibrationFeedbackEnabled: StateFlow<Boolean> = _isVibrationFeedbackEnabled.asStateFlow()

    private val _logRetentionPolicy = MutableStateFlow(LogRetentionPolicy.ONE_DAY)
    val logRetentionPolicy: StateFlow<LogRetentionPolicy> = _logRetentionPolicy.asStateFlow()

    fun init(context: Context) {
        if (prefs == null) {
            val p = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs = p
            _galleryColumnCount.value = p.getInt(KEY_GALLERY_COLUMNS, 2).coerceIn(1, 4)
            _dashboardPaddingDp.value = p.getInt(KEY_DASHBOARD_PADDING, 18).coerceIn(8, 32)
            _dialogOpacityPercent.value = p.getInt(KEY_DIALOG_OPACITY, 94).coerceIn(50, 100)
            _isVibrationFeedbackEnabled.value = p.getBoolean(KEY_VIBRATION, true)
            val policyName = p.getString(KEY_LOG_RETENTION, LogRetentionPolicy.ONE_DAY.name)
            _logRetentionPolicy.value = try {
                LogRetentionPolicy.valueOf(policyName ?: LogRetentionPolicy.ONE_DAY.name)
            } catch (e: Exception) {
                LogRetentionPolicy.ONE_DAY
            }
        }
    }

    /**
     * Compute dialog surface color based on the configured opacity percent.
     * High default opacity (~94%) ensures text and dialog controls are crisp and legible
     * without background bleed-through.
     */
    fun getDialogSurfaceColor(opacityPercent: Int = _dialogOpacityPercent.value): Color {
        val clamped = opacityPercent.coerceIn(50, 100)
        return Color(0xFF0F172A).copy(alpha = clamped / 100f)
    }

    fun setGalleryColumnCount(count: Int) {
        if (count in 1..4) {
            _galleryColumnCount.value = count
            prefs?.edit()?.putInt(KEY_GALLERY_COLUMNS, count)?.apply()
        }
    }

    fun setDashboardPadding(paddingDp: Int) {
        val clamped = paddingDp.coerceIn(8, 32)
        _dashboardPaddingDp.value = clamped
        prefs?.edit()?.putInt(KEY_DASHBOARD_PADDING, clamped)?.apply()
    }

    fun setDialogOpacity(opacityPercent: Int) {
        val clamped = opacityPercent.coerceIn(50, 100)
        _dialogOpacityPercent.value = clamped
        prefs?.edit()?.putInt(KEY_DIALOG_OPACITY, clamped)?.apply()
    }

    fun setVibrationFeedback(enabled: Boolean) {
        _isVibrationFeedbackEnabled.value = enabled
        prefs?.edit()?.putBoolean(KEY_VIBRATION, enabled)?.apply()
    }

    fun setLogRetentionPolicy(policy: LogRetentionPolicy) {
        _logRetentionPolicy.value = policy
        prefs?.edit()?.putString(KEY_LOG_RETENTION, policy.name)?.apply()
    }

    fun resetToDefaults() {
        _galleryColumnCount.value = 2
        _dashboardPaddingDp.value = 18
        _dialogOpacityPercent.value = 94
        _isVibrationFeedbackEnabled.value = true
        _logRetentionPolicy.value = LogRetentionPolicy.ONE_DAY

        prefs?.edit()
            ?.putInt(KEY_GALLERY_COLUMNS, 2)
            ?.putInt(KEY_DASHBOARD_PADDING, 18)
            ?.putInt(KEY_DIALOG_OPACITY, 94)
            ?.putBoolean(KEY_VIBRATION, true)
            ?.putString(KEY_LOG_RETENTION, LogRetentionPolicy.ONE_DAY.name)
            ?.apply()
    }
}
