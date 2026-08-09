package com.finnvek.homecheck.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CompletionPlanTest {
    private val completionDate = LocalDate.of(2026, 8, 7)

    @Test fun `recurring completion keeps task active and advances from completion`() {
        val plan =
            MaintenanceCompletionPlan.create(
                taskId = "task-1",
                assetId = "asset-1",
                title = "Clean filter",
                recurrence = Recurrence(3, RecurrenceUnit.MONTHS),
                completedOn = completionDate,
            )

        assertFalse(plan.removeActiveTask)
        assertEquals(LocalDate.of(2026, 11, 7), plan.nextDueDate)
        assertEquals("Clean filter", plan.historyTitle)
    }

    @Test fun `one-time completion removes active task but preserves history snapshot`() {
        val plan =
            MaintenanceCompletionPlan.create(
                taskId = "task-1",
                assetId = "asset-1",
                title = "Professional service",
                recurrence = null,
                completedOn = completionDate,
            )

        assertTrue(plan.removeActiveTask)
        assertNull(plan.nextDueDate)
        assertEquals("Professional service", plan.historyTitle)
    }
}
