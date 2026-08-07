package com.finnvek.homecheck.billing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumEntitlementTest {
    @Test fun `only the lifetime product grants premium`() {
        assertTrue(PremiumEntitlement.fromOwnedProductIds(setOf(PremiumEntitlement.PRODUCT_ID)).isPremium)
        assertFalse(PremiumEntitlement.fromOwnedProductIds(setOf("another_product")).isPremium)
        assertFalse(PremiumEntitlement.fromOwnedProductIds(emptySet()).isPremium)
    }
}
