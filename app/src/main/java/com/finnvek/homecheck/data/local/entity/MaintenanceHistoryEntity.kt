package com.finnvek.homecheck.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "maintenance_history",
    foreignKeys = [
        ForeignKey(
            entity = AssetEntity::class,
            parentColumns = ["id"],
            childColumns = ["assetId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("assetId"), Index("completedAt")],
)
data class MaintenanceHistoryEntity(
    @PrimaryKey val id: String,
    val assetId: String,
    val sourceTaskId: String? = null,
    val titleSnapshot: String,
    val completedAt: Long,
    val note: String? = null,
)
