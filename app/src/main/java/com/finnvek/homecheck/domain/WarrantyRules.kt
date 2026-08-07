package com.finnvek.homecheck.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object WarrantyRules {
    val REMINDER_DAYS = setOf(30, 7, 1)

    fun daysRemaining(expirationDate: LocalDate, today: LocalDate = LocalDate.now()): Long =
        ChronoUnit.DAYS.between(today, expirationDate)

    fun isExpiringSoon(expirationDate: LocalDate, today: LocalDate = LocalDate.now()): Boolean =
        daysRemaining(expirationDate, today) in 0..30

    fun shouldNotify(expirationDate: LocalDate, today: LocalDate = LocalDate.now()): Boolean =
        daysRemaining(expirationDate, today).toInt() in REMINDER_DAYS
}

