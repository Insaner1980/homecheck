package com.finnvek.homecheck.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeStatusTest {
    private val today = LocalDate.of(2026, 8, 7)

    @Test fun `overdue has priority over every calmer state`() {
        assertEquals(HomeAttentionStatus.OVERDUE, homeAttentionStatus(listOf(today.plusDays(2), today, today.minusDays(1)), today))
    }

    @Test fun `today has priority over upcoming work`() {
        assertEquals(HomeAttentionStatus.DUE_TODAY, homeAttentionStatus(listOf(today.plusDays(2), today), today))
    }

    @Test fun `next seven days are upcoming attention and empty is clear`() {
        assertEquals(HomeAttentionStatus.UPCOMING, homeAttentionStatus(listOf(today.plusDays(7)), today))
        assertEquals(HomeAttentionStatus.ALL_CLEAR, homeAttentionStatus(emptyList(), today))
    }
}

