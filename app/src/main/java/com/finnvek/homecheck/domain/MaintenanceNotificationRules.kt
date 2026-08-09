package com.finnvek.homecheck.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object MaintenanceNotificationRules {
    private const val OVERDUE_REMINDER_INTERVAL_DAYS = 7L

    fun shouldNotify(
        dueDate: LocalDate,
        today: LocalDate = LocalDate.now(),
    ): Boolean {
        val overdueDays = ChronoUnit.DAYS.between(dueDate, today)
        return overdueDays == 0L || (overdueDays > 0 && overdueDays % OVERDUE_REMINDER_INTERVAL_DAYS == 0L)
    }
}
