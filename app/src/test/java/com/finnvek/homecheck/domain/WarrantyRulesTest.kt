package com.finnvek.homecheck.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WarrantyRulesTest {
    private val today = LocalDate.of(2026, 8, 7)

    @Test fun `warranty is expiring soon within thirty days`() {
        assertTrue(WarrantyRules.isExpiringSoon(today.plusDays(30), today))
        assertTrue(WarrantyRules.isExpiringSoon(today, today))
        assertFalse(WarrantyRules.isExpiringSoon(today.plusDays(31), today))
        assertFalse(WarrantyRules.isExpiringSoon(today.minusDays(1), today))
    }

    @Test fun `warranty notification milestones are exact`() {
        val expiration = today.plusDays(30)
        assertEquals(setOf(30, 7, 1), WarrantyRules.REMINDER_DAYS)
        assertTrue(WarrantyRules.shouldNotify(expiration, today))
        assertTrue(WarrantyRules.shouldNotify(expiration, expiration.minusDays(7)))
        assertTrue(WarrantyRules.shouldNotify(expiration, expiration.minusDays(1)))
        assertFalse(WarrantyRules.shouldNotify(expiration, expiration.minusDays(2)))
    }
}

