package com.finnvek.homecheck.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssetLimitPolicyTest {
    @Test fun `free user may create the first three assets`() {
        assertTrue(AssetLimitPolicy.canCreate(currentCount = 0, isPremium = false))
        assertTrue(AssetLimitPolicy.canCreate(currentCount = 1, isPremium = false))
        assertTrue(AssetLimitPolicy.canCreate(currentCount = 2, isPremium = false))
    }

    @Test fun `free user needs premium for a fourth asset`() {
        assertFalse(AssetLimitPolicy.canCreate(currentCount = 3, isPremium = false))
        assertFalse(AssetLimitPolicy.canCreate(currentCount = 8, isPremium = false))
    }

    @Test fun `premium never limits additional assets`() {
        assertTrue(AssetLimitPolicy.canCreate(currentCount = 3, isPremium = true))
        assertTrue(AssetLimitPolicy.canCreate(currentCount = 300, isPremium = true))
    }
}

