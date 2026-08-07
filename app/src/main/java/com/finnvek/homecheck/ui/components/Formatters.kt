package com.finnvek.homecheck.ui.components

import android.content.Context
import com.finnvek.homecheck.R
import com.finnvek.homecheck.domain.MaintenanceStatus
import com.finnvek.homecheck.domain.maintenanceStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

fun LocalDate.localized(): String = format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

fun dueLabel(context: Context, date: LocalDate, today: LocalDate = LocalDate.now()): String = when (maintenanceStatus(date, today)) {
    MaintenanceStatus.OVERDUE -> context.getString(R.string.overdue)
    MaintenanceStatus.DUE_TODAY -> context.getString(R.string.due_today)
    MaintenanceStatus.DUE_SOON -> context.resources.getQuantityString(
        R.plurals.in_days,
        ChronoUnit.DAYS.between(today, date).toInt(),
        ChronoUnit.DAYS.between(today, date).toInt(),
    )
    MaintenanceStatus.UPCOMING -> date.localized()
}
