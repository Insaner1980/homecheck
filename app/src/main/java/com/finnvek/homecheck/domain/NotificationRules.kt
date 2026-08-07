package com.finnvek.homecheck.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object MaintenanceNotificationRules {
    fun shouldNotify(dueDate: LocalDate, today: LocalDate = LocalDate.now()): Boolean {
        val overdueDays = ChronoUnit.DAYS.between(dueDate, today)
        return overdueDays == 0L || overdueDays > 0 && overdueDays % 7L == 0L
    }
}

