package dev.motherofallapps.host.ftp

import dev.motherofallapps.host.ftp.server.FtpFileSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FtpFileSystemTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testVirtualPathNormalization() {
        assertEquals("/", FtpFileSystem.normalizeVirtualPath("/"))
        assertEquals("/sub", FtpFileSystem.normalizeVirtualPath("/sub"))
        assertEquals("/sub/nested", FtpFileSystem.normalizeVirtualPath("/sub/nested/"))
        assertEquals("/sub", FtpFileSystem.normalizeVirtualPath("/sub/nested/.."))
        assertEquals("/", FtpFileSystem.normalizeVirtualPath("/../../../"))
        assertEquals("/foo", FtpFileSystem.normalizeVirtualPath("/../../../foo"))
    }

    @Test
    fun testChrootSandboxingKeepsAccessInsideRoot() {
        val root = tempFolder.newFolder("ftp_root")
        val secretOutside = tempFolder.newFile("secret.txt").apply { writeText("outside") }
        val fs = FtpFileSystem(root)

        // Attempting to resolve "../secret.txt" is normalized and chrooted to root/secret.txt
        val resolved = fs.resolve("/", "../secret.txt")
        assertNotNull(resolved)
        // Verify it resolves to inside the root folder, NOT the parent's secret.txt
        assertEquals(File(root, "secret.txt").canonicalPath, resolved?.canonicalPath)
        assertTrue(resolved!!.canonicalPath.startsWith(root.canonicalPath))
        // Verify the file inside root does NOT match the outside file
        assertEquals(false, resolved.exists())

        // Inside root file exists
        val insideFile = File(root, "allowed.txt").apply { writeText("hello") }
        val resolvedInside = fs.resolve("/", "allowed.txt")
        assertNotNull(resolvedInside)
        assertEquals(insideFile.canonicalPath, resolvedInside?.canonicalPath)
        assertEquals(true, resolvedInside?.exists())
    }

    @Test
    fun testToVirtualPathConversion() {
        val root = tempFolder.newFolder("ftp_root")
        val subDir = File(root, "photos").apply { mkdirs() }
        val photoFile = File(subDir, "pic.jpg").apply { writeText("data") }
        val fs = FtpFileSystem(root)

        assertEquals("/", fs.toVirtualPath(root))
        assertEquals("/photos", fs.toVirtualPath(subDir))
        assertEquals("/photos/pic.jpg", fs.toVirtualPath(photoFile))
    }
}
