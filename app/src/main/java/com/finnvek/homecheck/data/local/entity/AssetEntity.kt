package com.finnvek.homecheck.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "assets")
data class AssetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val category: String? = null,
    val location: String? = null,
    val manufacturer: String? = null,
    val modelNumber: String? = null,
    val serialNumber: String? = null,
    val purchaseDate: LocalDate? = null,
    val retailer: String? = null,
    val warrantyExpirationDate: LocalDate? = null,
    val notes: String? = null,
)
