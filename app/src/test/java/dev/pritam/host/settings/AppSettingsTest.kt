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
        assertEquals(94, AppSettingsManager.dialogOpacityPercent.value)
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
    fun testSetDialogOpacity() {
        AppSettingsManager.setDialogOpacity(75)
        assertEquals(75, AppSettingsManager.dialogOpacityPercent.value)

        AppSettingsManager.setDialogOpacity(100)
        assertEquals(100, AppSettingsManager.dialogOpacityPercent.value)

        // Range coercion: below 50 clamped to 50, above 100 clamped to 100
        AppSettingsManager.setDialogOpacity(20)
        assertEquals(50, AppSettingsManager.dialogOpacityPercent.value)

        AppSettingsManager.setDialogOpacity(150)
        assertEquals(100, AppSettingsManager.dialogOpacityPercent.value)
    }

    @Test
    fun testGetDialogSurfaceColor() {
        val color94 = AppSettingsManager.getDialogSurfaceColor(94)
        assertEquals(0.94f, color94.alpha, 0.01f)

        val color100 = AppSettingsManager.getDialogSurfaceColor(100)
        assertEquals(1.0f, color100.alpha, 0.01f)

        val color50 = AppSettingsManager.getDialogSurfaceColor(50)
        assertEquals(0.50f, color50.alpha, 0.01f)
    }

    @Test
    fun testResetToDefaults() {
        AppSettingsManager.setDashboardPadding(30)
        AppSettingsManager.setGalleryColumnCount(4)
        AppSettingsManager.setDialogOpacity(60)
        assertEquals(30, AppSettingsManager.dashboardPaddingDp.value)
        assertEquals(4, AppSettingsManager.galleryColumnCount.value)
        assertEquals(60, AppSettingsManager.dialogOpacityPercent.value)

        AppSettingsManager.resetToDefaults()
        assertEquals(18, AppSettingsManager.dashboardPaddingDp.value)
        assertEquals(2, AppSettingsManager.galleryColumnCount.value)
        assertEquals(94, AppSettingsManager.dialogOpacityPercent.value)
        assertTrue(AppSettingsManager.toolOrder.value.isEmpty())
    }

    @Test
    fun testToolOrderPersistence() {
        assertTrue(AppSettingsManager.toolOrder.value.isEmpty())
        val customOrder = listOf("terminal", "net-topology", "ftp-server")
        AppSettingsManager.setToolOrder(customOrder)
        assertEquals(customOrder, AppSettingsManager.toolOrder.value)

        AppSettingsManager.resetToolOrder()
        assertTrue(AppSettingsManager.toolOrder.value.isEmpty())
    }
}

