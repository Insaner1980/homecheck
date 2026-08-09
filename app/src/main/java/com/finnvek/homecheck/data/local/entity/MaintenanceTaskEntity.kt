package com.finnvek.homecheck.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.finnvek.homecheck.domain.Recurrence
import com.finnvek.homecheck.domain.RecurrenceUnit
import java.time.LocalDate

@Entity(
    tableName = "maintenance_tasks",
    foreignKeys = [
        ForeignKey(
            entity = AssetEntity::class,
            parentColumns = ["id"],
            childColumns = ["assetId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("assetId"), Index("dueDate")],
)
data class MaintenanceTaskEntity(
    @PrimaryKey val id: String,
    val assetId: String,
    val title: String,
    val dueDate: LocalDate,
    val notes: String? = null,
    val recurrenceInterval: Int? = null,
    val recurrenceUnit: RecurrenceUnit? = null,
    val reminderEnabled: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
) {
    val recurrence: Recurrence?
        get() =
            if (recurrenceInterval != null && recurrenceUnit != null) {
                Recurrence(recurrenceInterval, recurrenceUnit)
            } else {
                null
            }
}
