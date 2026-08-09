package com.finnvek.homecheck.domain

import java.time.LocalDate

private const val DUE_SOON_DAYS = 7L

enum class MaintenanceStatus {
    OVERDUE,
    DUE_TODAY,
    DUE_SOON,
    UPCOMING,
}

fun maintenanceStatus(
    dueDate: LocalDate,
    today: LocalDate = LocalDate.now(),
): MaintenanceStatus =
    when {
        dueDate.isBefore(today) -> MaintenanceStatus.OVERDUE
        dueDate == today -> MaintenanceStatus.DUE_TODAY
        !dueDate.isAfter(today.plusDays(DUE_SOON_DAYS)) -> MaintenanceStatus.DUE_SOON
        else -> MaintenanceStatus.UPCOMING
    }

enum class RecurrenceUnit {
    DAYS,
    WEEKS,
    MONTHS,
    YEARS,
}

data class Recurrence(
    val interval: Int,
    val unit: RecurrenceUnit,
) {
    init {
        require(interval > 0) { "Recurrence interval must be positive" }
    }

    fun nextDueDate(completedOn: LocalDate): LocalDate =
        when (unit) {
            RecurrenceUnit.DAYS -> completedOn.plusDays(interval.toLong())
            RecurrenceUnit.WEEKS -> completedOn.plusWeeks(interval.toLong())
            RecurrenceUnit.MONTHS -> completedOn.plusMonths(interval.toLong())
            RecurrenceUnit.YEARS -> completedOn.plusYears(interval.toLong())
        }
}

data class MaintenanceCompletionPlan(
    val taskId: String,
    val assetId: String,
    val historyTitle: String,
    val completedOn: LocalDate,
    val removeActiveTask: Boolean,
    val nextDueDate: LocalDate?,
) {
    companion object {
        fun create(
            taskId: String,
            assetId: String,
            title: String,
            recurrence: Recurrence?,
            completedOn: LocalDate,
        ) = MaintenanceCompletionPlan(
            taskId = taskId,
            assetId = assetId,
            historyTitle = title,
            completedOn = completedOn,
            removeActiveTask = recurrence == null,
            nextDueDate = recurrence?.nextDueDate(completedOn),
        )
    }
}
