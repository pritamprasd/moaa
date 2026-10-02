package dev.pritam.host.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppSettingsTest {

    @Before
    fun setUp() {
        AppSettingsManager.resetToDefaults()
    }

    @Test
    fun testDefaultSettings() {
        assertEquals(2, AppSettingsManager.galleryColumnCount.value)
        assertEquals(18, AppSettingsManager.dashboardPaddingDp.value)
        assertTrue(AppSettingsManager.isVibrationFeedbackEnabled.value)
        assertEquals(LogRetentionPolicy.ONE_DAY, AppSettingsManager.logRetentionPolicy.value)
    }

    @Test
    fun testSetDashboardPadding() {
        AppSettingsManager.setDashboardPadding(10)
        assertEquals(10, AppSettingsManager.dashboardPaddingDp.value)

        AppSettingsManager.setDashboardPadding(28)
        assertEquals(28, AppSettingsManager.dashboardPaddingDp.value)

        // Range coercion: below 8 clamped to 8, above 32 clamped to 32
        AppSettingsManager.setDashboardPadding(4)
        assertEquals(8, AppSettingsManager.dashboardPaddingDp.value)

        AppSettingsManager.setDashboardPadding(50)
        assertEquals(32, AppSettingsManager.dashboardPaddingDp.value)
    }

    @Test
    fun testResetToDefaults() {
        AppSettingsManager.setDashboardPadding(30)
        AppSettingsManager.setGalleryColumnCount(4)
        assertEquals(30, AppSettingsManager.dashboardPaddingDp.value)
        assertEquals(4, AppSettingsManager.galleryColumnCount.value)

        AppSettingsManager.resetToDefaults()
        assertEquals(18, AppSettingsManager.dashboardPaddingDp.value)
        assertEquals(2, AppSettingsManager.galleryColumnCount.value)
    }
}
