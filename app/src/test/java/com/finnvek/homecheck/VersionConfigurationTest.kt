package com.finnvek.homecheck

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionConfigurationTest {
    @Test
    fun versionMetadataMatchesBuildType() {
        assertTrue(BuildConfig.VERSION_CODE > 0)
        assertTrue(BuildConfig.VERSION_NAME.isNotBlank())
        assertEquals(BuildConfig.DEBUG, BuildConfig.VERSION_NAME.endsWith("-debug"))
    }
}
