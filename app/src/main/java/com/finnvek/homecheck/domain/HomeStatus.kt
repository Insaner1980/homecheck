package com.finnvek.homecheck.domain

import java.time.LocalDate

enum class HomeAttentionStatus { ALL_CLEAR, UPCOMING, DUE_TODAY, OVERDUE }

fun homeAttentionStatus(dueDates: List<LocalDate>, today: LocalDate = LocalDate.now()): HomeAttentionStatus {
    val statuses = dueDates.map { maintenanceStatus(it, today) }
    return when {
        MaintenanceStatus.OVERDUE in statuses -> HomeAttentionStatus.OVERDUE
        MaintenanceStatus.DUE_TODAY in statuses -> HomeAttentionStatus.DUE_TODAY
        MaintenanceStatus.DUE_SOON in statuses -> HomeAttentionStatus.UPCOMING
        else -> HomeAttentionStatus.ALL_CLEAR
    }
}

