package com.finnvek.homecheck.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AttachmentType { ASSET_PHOTO, RECEIPT, MANUAL, WARRANTY, OTHER }

@Entity(
    tableName = "attachments",
    foreignKeys = [ForeignKey(
        entity = AssetEntity::class,
        parentColumns = ["id"],
        childColumns = ["assetId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("assetId"), Index(value = ["localPath"], unique = true)],
)
data class AttachmentEntity(
    @PrimaryKey val id: String,
    val assetId: String,
    val type: AttachmentType,
    val displayName: String,
    val mimeType: String,
    val localPath: String,
    val createdAt: Long,
)

