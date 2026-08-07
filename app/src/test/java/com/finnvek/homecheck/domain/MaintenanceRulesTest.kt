package com.finnvek.homecheck.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class MaintenanceRulesTest {
    private val today = LocalDate.of(2026, 8, 7)

    @Test fun `a date before today is overdue`() {
        assertEquals(MaintenanceStatus.OVERDUE, maintenanceStatus(today.minusDays(1), today))
    }

    @Test fun `today is due today`() {
        assertEquals(MaintenanceStatus.DUE_TODAY, maintenanceStatus(today, today))
    }

    @Test fun `the next seven days are due soon`() {
        assertEquals(MaintenanceStatus.DUE_SOON, maintenanceStatus(today.plusDays(1), today))
        assertEquals(MaintenanceStatus.DUE_SOON, maintenanceStatus(today.plusDays(7), today))
    }

    @Test fun `a date beyond seven days is upcoming`() {
        assertEquals(MaintenanceStatus.UPCOMING, maintenanceStatus(today.plusDays(8), today))
    }

    @Test fun `recurrence advances from actual completion date`() {
        val completion = LocalDate.of(2026, 1, 31)

        assertEquals(completion.plusDays(3), Recurrence(3, RecurrenceUnit.DAYS).nextDueDate(completion))
        assertEquals(completion.plusWeeks(2), Recurrence(2, RecurrenceUnit.WEEKS).nextDueDate(completion))
        assertEquals(completion.plusMonths(1), Recurrence(1, RecurrenceUnit.MONTHS).nextDueDate(completion))
        assertEquals(completion.plusYears(1), Recurrence(1, RecurrenceUnit.YEARS).nextDueDate(completion))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `recurrence interval must be positive`() {
        Recurrence(0, RecurrenceUnit.MONTHS)
    }
}

