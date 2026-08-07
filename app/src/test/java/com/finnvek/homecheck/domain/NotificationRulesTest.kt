package com.finnvek.homecheck.domain

import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationRulesTest {
    private val today = LocalDate.of(2026, 8, 7)

    @Test fun `maintenance notifies when due and then every seven overdue days`() {
        assertTrue(MaintenanceNotificationRules.shouldNotify(today, today))
        assertTrue(MaintenanceNotificationRules.shouldNotify(today.minusDays(7), today))
        assertTrue(MaintenanceNotificationRules.shouldNotify(today.minusDays(14), today))
    }

    @Test fun `maintenance does not notify early or every overdue day`() {
        assertFalse(MaintenanceNotificationRules.shouldNotify(today.plusDays(1), today))
        assertFalse(MaintenanceNotificationRules.shouldNotify(today.minusDays(1), today))
        assertFalse(MaintenanceNotificationRules.shouldNotify(today.minusDays(8), today))
    }
}

