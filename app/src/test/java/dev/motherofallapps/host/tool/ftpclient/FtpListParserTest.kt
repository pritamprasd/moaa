package dev.motherofallapps.host.tool.ftpclient

import dev.motherofallapps.host.tool.ftpclient.engine.FtpListParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FtpListParserTest {

    @Test
    fun testParseUnixDirectory() {
        val line = "drwxr-xr-x 2 user group 4096 Oct 02 12:00 my_folder"
        val parsed = FtpListParser.parseLine(line, "/home/user")
        assertNotNull(parsed)
        assertEquals("my_folder", parsed?.name)
        assertEquals("/home/user/my_folder", parsed?.path)
        assertTrue(parsed?.isDirectory == true)
        assertEquals(4096L, parsed?.sizeBytes)
        assertEquals("Folder", parsed?.formattedSize)
    }

    @Test
    fun testParseUnixFile() {
        val line = "-rw-r--r-- 1 user group 1048576 Oct 02 12:00 sample.mp4"
        val parsed = FtpListParser.parseLine(line, "/media")
        assertNotNull(parsed)
        assertEquals("sample.mp4", parsed?.name)
        assertEquals("/media/sample.mp4", parsed?.path)
        assertFalse(parsed?.isDirectory == true)
        assertEquals(1048576L, parsed?.sizeBytes)
        assertEquals("1.00 MB", parsed?.formattedSize)
        assertEquals("mp4", parsed?.fileExtension)
    }

    @Test
    fun testParseMlsdDirectory() {
        val line = "type=dir;size=0;modify=20231002120000; downloads"
        val parsed = FtpListParser.parseLine(line, "/")
        assertNotNull(parsed)
        assertEquals("downloads", parsed?.name)
        assertEquals("/downloads", parsed?.path)
        assertTrue(parsed?.isDirectory == true)
    }

    @Test
    fun testParseMlsdFile() {
        val line = "type=file;size=5242880;modify=20231002120000; archive.zip"
        val parsed = FtpListParser.parseLine(line, "/pub")
        assertNotNull(parsed)
        assertEquals("archive.zip", parsed?.name)
        assertEquals("/pub/archive.zip", parsed?.path)
        assertFalse(parsed?.isDirectory == true)
        assertEquals(5242880L, parsed?.sizeBytes)
        assertEquals("5.00 MB", parsed?.formattedSize)
        assertEquals("zip", parsed?.fileExtension)
    }

    @Test
    fun testParseDosDirectoryAndFile() {
        val dirLine = "04-12-23  09:30PM       <DIR>          BackupFolder"
        val parsedDir = FtpListParser.parseLine(dirLine, "/c")
        assertNotNull(parsedDir)
        assertEquals("BackupFolder", parsedDir?.name)
        assertTrue(parsedDir?.isDirectory == true)

        val fileLine = "04-12-23  09:35PM             2048  config.json"
        val parsedFile = FtpListParser.parseLine(fileLine, "/c")
        assertNotNull(parsedFile)
        assertEquals("config.json", parsedFile?.name)
        assertFalse(parsedFile?.isDirectory == true)
        assertEquals(2048L, parsedFile?.sizeBytes)
        assertEquals("2.0 KB", parsedFile?.formattedSize)
    }

    @Test
    fun testIgnoreDotAndTotalEntries() {
        assertNull(FtpListParser.parseLine("total 128", "/"))
        assertNull(FtpListParser.parseLine(".", "/"))
        assertNull(FtpListParser.parseLine("..", "/"))
        assertNull(FtpListParser.parseLine("", "/"))
    }

    @Test
    fun testBuildFullPath() {
        assertEquals("/var/log/app.log", FtpListParser.buildFullPath("/var/log", "app.log"))
        assertEquals("/var/log/app.log", FtpListParser.buildFullPath("/var/log/", "app.log"))
        assertEquals("/app.log", FtpListParser.buildFullPath("/", "app.log"))
    }
}
