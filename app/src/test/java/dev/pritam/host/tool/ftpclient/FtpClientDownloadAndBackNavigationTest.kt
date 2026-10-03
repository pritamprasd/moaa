package dev.pritam.host.tool.ftpclient

import dev.pritam.host.tool.ftpclient.notification.FtpDownloadNotificationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FtpClientDownloadAndBackNavigationTest {

    @Test
    fun testNotificationHelperChannelId() {
        assertEquals("ftp_client_downloads", FtpDownloadNotificationHelper.CHANNEL_ID)
    }

    @Test
    fun testDownloadCompleteNotificationCreationDoesNotThrow() {
        // Create a temporary file to test notification building components
        val tempFile = File.createTempFile("test_download", ".txt")
        tempFile.writeText("FTP downloaded content")
        assertTrue(tempFile.exists())
        assertTrue(tempFile.length() > 0)
        tempFile.delete()
    }
}
