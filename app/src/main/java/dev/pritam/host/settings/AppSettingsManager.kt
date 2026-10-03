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

enum class AccentPalette(
    val id: String,
    val displayName: String,
    val description: String,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val borderTint: Color,
) {
    LINEAR_CYAN(
        id = "linear_cyan",
        displayName = "Linear Ice",
        description = "Crisp ice blue & soft indigo inspired by Linear & VS Code",
        primary = Color(0xFF38BDF8),
        secondary = Color(0xFF818CF8),
        tertiary = Color(0xFF34D399),
        borderTint = Color(0xFF1E293B)
    ),
    NOTHING_AMBER(
        id = "nothing_amber",
        displayName = "Industrial Amber",
        description = "High-contrast monochrome & warm amber inspired by Nothing OS",
        primary = Color(0xFFF59E0B),
        secondary = Color(0xFFF4F4F6),
        tertiary = Color(0xFFE2E8F0),
        borderTint = Color(0xFF262626)
    ),
    NORDIC_EMERALD(
        id = "nordic_emerald",
        displayName = "Nordic Emerald",
        description = "Calming organic terminal look with high legibility emerald & teal",
        primary = Color(0xFF10B981),
        secondary = Color(0xFF06B6D4),
        tertiary = Color(0xFFA7F3D0),
        borderTint = Color(0xFF132E24)
    ),
    TOKYO_VIOLET(
        id = "tokyo_violet",
        displayName = "Tokyo Dusk",
        description = "Atmospheric developer tool aesthetic with subtle electric violet",
        primary = Color(0xFFA855F7),
        secondary = Color(0xFFEC4899),
        tertiary = Color(0xFF818CF8),
        borderTint = Color(0xFF2A1B3D)
    ),
    SOLARIS_COPPER(
        id = "solaris_copper",
        displayName = "Solaris Copper",
        description = "Dense hardware monitor feel with burnished copper & crimson",
        primary = Color(0xFFFB923C),
        secondary = Color(0xFFF43F5E),
        tertiary = Color(0xFFFDE047),
        borderTint = Color(0xFF331E17)
    );

    companion object {
        fun fromId(id: String?): AccentPalette =
            entries.find { it.id.equals(id, ignoreCase = true) || it.name.equals(id, ignoreCase = true) }
                ?: LINEAR_CYAN
    }
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
    private const val KEY_ACCENT_PALETTE = "app_accent_palette"
    private const val KEY_SECTION_GENERAL = "section_general_expanded"
    private const val KEY_SECTION_AI = "section_ai_expanded"
    private const val KEY_SECTION_AUTOMATION = "section_automation_expanded"
    private const val KEY_SECTION_DIAGNOSTICS = "section_diagnostics_expanded"
    private const val KEY_SECTION_ABOUT = "section_about_expanded"
    private const val KEY_TOOL_ORDER = "tool_order_list"

    private var prefs: SharedPreferences? = null

    private val _accentPalette = MutableStateFlow(AccentPalette.LINEAR_CYAN)
    val accentPalette: StateFlow<AccentPalette> = _accentPalette.asStateFlow()

    private val _toolOrder = MutableStateFlow<List<String>>(emptyList())
    val toolOrder: StateFlow<List<String>> = _toolOrder.asStateFlow()

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

    // ── Collapsible Section States (Collapsed by default: false) ───────────
    private val _sectionGeneralExpanded = MutableStateFlow(false)
    val sectionGeneralExpanded: StateFlow<Boolean> = _sectionGeneralExpanded.asStateFlow()

    private val _sectionAiExpanded = MutableStateFlow(false)
    val sectionAiExpanded: StateFlow<Boolean> = _sectionAiExpanded.asStateFlow()

    private val _sectionAutomationExpanded = MutableStateFlow(false)
    val sectionAutomationExpanded: StateFlow<Boolean> = _sectionAutomationExpanded.asStateFlow()

    private val _sectionDiagnosticsExpanded = MutableStateFlow(false)
    val sectionDiagnosticsExpanded: StateFlow<Boolean> = _sectionDiagnosticsExpanded.asStateFlow()

    private val _sectionAboutExpanded = MutableStateFlow(false)
    val sectionAboutExpanded: StateFlow<Boolean> = _sectionAboutExpanded.asStateFlow()

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
            val paletteRaw = p.getString(KEY_ACCENT_PALETTE, AccentPalette.LINEAR_CYAN.id)
            _accentPalette.value = AccentPalette.fromId(paletteRaw)
            _sectionGeneralExpanded.value = p.getBoolean(KEY_SECTION_GENERAL, false)
            _sectionAiExpanded.value = p.getBoolean(KEY_SECTION_AI, false)
            _sectionAutomationExpanded.value = p.getBoolean(KEY_SECTION_AUTOMATION, false)
            _sectionDiagnosticsExpanded.value = p.getBoolean(KEY_SECTION_DIAGNOSTICS, false)
            _sectionAboutExpanded.value = p.getBoolean(KEY_SECTION_ABOUT, false)

            val orderRaw = p.getString(KEY_TOOL_ORDER, null)
            if (!orderRaw.isNullOrBlank()) {
                _toolOrder.value = orderRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            }
        }
    }

    /**
     * Compute dialog surface color based on the configured opacity percent.
     * Matte dark obsidian tone ensures text and dialog controls are crisp and legible
     * without harsh glare or background bleed-through.
     */
    fun getDialogSurfaceColor(opacityPercent: Int = _dialogOpacityPercent.value): Color {
        val clamped = opacityPercent.coerceIn(50, 100)
        return Color(0xFF14161F).copy(alpha = clamped / 100f)
    }

    fun setAccentPalette(palette: AccentPalette) {
        _accentPalette.value = palette
        prefs?.edit()?.putString(KEY_ACCENT_PALETTE, palette.id)?.apply()
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

    fun setSectionGeneralExpanded(expanded: Boolean) {
        _sectionGeneralExpanded.value = expanded
        prefs?.edit()?.putBoolean(KEY_SECTION_GENERAL, expanded)?.apply()
    }

    fun setSectionAiExpanded(expanded: Boolean) {
        _sectionAiExpanded.value = expanded
        prefs?.edit()?.putBoolean(KEY_SECTION_AI, expanded)?.apply()
    }

    fun setSectionAutomationExpanded(expanded: Boolean) {
        _sectionAutomationExpanded.value = expanded
        prefs?.edit()?.putBoolean(KEY_SECTION_AUTOMATION, expanded)?.apply()
    }

    fun setSectionDiagnosticsExpanded(expanded: Boolean) {
        _sectionDiagnosticsExpanded.value = expanded
        prefs?.edit()?.putBoolean(KEY_SECTION_DIAGNOSTICS, expanded)?.apply()
    }

    fun setSectionAboutExpanded(expanded: Boolean) {
        _sectionAboutExpanded.value = expanded
        prefs?.edit()?.putBoolean(KEY_SECTION_ABOUT, expanded)?.apply()
    }

    fun setToolOrder(order: List<String>) {
        _toolOrder.value = order
        prefs?.edit()?.putString(KEY_TOOL_ORDER, order.joinToString(","))?.apply()
    }

    fun resetToolOrder() {
        _toolOrder.value = emptyList()
        prefs?.edit()?.remove(KEY_TOOL_ORDER)?.apply()
    }

    fun resetToDefaults() {
        _galleryColumnCount.value = 2
        _dashboardPaddingDp.value = 18
        _dialogOpacityPercent.value = 94
        _isVibrationFeedbackEnabled.value = true
        _logRetentionPolicy.value = LogRetentionPolicy.ONE_DAY
        _sectionGeneralExpanded.value = false
        _sectionAiExpanded.value = false
        _sectionAutomationExpanded.value = false
        _sectionDiagnosticsExpanded.value = false
        _sectionAboutExpanded.value = false
        _accentPalette.value = AccentPalette.LINEAR_CYAN

        prefs?.edit()
            ?.putInt(KEY_GALLERY_COLUMNS, 2)
            ?.putInt(KEY_DASHBOARD_PADDING, 18)
            ?.putInt(KEY_DIALOG_OPACITY, 94)
            ?.putBoolean(KEY_VIBRATION, true)
            ?.putString(KEY_LOG_RETENTION, LogRetentionPolicy.ONE_DAY.name)
            ?.putString(KEY_ACCENT_PALETTE, AccentPalette.LINEAR_CYAN.id)
            ?.putBoolean(KEY_SECTION_GENERAL, false)
            ?.putBoolean(KEY_SECTION_AI, false)
            ?.putBoolean(KEY_SECTION_AUTOMATION, false)
            ?.putBoolean(KEY_SECTION_DIAGNOSTICS, false)
            ?.putBoolean(KEY_SECTION_ABOUT, false)
            ?.apply()
    }
}

