package com.finnvek.homecheck.backup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupPathValidatorTest {
    @Test fun `normal manifest and attachment paths are accepted`() {
        assertTrue(BackupPathValidator.isSafe("manifest.json"))
        assertTrue(BackupPathValidator.isSafe("attachments/asset-photo.jpg"))
    }

    @Test fun `absolute traversal and ambiguous paths are rejected`() {
        assertFalse(BackupPathValidator.isSafe("../secrets.txt"))
        assertFalse(BackupPathValidator.isSafe("attachments/../../secrets.txt"))
        assertFalse(BackupPathValidator.isSafe("/absolute/file.pdf"))
        assertFalse(BackupPathValidator.isSafe("C:\\absolute\\file.pdf"))
        assertFalse(BackupPathValidator.isSafe("attachments//file.pdf"))
        assertFalse(BackupPathValidator.isSafe("attachments/./file.pdf"))
    }
}
