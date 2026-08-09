package com.finnvek.homecheck.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object WarrantyRules {
    private const val EXPIRING_SOON_DAYS = 30L
    val REMINDER_DAYS = setOf(30, 7, 1)

    fun daysRemaining(
        expirationDate: LocalDate,
        today: LocalDate = LocalDate.now(),
    ): Long = ChronoUnit.DAYS.between(today, expirationDate)

    fun isExpiringSoon(
        expirationDate: LocalDate,
        today: LocalDate = LocalDate.now(),
    ): Boolean = daysRemaining(expirationDate, today) in 0..EXPIRING_SOON_DAYS

    fun shouldNotify(
        expirationDate: LocalDate,
        today: LocalDate = LocalDate.now(),
    ): Boolean = daysRemaining(expirationDate, today).toInt() in REMINDER_DAYS
}
