package com.finnvek.homecheck.backup

data class HomeCheckBackup(
    val schemaVersion: Int,
    val exportedAtEpochMillis: Long,
    val assets: List<BackupAsset>,
    val attachments: List<BackupAttachment>,
    val tasks: List<BackupTask>,
    val history: List<BackupHistory>,
)

data class BackupAsset(
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val category: String? = null,
    val location: String? = null,
    val manufacturer: String? = null,
    val modelNumber: String? = null,
    val serialNumber: String? = null,
    val purchaseDate: String? = null,
    val retailer: String? = null,
    val warrantyExpirationDate: String? = null,
    val notes: String? = null,
)

data class BackupAttachment(
    val id: String,
    val assetId: String,
    val type: String,
    val displayName: String,
    val mimeType: String,
    val archivePath: String,
    val createdAt: Long,
)

data class BackupTask(
    val id: String,
    val assetId: String,
    val title: String,
    val dueDate: String,
    val notes: String? = null,
    val recurrenceInterval: Int? = null,
    val recurrenceUnit: String? = null,
    val reminderEnabled: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
)

data class BackupHistory(
    val id: String,
    val assetId: String,
    val sourceTaskId: String? = null,
    val title: String,
    val completedAt: Long,
    val note: String? = null,
)

class UnsupportedBackupVersionException(
    version: Int,
) : IllegalArgumentException("Unsupported backup schema version: $version")
