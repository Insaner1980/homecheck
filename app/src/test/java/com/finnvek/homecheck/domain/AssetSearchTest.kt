package com.finnvek.homecheck.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssetSearchTest {
    private val asset =
        AssetSearchDocument(
            name = "Bosch dishwasher",
            manufacturer = "Bosch",
            modelNumber = "SMV6ZCX10E",
            serialNumber = "FD 0203",
            location = "Kitchen",
            category = "Appliance",
        )

    @Test fun `search matches all useful asset fields without case sensitivity`() {
        assertTrue(asset.matches("dishWASHER"))
        assertTrue(asset.matches("bosch"))
        assertTrue(asset.matches("smv6"))
        assertTrue(asset.matches("0203"))
        assertTrue(asset.matches("kitchen"))
        assertTrue(asset.matches("appliance"))
    }

    @Test fun `blank query matches and unrelated query does not`() {
        assertTrue(asset.matches("  "))
        assertFalse(asset.matches("heat pump"))
    }
}
